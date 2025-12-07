package org.dataledge.datasourceservice.manager;

import org.dataledge.datasourceservice.dto.Storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface IAzureBlobStorage {
    String write(Storage storage) throws IOException;
    List<String> listFiles(Storage storage);
    boolean exists(String relativePath);

}
