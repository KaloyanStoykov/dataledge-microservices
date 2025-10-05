package org.dataledge.datasourceservice.dto.datatypesDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataTypeListResponse {
    private List<DataTypeResponse> dataTypes;
}
