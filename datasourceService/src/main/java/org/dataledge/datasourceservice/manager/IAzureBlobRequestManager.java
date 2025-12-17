package org.dataledge.datasourceservice.manager;

import org.dataledge.datasourceservice.config.exceptions.BlobStorageOperationException;
import org.dataledge.datasourceservice.config.exceptions.InvalidUserException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface IAzureBlobRequestManager {
    /**
     * Saves an API response from supplied URL in Azure Blob Storage.
     * @param apiUrl the address of the API to call in the manager class
     * @param blobFileName the chosen file name to save in Azure
     * @param userId the User ID to associate with the saved file. Obtained from authenticated user.
     * @throws BlobStorageOperationException if invalid url or userId is supplied or Azure operation failed.
     * @return String message of success
     */
    String saveAPIContentToBlob(String apiUrl, String blobFileName, String userId, Long datasourceId) throws BlobStorageOperationException;

    /**
     * Writes user supplied file to Azure Blob Storage
     * @param file MultiPartFile from HTTP request containing the file to upload.
     * @param requestedFileName user requested fileName to save to Azure Blob.
     * @param userId supplied from authenticated request header
     * @return String message for successful operation
     * @throws BlobStorageOperationException on invalid requests
     */
    String writeFileToBlob(MultipartFile file, String requestedFileName, String userId) throws  BlobStorageOperationException;

    /**
     * Handles userID checks for malicious or invalid entries that will be used for the virtual folder structure in Azure Blob Storage.
     * @param userId coming from the request header from authenticated user
     * @return string integer userId to use in the saving of blobs
     * @throws InvalidUserException when userId isn't a integer based value.
     */
    String sanitizeUserId(String userId);

    /**
     * Deletes blobs from azure by passing a list of blobs to delete. Used to handle client requests from FE as well as server-side to delete via message broker
     * @param userId logged-in user's Identification header.
     * @param blobNamesToDelete list of blob names to delete from Azure (hard-delete). Can be used client-side
     * @throws BlobStorageOperationException on failed operation in blob operations
     */
    void deleteUserBlobs(String userId, List<String> blobNamesToDelete) throws BlobStorageOperationException;

    /**
     * Lists user files with PagedIterable implementation
     * @param userId logged in user's identification header
     * @return list of file names for blobs
     */
    List<String> getFiles(String userId);
}
