package org.dataledge.datasourceservice.manager;

import org.dataledge.datasourceservice.dto.datasourcesDTO.CreateDataSourceRequest;
import org.dataledge.datasourceservice.dto.datasourcesDTO.CreateDataSourceResponse;
import org.dataledge.datasourceservice.dto.datasourcesDTO.GetDataSourcesResponse;

public interface IDataSourceManager {
    GetDataSourcesResponse getDataSources(int pageNumber, int pageSize);
    CreateDataSourceResponse createDataSource(CreateDataSourceRequest dataSourceRequest);

}
