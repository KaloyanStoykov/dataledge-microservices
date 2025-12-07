package org.dataledge.datasourceservice.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureBlobStorageConfiguration {

    @Value("${AZURE_FILE_CONTAINER_NAME}")
    private String containerName;

    @Value("${AZURE_CONNECTION_STRING}")
    private String connectionString;

    @Bean
    public BlobServiceClient getBlobServiceClient() {
        return new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
    }

    @Bean
    public BlobContainerClient getBlobContainerClient() {
        return getBlobServiceClient().getBlobContainerClient(containerName);
    }

}
