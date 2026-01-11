package org.dataledge.datasourceservice.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.dataledge.common.DataLedgeUtil;
import org.dataledge.datasourceservice.dto.datasourcesDTO.*;
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
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String searchTerm,
            @RequestHeader(DataLedgeUtil.USER_ID_HEADER) String userId
    ) {
        GetDataSourcesResponse response = dataSourceManager.getDataSources(userId, pageNumber, pageSize, searchTerm);

        return ResponseEntity.ok(response);
    }

    @PostMapping()
    public ResponseEntity<CreateDataSourceResponse> createDataSource(
            @RequestBody CreateDataSourceRequest createDataSourceRequest,
            @RequestHeader(DataLedgeUtil.USER_ID_HEADER) String userId
    ) {
        CreateDataSourceResponse response = dataSourceManager.createDataSource(userId, createDataSourceRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UpdateDataSourceResponse> updateDataSource(
            @RequestHeader(DataLedgeUtil.USER_ID_HEADER) String userId,
            @PathVariable int id,
            @Valid @RequestBody UpdateDataSourceRequest updateRequest) {

        UpdateDataSourceResponse response = dataSourceManager.updateDataSource(userId, id, updateRequest);

        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteDataSourceResponse> deleteDataSource(@PathVariable("id") int id,
                                                                     @RequestHeader(DataLedgeUtil.USER_ID_HEADER) String userId){
        DeleteDataSourceResponse response = dataSourceManager.deleteDataSource(userId, id);
        return ResponseEntity.ok(response);
    }

}
