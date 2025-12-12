package org.dataledge.datasourceservice.manager;

import org.dataledge.datasourceservice.dto.Storage;

import java.io.IOException;
import java.util.List;

public interface IAzureBlobStorage {
    String write(Storage storage) throws IOException;
    List<String> listFiles(String userId);
    void deleteFilesBatch(String userId, List<String> blobNamesToDelete);
    boolean exists(String relativePath);

}
