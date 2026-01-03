package org.dataledge.datasourceservice.manager;

import org.dataledge.datasourceservice.dto.datasourcesDTO.CreateDataSourceRequest;
import org.dataledge.datasourceservice.dto.datasourcesDTO.CreateDataSourceResponse;
import org.dataledge.datasourceservice.dto.datasourcesDTO.DeleteDataSourceResponse;
import org.dataledge.datasourceservice.dto.datasourcesDTO.GetDataSourcesResponse;

public interface IDataSourceManager {
    GetDataSourcesResponse getDataSources(String userId, int pageNumber, int pageSize, String searchTerm);
    CreateDataSourceResponse createDataSource(String userId, CreateDataSourceRequest dataSourceRequest);
    DeleteDataSourceResponse deleteDataSource(String userId, int id);

}
