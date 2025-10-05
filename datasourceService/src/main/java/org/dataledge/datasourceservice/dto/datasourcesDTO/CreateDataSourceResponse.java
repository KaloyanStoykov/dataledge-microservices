package org.dataledge.datasourceservice.dto.datasourcesDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateDataSourceResponse {
    private long id;
    private String name;
}
