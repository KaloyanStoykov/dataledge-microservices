package org.dataledge.datasourceservice.data.filesnaps;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.dataledge.datasourceservice.data.datasources.DataSource;

import java.time.Instant;

@Entity
@NoArgsConstructor
@AllArgsConstructor
public class BlobMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50, name = "file_name")
    private String fileName;
    @Column(nullable = false, updatable = false)
    private Instant created;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datasource_id", nullable = false)
    private DataSource dataSource;
}
