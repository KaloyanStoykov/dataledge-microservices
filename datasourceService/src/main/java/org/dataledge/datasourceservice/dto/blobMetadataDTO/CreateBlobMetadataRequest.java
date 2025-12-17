package org.dataledge.datasourceservice.dto.blobMetadataDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateBlobMetadataRequest {
    private String fileName;
    private Long datasourceId;

}
