package org.dataledge.datasourceservice.manager;

import org.dataledge.datasourceservice.data.DataSource;
import org.dataledge.datasourceservice.dto.DataSourceResponse;
import org.mapstruct.Mapper;

@Mapper
public interface IDataSourceMapper {
    DataSourceResponse toDataSourceResponse(DataSource dataSource);
}
