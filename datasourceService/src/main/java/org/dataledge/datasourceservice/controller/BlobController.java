package org.dataledge.datasourceservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import org.dataledge.common.DataLedgeUtil;
import java.nio.charset.Charset;

@RestController
@RequestMapping("blob")
public class BlobController {

    @Qualifier("azureStorageBlobProtocolResolver")
    @Autowired
    private ResourceLoader resourceLoader;

    @Value("${AZURE_FILE_CONTAINER_NAME}")
    private String containerName;

    @Value("azure-blob://${AZURE_FILE_CONTAINER_NAME}/test.txt")
    private Resource blobFile;

    // Method to construct the full Azure path
    private String getAzurePath(String userId, String fileName) {
        // The format is: azure-blob://<container-name>/<user-id>/<file-name>
        return String.format("azure-blob://%s/%s/%s",
                containerName,
                userId,
                fileName);
    }

    @GetMapping("/readBlobFile")
    public String readBlobFile(@RequestHeader(DataLedgeUtil.USER_ID_HEADER) String userId) throws IOException {
        // Dynamically resolve the resource path
        Resource blobFile = resourceLoader.getResource(getAzurePath(userId, "test.txt"));

        return StreamUtils.copyToString(
                blobFile.getInputStream(),
                Charset.defaultCharset());
    }

    @PostMapping("/writeBlobFile")
    public String writeBlobFile(@RequestBody String data, @RequestHeader(DataLedgeUtil.USER_ID_HEADER) String userId ) throws IOException {
        // Dynamically resolve the WritableResource
        Resource resource = resourceLoader.getResource(getAzurePath(userId, "test.txt"));

        if (resource instanceof WritableResource writableResource) {
            try (OutputStream os = writableResource.getOutputStream()) {
                os.write(data.getBytes());
            }
            return "File updated for user " + userId;
        }
        throw new IOException("Resource is not writable.");
    }
}