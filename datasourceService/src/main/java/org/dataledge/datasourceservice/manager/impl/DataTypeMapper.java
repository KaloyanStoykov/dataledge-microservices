package org.dataledge.datasourceservice.manager.impl;

import org.dataledge.datasourceservice.data.DataType;
import org.dataledge.datasourceservice.dto.datatypesDTO.DataTypeResponse;
import org.dataledge.datasourceservice.manager.IDataTypeMapper;
import org.springframework.stereotype.Component;

@Component
public class DataTypeMapper implements IDataTypeMapper {
    @Override
    public DataTypeResponse mapToResponse(DataType data) {
        DataTypeResponse response = new DataTypeResponse();
        response.setId(data.getId());
        response.setName(data.getName());
        response.setDescription(data.getDescription());

        return response;
    }
}
