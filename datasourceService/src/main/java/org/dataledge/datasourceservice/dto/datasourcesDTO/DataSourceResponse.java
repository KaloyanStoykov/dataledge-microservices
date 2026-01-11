package org.dataledge.datasourceservice.dto.datasourcesDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dataledge.datasourceservice.data.DataType;
import org.dataledge.datasourceservice.dto.datatypesDTO.DataTypeResponse;

import java.time.Instant;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataSourceResponse {
    private Long id;
    private String name;
    private DataTypeResponse type;
    private String description;
    private String url;
    private Instant created;

    private Date updated;

    public DataSourceResponse(long id, String name, DataType dataType, String desc, String url, Instant now, Date updated) {
    }
}
