package org.dataledge.datasourceservice.manager.impl;

import lombok.extern.slf4j.Slf4j;
import org.dataledge.datasourceservice.config.exceptions.BlobStorageOperationException;
import org.dataledge.datasourceservice.config.exceptions.InvalidUserException;
import org.dataledge.datasourceservice.dto.Storage;
import org.dataledge.datasourceservice.manager.IAzureBlobRequestManager;
import org.dataledge.datasourceservice.manager.IAzureBlobStorage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
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

        // 1. Fetch data from the external API
        String apiResponse;
        try {
            // For a String response (like JSON or text)
            apiResponse = restTemplate.getForObject(apiUrl, String.class);
            if (apiResponse == null) {
                throw new BlobStorageOperationException("API returned no content.");
            }
        } catch (Exception e) {
            throw new BlobStorageOperationException("Failed to call external API: " + apiUrl);
        }

        // 2. Convert the API String response into an InputStream
        byte[] contentBytes = apiResponse.getBytes(Charset.defaultCharset());

        long contentSize = contentBytes.length;
        try (InputStream dataStream = new ByteArrayInputStream(contentBytes)) {

            String potentialBlobPath = sanitizedUserId + "/" + blobFileName;
            if (azureBlobStorage.exists(potentialBlobPath)) {
                throw new BlobStorageOperationException("File already exists at path: " + potentialBlobPath);
            }

            // 4. Create Storage DTO and write to Azure Blob Storage
            Storage writeStorage = new Storage(dataStream, sanitizedUserId, blobFileName, contentSize);
            String blobPath = azureBlobStorage.write(writeStorage);

            log.info("Successfully saved API response reto blob for user {} at path {}: ", sanitizedUserId, blobPath);
            return "API content successfully saved!";

        } catch (IOException e) {
            throw new BlobStorageOperationException("Error writing blob to storage.");
        }
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File name must be provided.");
        }

        // Construct the potential path (e.g., /users/{userId}/{fileName})
        String potentialBlobPath = sanitizedUserId + "/" + finalFileName;

        // 4. Checking for duplicate paths
        if (azureBlobStorage.exists(potentialBlobPath)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "File already exists at path: " + potentialBlobPath);
        }

        try (InputStream dataStream = file.getInputStream()) {

            // 2. Create the Storage DTO for the write/create operation
            Storage writeStorage = new Storage(dataStream, sanitizedUserId, finalFileName, file.getSize());

            String blobPath = azureBlobStorage.write(writeStorage);
            log.info("Successfully saved file to blob for user {} at path {}: ", sanitizedUserId, blobPath);
            return "File created successfully!";

        } catch (IOException e) {
            // Handle I/O issues during file processing
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing file upload", e);
        }
    }

    @Override
    public String sanitizeUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
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