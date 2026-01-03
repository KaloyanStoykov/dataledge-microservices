package org.dataledge.datasourceservice.controller;

import org.dataledge.datasourceservice.dto.blobMetadataDTO.GetPagedBlobMetadataResponse;
import org.dataledge.datasourceservice.dto.datasourcesDTO.DeleteDataSourcesRequest;
import org.dataledge.datasourceservice.manager.IAzureBlobRequestManager;
import org.dataledge.datasourceservice.manager.IBlobMetadataManager;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.dataledge.common.DataLedgeUtil;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("blob")
public class BlobController {

    private final IAzureBlobRequestManager azureBlobRequestManager;
    private final IBlobMetadataManager blobMetadataManager;


    public BlobController(IAzureBlobRequestManager azureBlobRequestManager ,IBlobMetadataManager blobMetadataManager) {
        this.azureBlobRequestManager = azureBlobRequestManager;
        this.blobMetadataManager = blobMetadataManager;
    }

    @PostMapping("/writeBlobFile")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> writeBlobFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileName") String requestedFileName,
            @RequestParam("dsId") Long datasourceId,
            @RequestHeader(DataLedgeUtil.USER_ID_HEADER) String userId) {

        String response = azureBlobRequestManager.writeFileToBlob(file, requestedFileName, userId, datasourceId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/saveApiContent")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> saveApiContentToBlob(
            @RequestParam("apiUrl") String apiUrl,
            @RequestParam("blobFileName") String blobFileName,
            @RequestParam("source_id") Long sourceId,
            @RequestHeader(DataLedgeUtil.USER_ID_HEADER) String userId) {

        String response = azureBlobRequestManager.saveAPIContentToBlob(apiUrl, blobFileName, userId, sourceId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getFiles")
    public ResponseEntity<GetPagedBlobMetadataResponse> getFiles(
            @RequestHeader(DataLedgeUtil.USER_ID_HEADER) String userId,
            @RequestParam("dsId") int datasourceId,
            Pageable pageable
            ) {
        var response = blobMetadataManager.getBlobsForDatasources(userId, datasourceId, pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/deleteBatch")
    public ResponseEntity<String> deleteBlob(
            @RequestBody DeleteDataSourcesRequest request,
            @RequestHeader(DataLedgeUtil.USER_ID_HEADER) String userId

    ) {
        azureBlobRequestManager.deleteUserBlobs(userId, request.getBlobFileNames());
        return ResponseEntity.ok("Successfully deleted files");
    }

}