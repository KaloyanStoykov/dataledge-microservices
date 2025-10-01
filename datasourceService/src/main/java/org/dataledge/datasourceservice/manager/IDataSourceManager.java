package org.dataledge.datasourceservice.manager;

import org.dataledge.datasourceservice.dto.DataSourceRequest;
import org.dataledge.datasourceservice.dto.DataSourceResponse;
import org.dataledge.datasourceservice.dto.GetDataSourcesResponse;

public interface IDataSourceManager {
    GetDataSourcesResponse getDataSources(int pageNumber, int pageSize);
}
