package org.dataledge.datasourceservice.dto;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.dataledge.datasourceservice.data.DataSource;
import org.springframework.data.annotation.LastModifiedDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataSourceResponse {
    private Long id;
    private String name;
    private String type;
    private String description;
    private String url;
    private Instant created;

    private Date updated;
}
