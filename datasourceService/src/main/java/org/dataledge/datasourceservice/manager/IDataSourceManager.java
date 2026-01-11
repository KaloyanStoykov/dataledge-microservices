package org.dataledge.datasourceservice.manager;

import org.dataledge.datasourceservice.dto.datasourcesDTO.*;

public interface IDataSourceManager {
    GetDataSourcesResponse getDataSources(String userId, int pageNumber, int pageSize, String searchTerm);
    CreateDataSourceResponse createDataSource(String userId, CreateDataSourceRequest dataSourceRequest);
    DeleteDataSourceResponse deleteDataSource(String userId, int id);
    UpdateDataSourceResponse updateDataSource(String userId, int id, UpdateDataSourceRequest updateRequest);
}
