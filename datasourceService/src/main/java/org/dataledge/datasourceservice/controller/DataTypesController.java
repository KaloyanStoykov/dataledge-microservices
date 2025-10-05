package org.dataledge.datasourceservice.controller;

import lombok.AllArgsConstructor;
import org.dataledge.datasourceservice.dto.datatypesDTO.DataTypeListResponse;
import org.dataledge.datasourceservice.manager.IDataTypesManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/datasource-types")
@AllArgsConstructor
public class DataTypesController {

    private IDataTypesManager dataTypesManager;

    @GetMapping()
    public ResponseEntity<DataTypeListResponse> getAllTypes(){
        DataTypeListResponse response = dataTypesManager.getDataTypes();
        return ResponseEntity.ok(response);
    }
}
