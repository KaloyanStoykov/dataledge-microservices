package org.dataledge.datasourceservice.controller;

import lombok.AllArgsConstructor;
import org.dataledge.datasourceservice.dto.GetDataSourcesResponse;
import org.dataledge.datasourceservice.manager.IDataSourceManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/datasources")
@AllArgsConstructor
public class DataSourceController {

    private IDataSourceManager dataSourceManager;

    @GetMapping()
    public ResponseEntity<GetDataSourcesResponse> getDataSource(
            @RequestParam int pageNumber,
            @RequestParam int pageSize
    ) {
        GetDataSourcesResponse response = dataSourceManager.getDataSources(pageNumber, pageSize);

        return ResponseEntity.ok(response);
    }

}
