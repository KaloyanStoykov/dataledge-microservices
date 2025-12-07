package org.dataledge.datasourceservice.manager.impl;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.dataledge.datasourceservice.config.exceptions.BlobStorageOperationException;
import org.dataledge.datasourceservice.dto.Storage;
import org.dataledge.datasourceservice.manager.IAzureBlobStorage;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * Handles I/O operations for Azure Blob Storage
 * User entries are made in virtual folders via the request header in controller layer.
 */
@Service
@Slf4j
public class AzureBlobStorageImpl implements IAzureBlobStorage {

    private final BlobServiceClient blobServiceClient;
    private final BlobContainerClient blobContainerClient;

    public AzureBlobStorageImpl(final BlobServiceClient blobServiceClient, final BlobContainerClient blobContainerClient) {
        this.blobServiceClient = blobServiceClient;
        this.blobContainerClient = blobContainerClient;
    }

    /**
     * Helper method to get the BlobClient and handle path generation/errors.
     */
    private BlobClient getBlobClient(Storage storage) {
        String path = getPath(storage);
        return blobContainerClient.getBlobClient(path);
    }


    @Override
    public String write(Storage storage) throws IOException {

        // 1. Construct the relative path within the container
        String relativePath = storage.getUserId() + "/" + storage.getFileName();

        // 2. Get the blob client reference
        // (You'll need to inject or obtain the BlobContainerClient instance)
        BlobClient blobClient = blobContainerClient.getBlobClient(relativePath);

        long fileSize = storage.getContentLength(); // Assumes DTO is updated

        // 4. Upload the stream
        try (InputStream dataStream = storage.getFileData()) {

            blobClient.upload(dataStream, fileSize);

        } catch (Exception e) {
            // Handle Azure-specific exceptions and re-throw an IOException
            throw new IOException("Failed to write blob to Azure: " + relativePath, e);
        }

        // 5. Return the full path/URI
        return blobClient.getBlobUrl();
    }

    @Override
    public boolean exists(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return false;
        }

        try {
            // 1. Get a client reference for the specific blob path.
            // Note: Getting the client doesn't actually check existence yet.
            BlobClient blobClient = blobContainerClient.getBlobClient(relativePath);

            return blobClient.exists();

        } catch (Exception e) {
            System.err.println("Error checking existence for path: " + relativePath + ". Details: " + e.getMessage());
            return false;
        }
    }


    @Override
    public List<String> listFiles(Storage storage) {
        String pathPrefix = storage.getFileName() == null ? "" : storage.getFileName() + "/";
        try {
            // listBlobsByHierarchy is the best choice for directory-like listing
            PagedIterable<BlobItem> blobList = blobContainerClient.listBlobsByHierarchy(pathPrefix);
            List<String> blobNamesList = new ArrayList<>();
            for (BlobItem blobItem : blobList) {
                // If it's a "directory" placeholder, skip it
                if (!blobItem.isPrefix()) {
                    blobNamesList.add(blobItem.getName());
                }
            }
            log.info("Successfully listed {} files in path prefix: {}", blobNamesList.size(), pathPrefix);
            return blobNamesList;
        } catch (BlobStorageException e) {
            log.error("Failed to list files with prefix: {}", pathPrefix, e);
            throw new BlobStorageOperationException("Failed to list files in path: " + pathPrefix, e);
        }
    }






    /**
     * Generates the full blob path (directory/filename).
     * Throws a custom exception for invalid input.
     */
    private String getPath(Storage storage) {
        if(StringUtils.isBlank(storage.getFileName())){
            throw new BlobStorageOperationException("Filename must be provided for the operation.");
        }

        if(StringUtils.isNotBlank(storage.getFileName())){
            // Ensure single separator and trim leading/trailing slashes from path for safety
            String path = StringUtils.strip(storage.getFileName(), "/");
            return path + "/" + storage.getFileName();
        }

        return storage.getFileName(); // No directory path, just the file name
    }
}