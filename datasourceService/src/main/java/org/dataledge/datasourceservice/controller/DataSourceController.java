package org.dataledge.datasourceservice.controller;

import lombok.AllArgsConstructor;
import org.dataledge.datasourceservice.dto.datasourcesDTO.CreateDataSourceRequest;
import org.dataledge.datasourceservice.dto.datasourcesDTO.CreateDataSourceResponse;
import org.dataledge.datasourceservice.dto.datasourcesDTO.GetDataSourcesResponse;
import org.dataledge.datasourceservice.manager.IDataSourceManager;
import org.springframework.http.HttpStatus;
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

    @PostMapping()
    public ResponseEntity<CreateDataSourceResponse> getDataSource(@RequestBody CreateDataSourceRequest createDataSourceRequest ) {
        CreateDataSourceResponse response = dataSourceManager.createDataSource(createDataSourceRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
