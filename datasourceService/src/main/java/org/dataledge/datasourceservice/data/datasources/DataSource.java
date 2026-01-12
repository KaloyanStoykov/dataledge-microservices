package org.dataledge.datasourceservice.data.datasources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dataledge.datasourceservice.data.DataType;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
public class DataSource {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id", nullable = false)
    @JsonIgnoreProperties("dataSources")
    private DataType type;

    private String description;

    private String url;

    @Column(nullable = false, updatable = false)
    private Instant created;

    @LastModifiedDate
    private Date updated;

    private int userId;
}
