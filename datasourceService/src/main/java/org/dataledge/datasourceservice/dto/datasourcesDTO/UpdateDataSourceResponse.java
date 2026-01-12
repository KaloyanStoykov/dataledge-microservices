package org.dataledge.datasourceservice.dto.datasourcesDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateDataSourceResponse {

    private int id;
    private String name;
    private String description;
    private String dataSourceUrl;

    private LocalDateTime updatedAt;
}
