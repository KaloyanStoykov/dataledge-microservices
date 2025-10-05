package org.dataledge.datasourceservice.manager.impl;

import org.dataledge.datasourceservice.data.datasources.DataSource;
import org.dataledge.datasourceservice.dto.datasourcesDTO.DataSourceResponse;
import org.dataledge.datasourceservice.manager.IDataSourceMapper;
import org.springframework.stereotype.Component;

@Component
public class DataSourceMapper implements IDataSourceMapper {
    @Override
    public DataSourceResponse toDataSourceResponse(DataSource dataSource) {
        DataSourceResponse dataSourceResponse = new DataSourceResponse();
        dataSourceResponse.setId(dataSource.getId());
        dataSourceResponse.setType(dataSource.getType());
        dataSourceResponse.setName(dataSource.getName());
        dataSourceResponse.setDescription(dataSource.getDescription());
        dataSourceResponse.setUrl(dataSource.getUrl());
        dataSourceResponse.setCreated(dataSource.getCreated());
        dataSourceResponse.setUpdated(dataSource.getUpdated());
        return dataSourceResponse;
    }
}
