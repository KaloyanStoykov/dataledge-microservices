package org.dataledge.datasourceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dataledge.datasourceservice.data.DataSource;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class GetDataSourcesResponse {
    private List<DataSourceResponse> items;
    private long totalCount;
    private int page;
    private int pageSize;
}
