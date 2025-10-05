package org.dataledge.datasourceservice.data;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    @JsonManagedReference
    private List<DataSource> dataSources;
}
