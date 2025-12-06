package org.dataledge.datasourceservice.manager;

import org.dataledge.datasourceservice.config.exceptions.*;
import org.springframework.web.multipart.MultipartFile;


public interface IAzureBlobRequestManager {
    /**
     * Saves an API response from supplied URL in Azure Blob Storage.
     * @param apiUrl the address of the API to call in the manager class
     * @param blobFileName the chose file name to save in Azure
     * @param userId the User ID to associate with the saved file. Obtained from authenticated user.
     * @throws BlobStorageOperationException if invalid url or userId is supplied ot Azure operation failed.
     * @return String message of success
     */
    String saveAPIContentToBlob(String apiUrl, String blobFileName, String userId) throws BlobStorageOperationException;

    /**
     * Writes user supplied file to Azure Blob Storage
     * @param file MultiPartFile from
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

}
