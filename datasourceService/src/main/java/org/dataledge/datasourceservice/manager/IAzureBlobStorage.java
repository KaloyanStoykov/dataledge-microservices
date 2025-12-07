package org.dataledge.datasourceservice.manager;

import org.dataledge.datasourceservice.dto.Storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface IAzureBlobStorage {
    String write(Storage storage) throws IOException;
    String update(Storage storage);
    InputStream read(Storage storage);
    List<String> listFiles(Storage storage);
    void delete(Storage storage);
    void createContainer();
    void deleteContainer();
    boolean exists(String relativePath);

}
