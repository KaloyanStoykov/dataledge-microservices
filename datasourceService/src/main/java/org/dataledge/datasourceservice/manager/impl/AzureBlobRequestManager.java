package org.dataledge.datasourceservice.manager.impl;

import lombok.extern.slf4j.Slf4j;
import org.dataledge.datasourceservice.config.exceptions.BlobStorageOperationException;
import org.dataledge.datasourceservice.config.exceptions.InvalidUserException;
import org.dataledge.datasourceservice.dto.Storage;
import org.dataledge.datasourceservice.manager.IAzureBlobRequestManager;
import org.dataledge.datasourceservice.manager.IAzureBlobStorage;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AzureBlobRequestManager implements IAzureBlobRequestManager {

    private final IAzureBlobStorage azureBlobStorage;


    public AzureBlobRequestManager(IAzureBlobStorage azureBlobStorage) {
        this.azureBlobStorage = azureBlobStorage;
    }

    @Override
    public String saveAPIContentToBlob(String apiUrl, String blobFileName, String userId) {
        String sanitizedUserId = sanitizeUserId(userId);

        // 1. Check if file exists (Move this UP to save network bandwidth)
        // It is better to check existence BEFORE downloading the file.
        String potentialBlobPath = sanitizedUserId + "/" + blobFileName;
        if (azureBlobStorage.exists(potentialBlobPath)) {
            throw new BlobStorageOperationException("File already exists at path: " + potentialBlobPath);
        }

        // 2. Fetch Data (Delegated to a separate method for testability)
        byte[] contentBytes = fetchSecurely(apiUrl);

        if (contentBytes == null || contentBytes.length == 0) {
            throw new BlobStorageOperationException("API returned no content.");
        }

        // 3. Save to Blob Storage
        try (InputStream dataStream = new ByteArrayInputStream(contentBytes)) {
            Storage writeStorage = new Storage(dataStream, sanitizedUserId, blobFileName, (long) contentBytes.length);
            azureBlobStorage.write(writeStorage);
            return "API content successfully saved!";
        } catch (IOException e) {
            throw new BlobStorageOperationException("Error writing blob to storage.", e);
        }
    }


    public byte[] fetchSecurely(String apiUrl) {
        URI uri;
        try {
            uri = URI.create(apiUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                throw new BlobStorageOperationException("Only HTTPS allowed");
            }
        } catch (IllegalArgumentException e) {
            throw new BlobStorageOperationException("Invalid URL format");
        }

        // REFRACTOR 1: Use helper method for DNS resolution
        try {
            InetAddress address = resolveHost(uri.getHost());
            if (address.isLoopbackAddress() || address.isSiteLocalAddress() ||
                    address.isLinkLocalAddress() || address.isAnyLocalAddress()) {
                throw new BlobStorageOperationException("Access to internal network is denied.");
            }
        } catch (Exception e) {
            throw new BlobStorageOperationException("Could not validate host IP");
        }

        // REFRACTOR 2: Use helper method for Connection creation
        try {
            URL url = uri.toURL();
            HttpURLConnection connection = createConnection(url);
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(5000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new BlobStorageOperationException("Failed to call external API. Code: " + responseCode);
            }

            try (InputStream in = connection.getInputStream()) {
                return in.readNBytes(10 * 1024 * 1024);
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Failed to call external API")) {
                assert e instanceof BlobStorageOperationException;
                throw (BlobStorageOperationException) e;
            }
            throw new BlobStorageOperationException("Failed to call external API: " + apiUrl, e);
        }
    }


    public InetAddress resolveHost(String host) throws UnknownHostException {
        return InetAddress.getByName(host);
    }

    public HttpURLConnection createConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    @Override
    public List<String> getFiles(String userId){
        String sanitizedUserId = sanitizeUserId(userId);
        return azureBlobStorage.listFiles(sanitizedUserId);
    }

    @Override
    public void deleteUserBlobs(String userId, List<String> blobNamesToDelete) throws BlobStorageOperationException {
        String sanitizedUserId = sanitizeUserId(userId);

        azureBlobStorage.deleteFilesBatch(sanitizedUserId, blobNamesToDelete);
    }


    @Override
    public String writeFileToBlob(MultipartFile file, String requestedFileName, String userId) throws BlobStorageOperationException {
        // 3. Sanitizing userId input to secure the application
        String sanitizedUserId = sanitizeUserId(userId);

        // Use the original filename if one isn't explicitly requested
        String finalFileName = requestedFileName != null && !requestedFileName.isEmpty()
                ? requestedFileName
                : file.getOriginalFilename();

        if (finalFileName == null || finalFileName.isEmpty()) {
            throw new BlobStorageOperationException("File name must be provided.");
        }

        // Construct the potential path (e.g., /users/{userId}/{fileName})
        String potentialBlobPath = sanitizedUserId + "/" + finalFileName;

        // 4. Checking for duplicate paths
        if (azureBlobStorage.exists(potentialBlobPath)) {
            throw new BlobStorageOperationException("File already exists at path: " + potentialBlobPath);
        }

        try (InputStream dataStream = file.getInputStream()) {

            // 2. Create the Storage DTO for the write/create operation
            Storage writeStorage = new Storage(dataStream, sanitizedUserId, finalFileName, file.getSize());

            String blobPath = azureBlobStorage.write(writeStorage);
            log.info("Successfully saved file to blob for user {} at path {}: ", sanitizedUserId, blobPath);
            return "File created successfully!";

        } catch (IOException e) {
            // Handle I/O issues during file processing
            throw new BlobStorageOperationException("Error processing file upload", e);
        }
    }

    @Override
    public String sanitizeUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.error("Invalid user id provided.");
            throw new InvalidUserException("User ID cannot be empty.");
        }

        // Updated safe pattern: only digits (0-9)
        if (!Pattern.matches("^[0-9]+$", userId.trim())) {
            log.error("User ID failed numeric regex pattern.");
            throw new InvalidUserException("Invalid User ID format. Must be an integer.");
        }

        // Trimming before using the result is still a good practice
        return userId.trim();
    }
}