package org.dataledge.datasourceservice.manager;


import jakarta.transaction.Transactional;
import org.dataledge.datasourceservice.data.datasources.DataSource;
import org.dataledge.datasourceservice.dto.blobMetadataDTO.CreateBlobMetadataRequest;
import org.dataledge.datasourceservice.dto.blobMetadataDTO.GetPagedBlobMetadataResponse;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;

import java.util.List;


public interface IBlobMetadataManager {
    GetPagedBlobMetadataResponse getBlobsForDatasources(String userId, int datasourceId, int pageNumber, int pageSize);
    String createBlobMetadata(int userId, String fileName, DataSource ds);
    void deleteMetadataBatch(int userId, List<String> blobNames);
}
