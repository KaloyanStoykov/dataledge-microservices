package org.dataledge.datasourceservice.dto.blobMetadataDTO;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
public class GetPagedBlobMetadataResponse {
    private List<BlobMetadataResponse> blobsData;
    private long totalCount;
    private int page;
    private int pageSize;

}
