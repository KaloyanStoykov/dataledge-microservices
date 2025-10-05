package org.dataledge.datasourceservice.manager;

import org.dataledge.datasourceservice.data.datasources.DataSource;
import org.dataledge.datasourceservice.dto.datasourcesDTO.DataSourceResponse;
import org.mapstruct.Mapper;

@Mapper
public interface IDataSourceMapper {
    DataSourceResponse toDataSourceResponse(DataSource dataSource);
}
