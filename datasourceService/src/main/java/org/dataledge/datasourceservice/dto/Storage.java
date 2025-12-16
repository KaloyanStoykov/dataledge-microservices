package org.dataledge.datasourceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Storage {
    private InputStream fileData;
    private String userId;
    private String fileName;
    private long contentLength;
}
