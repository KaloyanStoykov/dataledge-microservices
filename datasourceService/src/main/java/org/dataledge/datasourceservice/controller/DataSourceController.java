package org.dataledge.datasourceservice.controller;

import io.micrometer.core.ipc.http.HttpSender;
import lombok.AllArgsConstructor;
import org.dataledge.datasourceservice.dto.datasourcesDTO.CreateDataSourceRequest;
import org.dataledge.datasourceservice.dto.datasourcesDTO.CreateDataSourceResponse;
import org.dataledge.datasourceservice.dto.datasourcesDTO.DeleteDataSourceResponse;
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
            @RequestParam int pageSize,
            @RequestHeader("X-User-ID") String userId
    ) {
        GetDataSourcesResponse response = dataSourceManager.getDataSources(userId, pageNumber, pageSize);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/tup")
    public String getHeader(@RequestHeader("X-User-ID") String userId) {
        return userId;
    }

    @PostMapping()
    public ResponseEntity<CreateDataSourceResponse> createDataSource(
            @RequestBody CreateDataSourceRequest createDataSourceRequest,
            @RequestHeader("X-User-ID") String userId
    ) {
        CreateDataSourceResponse response = dataSourceManager.createDataSource(userId, createDataSourceRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteDataSourceResponse> deleteDataSource(@PathVariable("id") int id){
        DeleteDataSourceResponse response = dataSourceManager.deleteDataSource(id);
        return ResponseEntity.ok(response);
    }

}
