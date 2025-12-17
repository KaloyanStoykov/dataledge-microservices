package org.dataledge.datasourceservice.dto.blobMetadataDTO;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
public class BlobMetadataResponse {
    private Long id;
    private String name;
    private Instant created;
}
