package org.dataledge.datasourceservice.manager;

import org.dataledge.datasourceservice.dto.datatypesDTO.DataTypeListResponse;
import org.dataledge.datasourceservice.dto.datatypesDTO.DataTypeResponse;

import java.util.List;

public interface IDataTypesManager {
    DataTypeListResponse getDataTypes();
}
