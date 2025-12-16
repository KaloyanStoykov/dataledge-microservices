package org.dataledge.datasourceservice.manager.impl;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.batch.BlobBatchClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import lombok.extern.slf4j.Slf4j;
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
    private final BlobContainerClient blobContainerClient;
    private final BlobBatchClient blobBatchClient;

    public AzureBlobStorageImpl(final BlobContainerClient blobContainerClient,  final BlobBatchClient blobBatchClient) {
        this.blobContainerClient = blobContainerClient;
        this.blobBatchClient = blobBatchClient;
    }



    @Override
    public String write(Storage storage) throws IOException {

        // 1. Construct the relative path within the container
        String relativePath = storage.getUserId() + "/" + storage.getFileName();

        // 2. Get the blob client reference
        BlobClient blobClient = blobContainerClient.getBlobClient(relativePath);

        long fileSize = storage.getContentLength(); // Assumes DTO is updated

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
            BlobClient blobClient = blobContainerClient.getBlobClient(relativePath);

            return blobClient.exists();

        } catch (Exception e) {
            System.err.println("Error checking existence for path: " + relativePath + ". Details: " + e.getMessage());
            return false;
        }
    }


    @Override
    public List<String> listFiles(String userId) {
        String pathPrefix = userId + "/";
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

    @Override
    public void deleteFilesBatch(String userId, List<String> blobNamesToDelete) {
        String pathPrefix = userId + "/";

        // Filter list for security first
        List<String> validBlobs = blobNamesToDelete.stream()
                .filter(name -> name.startsWith(pathPrefix))
                .map(name -> blobContainerClient.getBlobClient(name).getBlobUrl()) // <--- THIS IS THE FIX
                .toList();

        if (validBlobs.isEmpty()) return;

        try {
            // Note: Batch operations usually have a limit (e.g., 256 per batch).
            blobBatchClient.deleteBlobs(validBlobs, DeleteSnapshotsOptionType.INCLUDE).forEach(response -> {
                if (response.getStatusCode() != 202) {
                    log.error("Failed to delete blob via batch. Blob URL: {}. Expected status 202 but got {}: {}",
                            response.getRequest().getUrl(), response.getStatusCode(), response.getValue());
                }
            });
            log.info("Batch deletion request completed for {} files.", validBlobs.size());
        } catch (Exception e) {
            log.error("Batch delete failed", e);
            throw new BlobStorageOperationException("Batch delete failed", e);
        }
    }



}