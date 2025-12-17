package org.dataledge.datasourceservice.manager;


import org.dataledge.datasourceservice.data.datasources.DataSource;
import org.dataledge.datasourceservice.dto.blobMetadataDTO.CreateBlobMetadataRequest;
import org.dataledge.datasourceservice.dto.blobMetadataDTO.GetPagedBlobMetadataResponse;


public interface IBlobMetadataManager {
    GetPagedBlobMetadataResponse getBlobsForDatasources(String userId, int datasourceId, int pageNumber, int pageSize);
    String createBlobMetadata(int userId, String fileName, DataSource ds);

}
