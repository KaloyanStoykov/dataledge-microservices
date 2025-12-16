package org.dataledge.datasourceservice.manager.impl;

import jakarta.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;
import org.dataledge.datasourceservice.data.DataTypeRepo;
import org.dataledge.datasourceservice.dto.datatypesDTO.DataTypeListResponse;
import org.dataledge.datasourceservice.manager.IDataTypeMapper;
import org.dataledge.datasourceservice.manager.IDataTypesManager;
import org.springframework.stereotype.Service;


/***
 * @author Kaloyan Stoykov
 */
@Service
@AllArgsConstructor
public class DataTypesManager implements IDataTypesManager {

    // Jpa repository for datatypes
    private final DataTypeRepo repo;
    // Mapper to responses
    private final IDataTypeMapper mapper;

    /**
     *
     * @return List of DataTypes from database schema
     */
    @Override
    public DataTypeListResponse getDataTypes() {
        var types =  repo.findAll();
        DataTypeListResponse response = new DataTypeListResponse();

        if(types.isEmpty()){
            throw new NotFoundException("No data types found!");
        }

        response.setDataTypes(types.stream().map(mapper::mapToResponse).toList());
        return response;

    }
}
