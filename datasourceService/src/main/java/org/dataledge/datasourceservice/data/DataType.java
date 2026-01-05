package org.dataledge.datasourceservice.data;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minidev.json.annotate.JsonIgnore;
import org.dataledge.datasourceservice.data.datasources.DataSource;

import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataType {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    private String description;

    @OneToMany(mappedBy = "type", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<DataSource> dataSources;
}
