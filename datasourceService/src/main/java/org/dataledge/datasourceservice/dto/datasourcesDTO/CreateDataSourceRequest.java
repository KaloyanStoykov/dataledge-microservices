package org.dataledge.datasourceservice.dto.datasourcesDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateDataSourceRequest {
    private String name;
    private Long typeId;
    private String description;
    private String url;
    private Instant created;

    private Date updated;
}
