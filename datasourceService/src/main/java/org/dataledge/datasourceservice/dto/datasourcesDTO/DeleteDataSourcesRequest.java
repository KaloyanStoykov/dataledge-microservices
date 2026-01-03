package org.dataledge.datasourceservice.dto.datasourcesDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DeleteDataSourcesRequest {
    private List<String> blobFileNames;
}
