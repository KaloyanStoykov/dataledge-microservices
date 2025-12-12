package org.dataledge.datasourceservice;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.batch.BlobBatchClient;
import org.dataledge.datasourceservice.dto.Storage;
import org.dataledge.datasourceservice.manager.impl.AzureBlobStorageImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class AzureBlobStorageTest {

    @Test
    void write_ShouldCatchException_AndThrowIOException_WhenUploadFails() {
        BlobContainerClient mockContainerClient = Mockito.mock(BlobContainerClient.class);
        BlobBatchClient mockBatchClient = Mockito.mock(BlobBatchClient.class);
        BlobClient mockBlobClient = Mockito.mock(BlobClient.class);

        when(mockContainerClient.getBlobClient(anyString())).thenReturn(mockBlobClient);

        doThrow(new RuntimeException("Simulated Azure Connection Failure"))
                .when(mockBlobClient).upload(org.mockito.ArgumentMatchers.any(),anyLong());

        AzureBlobStorageImpl serviceUnderTest = new AzureBlobStorageImpl(
                mockContainerClient,
                mockBatchClient
        );

        String userId = "user-error-test";
        String fileName = "fail.txt";
        Storage storage = new Storage(
                new ByteArrayInputStream("test-data".getBytes()),
                userId,
                fileName,
                100L
        );

        IOException thrownException = assertThrows(IOException.class, () -> serviceUnderTest.write(storage));

        String expectedPath = userId + "/" + fileName;
        assertThat(thrownException.getMessage()).contains("Failed to write blob to Azure");
        assertThat(thrownException.getMessage()).contains(expectedPath);
    }

    @Test
    void exists_ShouldReturnFalse_AndSuppressException_WhenAzureFails() {
        // 1. ARRANGE
        String userId = "user-error-test";
        String fileName = "fail.txt";
        String expectedPath = userId + "/" + fileName;

        BlobContainerClient mockContainerClient = Mockito.mock(BlobContainerClient.class);
        BlobBatchClient mockBatchClient = Mockito.mock(BlobBatchClient.class);

        BlobClient mockBlobClient = Mockito.mock(BlobClient.class);

        // Wire up the container to return our mock blob client
        when(mockContainerClient.getBlobClient(anyString())).thenReturn(mockBlobClient);

        when(mockBlobClient.exists())
                .thenThrow(new RuntimeException("Simulated Azure Connection Failure"));

        AzureBlobStorageImpl serviceUnderTest = new AzureBlobStorageImpl(
                mockContainerClient,
                mockBatchClient
        );

        // 2. ACT
        // We do NOT use assertThrows because your code catches the exception
        boolean result = serviceUnderTest.exists(expectedPath);

        // 3. ASSERT
        // Your code says: catch (Exception e) { return false; }
        // So we expect false.
        assertThat(result).isFalse();
    }
}
