package org.dataledge.datasourceservice.dto.datasourcesDTO;


import lombok.Data;

@Data
public class UpdateDataSourceRequest {
    private String name;
    private String description;
    private String dataSourceUrl;
    private Long typeId; // To update the relationship to DataType
}