package org.dataledge.datasourceservice.manager.impl;

import org.dataledge.datasourceservice.data.datasources.DataSource;
import org.dataledge.datasourceservice.dto.datasourcesDTO.DataSourceResponse;
import org.dataledge.datasourceservice.dto.datatypesDTO.DataTypeResponse;
import org.dataledge.datasourceservice.manager.IDataSourceMapper;
import org.springframework.stereotype.Component;

@Component
public class DataSourceMapper implements IDataSourceMapper {
    @Override
    public DataSourceResponse toDataSourceResponse(DataSource dataSource) {
        if (dataSource == null) {
            return null;
        }

        DataSourceResponse dataSourceResponse = new DataSourceResponse();
        dataSourceResponse.setId(dataSource.getId());
        dataSourceResponse.setName(dataSource.getName());
        dataSourceResponse.setDescription(dataSource.getDescription());
        dataSourceResponse.setUrl(dataSource.getUrl());
        dataSourceResponse.setCreated(dataSource.getCreated());
        dataSourceResponse.setUpdated(dataSource.getUpdated());

        // FIX: Pull data from the Entity (dataSource), not the Response (dataSourceResponse)
        if (dataSource.getType() != null) {
            DataTypeResponse typeDto = new DataTypeResponse();
            typeDto.setId(dataSource.getType().getId()); // Fixed: was dataSourceResponse
            typeDto.setName(dataSource.getType().getName());
            typeDto.setDescription(dataSource.getType().getDescription()); // Fixed: was dataSourceResponse

            dataSourceResponse.setType(typeDto);
        }

        return dataSourceResponse;
    }
}
