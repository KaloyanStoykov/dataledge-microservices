package org.dataledge.datasourceservice.manager;

import org.dataledge.datasourceservice.data.DataType;
import org.dataledge.datasourceservice.dto.datatypesDTO.DataTypeResponse;
import org.mapstruct.Mapper;

@Mapper
public interface IDataTypeMapper {
    DataTypeResponse mapToResponse(DataType data);
}
