package org.dataledge.datasourceservice.controller;

import org.dataledge.datasourceservice.manager.IAzureBlobRequestManager;
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

    public BlobController(IAzureBlobRequestManager azureBlobRequestManager) {
        this.azureBlobRequestManager = azureBlobRequestManager;
    }

    @PostMapping("/writeBlobFile")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> writeBlobFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileName") String requestedFileName,
            @RequestHeader(DataLedgeUtil.USER_ID_HEADER) String userId) {

        String response = azureBlobRequestManager.writeFileToBlob(file, requestedFileName, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/saveApiContent")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> saveApiContentToBlob(
            @RequestParam("apiUrl") String apiUrl,
            @RequestParam("blobFileName") String blobFileName,
            @RequestHeader(DataLedgeUtil.USER_ID_HEADER) String userId) {

        String response = azureBlobRequestManager.saveAPIContentToBlob(apiUrl, blobFileName, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getFiles")
    public ResponseEntity<List<String>> getFiles(@RequestHeader(DataLedgeUtil.USER_ID_HEADER) String userId) {
        var response = azureBlobRequestManager.getFiles(userId);
        return ResponseEntity.ok(response);
    }

}