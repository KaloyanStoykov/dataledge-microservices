package org.dataledge.datasourceservice.controller;

import org.dataledge.datasourceservice.dto.Storage;
import org.dataledge.datasourceservice.manager.IAzureBlobStorage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.dataledge.common.DataLedgeUtil;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

@RestController
@RequestMapping("blob")
public class BlobController {



    private final IAzureBlobStorage azureBlobStorage;

    private final RestTemplate restTemplate;

    public BlobController(IAzureBlobStorage azureBlobStorage, RestTemplate restTemplate) {
        this.azureBlobStorage = azureBlobStorage;
        this.restTemplate = restTemplate;
    }



    @PostMapping("/writeBlobFile")
    public String writeBlobFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileName") String requestedFileName,
            @RequestHeader(DataLedgeUtil.USER_ID_HEADER) String userId) {

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

            return "File created successfully for user " + sanitizedUserId + " at path: " + blobPath;

        } catch (IOException e) {
            // Handle I/O issues during file processing
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing file upload", e);
        }
    }

    @PostMapping("/saveApiContent")
    @ResponseStatus(HttpStatus.CREATED)
    public String saveApiContentToBlob(
            @RequestParam("apiUrl") String apiUrl,
            @RequestParam("blobFileName") String blobFileName,
            @RequestHeader(DataLedgeUtil.USER_ID_HEADER) String userId) {

        String sanitizedUserId = sanitizeUserId(userId);

        // 1. Fetch data from the external API
        String apiResponse;
        try {
            // For a String response (like JSON or text)
            apiResponse = restTemplate.getForObject(apiUrl, String.class);
            if (apiResponse == null) {
                throw new ResponseStatusException(HttpStatus.NO_CONTENT, "API returned no content.");
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.FAILED_DEPENDENCY, "Failed to call external API: " + apiUrl, e);
        }

        // 2. Convert the API String response into an InputStream
        byte[] contentBytes = apiResponse.getBytes(Charset.defaultCharset());
        long contentSize = contentBytes.length;
        try (InputStream dataStream = new ByteArrayInputStream(contentBytes)) {

            String potentialBlobPath = sanitizedUserId + "/" + blobFileName;
            if (azureBlobStorage.exists(potentialBlobPath)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "File already exists at path: " + potentialBlobPath);
            }

            // 4. Create Storage DTO and write to Azure Blob Storage
            Storage writeStorage = new Storage(dataStream, sanitizedUserId, blobFileName, contentSize);
            String blobPath = azureBlobStorage.write(writeStorage);

            return "API content successfully saved as blob for user " + sanitizedUserId + " at path: " + blobPath;

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error writing blob to storage.", e);
        }
    }



    private String sanitizeUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID cannot be empty.");
        }

        // Updated safe pattern: only digits (0-9)
        if (!Pattern.matches("^[0-9]+$", userId.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid User ID format. Must be an integer.");
        }

        // Trimming before using the result is still a good practice
        return userId.trim();
    }
}