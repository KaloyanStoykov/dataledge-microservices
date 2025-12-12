package org.dataledge.datasourceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.InputStream;

@Data
@AllArgsConstructor
public class Storage {
    private final InputStream fileData;
    private final String userId;
    private final String fileName;
    private final Long contentLength;
}


