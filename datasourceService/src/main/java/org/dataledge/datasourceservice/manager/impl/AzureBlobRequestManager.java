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
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class AzureBlobRequestManager implements IAzureBlobRequestManager {

    private final IAzureBlobStorage azureBlobStorage;

    private final RestTemplate restTemplate;

    public AzureBlobRequestManager(IAzureBlobStorage azureBlobStorage, RestTemplate restTemplate) {
        this.azureBlobStorage = azureBlobStorage;
        this.restTemplate = restTemplate;
    }

    @Override
    public String saveAPIContentToBlob(String apiUrl, String blobFileName, String userId) throws BlobStorageOperationException {
        String sanitizedUserId = sanitizeUserId(userId);

        URI uri = URI.create(apiUrl);
        String host = uri.getHost();

        // 1. Check Protocol
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new BlobStorageOperationException("Only HTTPS allowed");
        }

        // 2. Resolve Host to IP and check for Internal Addresses
        try {
            InetAddress address = InetAddress.getByName(host);

            if (address.isLoopbackAddress() ||  // 127.0.0.1
                    address.isSiteLocalAddress() || // 192.168.x.x, 10.x.x.x, etc.
                    address.isLinkLocalAddress() || // 169.254.x.x
                    address.isAnyLocalAddress()) {  // 0.0.0.0

                throw new BlobStorageOperationException("Access to internal network is denied.");
            }
        } catch (Exception e) {
            throw new BlobStorageOperationException("Could not validate host IP");
        }

        // 1. Fetch data from the external API
        String apiResponse;
        try {
            // Only guard the risky network call
            apiResponse = restTemplate.getForObject(apiUrl, String.class);
        } catch (Exception e) {
            // This catches network/timeout errors
            throw new BlobStorageOperationException("Failed to call external API: " + apiUrl, e);
        }

        // 2. Validate (Outside the try/catch)
        if (apiResponse == null) {
            // This will now propagate correctly to the test
            throw new BlobStorageOperationException("API returned no content.");
        }

        // 3. Convert and Save
        byte[] contentBytes = apiResponse.getBytes(Charset.defaultCharset());
        long contentSize = contentBytes.length;

        try (InputStream dataStream = new ByteArrayInputStream(contentBytes)) {
            String potentialBlobPath = sanitizedUserId + "/" + blobFileName;

            if (azureBlobStorage.exists(potentialBlobPath)) {
                throw new BlobStorageOperationException("File already exists at path: " + potentialBlobPath);
            }

            Storage writeStorage = new Storage(dataStream, sanitizedUserId, blobFileName, contentSize);
            String blobPath = azureBlobStorage.write(writeStorage);

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