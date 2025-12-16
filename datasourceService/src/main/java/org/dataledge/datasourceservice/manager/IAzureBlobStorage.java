package org.dataledge.datasourceservice.manager;

import org.dataledge.datasourceservice.dto.Storage;

import java.io.IOException;
import java.util.List;

/**
 * Defines core file operations for Azure Blob Storage.
 * This interface handles the lifecycle of files (blobs) organized by user ID,
 * providing methods to upload, list, check existence, and delete files in batches.
 */
public interface IAzureBlobStorage {

    /**
     * Uploads a file to Azure Blob Storage.
     * The file is stored in a virtual directory structure defined by {@code userId/fileName}.
     * @param storage The object containing file metadata (User ID, File Name) and the content stream.
     * @return The full public URL of the uploaded blob.
     * @throws IOException If the upload fails due to I/O errors or network interruption.
     */
    String write(Storage storage) throws IOException;

    /**
     * Lists all file paths belonging to a specific user.
     * This scans the container for all blobs prefixed with the given {@code userId}.
     * @param userId The unique identifier for the user.
     * @return A list of relative file paths (blob names) found for this user.
     */
    List<String> listFiles(String userId);

    /**
     * Deletes a specific list of files for a user in a single batch operation.
     * This method ensures security by validating that all target files belong to the
     * specified {@code userId} before deletion.
     * @param userId            The owner of the files to be deleted.
     * @param blobNamesToDelete A list of relative paths (blob names) to delete.
     */
    void deleteFilesBatch(String userId, List<String> blobNamesToDelete);

    /**
     * Checks if a specific file exists in the storage container.
     * @param relativePath The full relative path to the blob (e.g., "userId/filename.txt").
     * @return {@code true} if the file exists; {@code false} otherwise.
     */
    boolean exists(String relativePath);
}