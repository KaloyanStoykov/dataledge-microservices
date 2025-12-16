package org.dataledge.datasourceservice.manager.impl;

import lombok.extern.slf4j.Slf4j;
import org.dataledge.datasourceservice.config.exceptions.BlobStorageOperationException;
import org.dataledge.datasourceservice.config.exceptions.InvalidUserException;
import org.dataledge.datasourceservice.dto.Storage;
import org.dataledge.datasourceservice.manager.IAzureBlobRequestManager;
import org.dataledge.datasourceservice.manager.IAzureBlobStorage;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
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
    public String saveAPIContentToBlob(String apiUrl, String blobFileName, String userId) throws BlobStorageOperationException {
        String sanitizedUserId = sanitizeUserId(userId);

        // 1. Strict Protocol Check (Reject non-HTTPS immediately)
        URI uri;
        try {
            uri = URI.create(apiUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                throw new BlobStorageOperationException("Only HTTPS allowed");
            }
        } catch (IllegalArgumentException e) {
            throw new BlobStorageOperationException("Invalid URL format");
        }

        // 2. Resolve IP and Check Blocklist
        // Note: This reduces risk but does not eliminate DNS Rebinding (see note below)
        try {
            InetAddress address = InetAddress.getByName(uri.getHost());
            if (address.isLoopbackAddress() || address.isSiteLocalAddress() ||
                    address.isLinkLocalAddress() || address.isAnyLocalAddress()) {
                throw new BlobStorageOperationException("Access to internal network is denied.");
            }
        } catch (Exception e) {
            throw new BlobStorageOperationException("Could not validate host IP");
        }

        // 3. Fetch data using HttpURLConnection (Replaces RestTemplate)
        byte[] contentBytes;
        try {
            URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // CRITICAL: Disable automatic redirects.
            // Attackers use redirects to bypass the IP check above.
            connection.setInstanceFollowRedirects(false);

            // Security Timeouts (Prevent DoS)
            connection.setConnectTimeout(3000); // 3 seconds to connect
            connection.setReadTimeout(5000);    // 5 seconds to read

            connection.connect();

            // Ensure we got a 200 OK (and not a 301/302 Redirect to an internal IP)
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new BlobStorageOperationException("API call failed or attempted redirect. Code: " + responseCode);
            }

            // Read content safely (Limit size to avoid Memory Exhaustion)
            try (InputStream in = connection.getInputStream()) {
                // Read max 10MB (adjust as needed)
                contentBytes = in.readNBytes(10 * 1024 * 1024);
            }

        } catch (Exception e) {
            throw new BlobStorageOperationException("Failed to safely call external API: " + apiUrl, e);
        }

        // 4. Validate Content
        if (contentBytes == null || contentBytes.length == 0) {
            throw new BlobStorageOperationException("API returned no content.");
        }

        // 5. Save to Blob Storage (Existing Logic)
        long contentSize = contentBytes.length;
        try (InputStream dataStream = new ByteArrayInputStream(contentBytes)) {
            String potentialBlobPath = sanitizedUserId + "/" + blobFileName;

            if (azureBlobStorage.exists(potentialBlobPath)) {
                throw new BlobStorageOperationException("File already exists at path: " + potentialBlobPath);
            }

            Storage writeStorage = new Storage(dataStream, sanitizedUserId, blobFileName, contentSize);
            azureBlobStorage.write(writeStorage);

            return "API content successfully saved!";

        } catch (IOException e) {
            throw new BlobStorageOperationException("Error writing blob to storage.");
        }
    }

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