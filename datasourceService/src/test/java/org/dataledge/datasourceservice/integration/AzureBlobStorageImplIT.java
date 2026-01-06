package org.dataledge.datasourceservice.integration;

import com.azure.storage.blob.*;
import com.azure.storage.blob.batch.BlobBatchClient;
import com.azure.storage.blob.batch.BlobBatchClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import net.bytebuddy.utility.RandomString;
import org.dataledge.datasourceservice.config.exceptions.BlobStorageOperationException;
import org.dataledge.datasourceservice.dto.Storage;
import org.dataledge.datasourceservice.manager.impl.AzureBlobStorageImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import java.io.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.GenericContainer;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
class AzureBlobStorageImplIT {

    // --- TEST CONTAINER SETUP ---
    private static final GenericContainer<?> AZURITE_CONTAINER = new GenericContainer<>("mcr.microsoft.com/azure-storage/azurite:latest")
            .withCommand("azurite-blob", "--blobHost", "0.0.0.0")
            .withExposedPorts(10000);

    @BeforeAll
    static void startContainer() {
        AZURITE_CONTAINER.start();
    }

    private BlobContainerClient realContainerClient;

    private BlobBatchClient blobBatchClient;

    private AzureBlobStorageImpl azureBlobStorageImpl;

    @BeforeEach
    void setUp() {
        Integer blobPort = AZURITE_CONTAINER.getMappedPort(10000);
        // Use localhost consistently
        String connectionString = String.format(
                "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://localhost:%s/devstoreaccount1;",
                blobPort
        );

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        // The Batch client must be created from the same service client to share credentials and endpoint logic
        this.blobBatchClient = new BlobBatchClientBuilder(blobServiceClient).buildClient();

        String containerName = RandomString.make(10).toLowerCase();
        realContainerClient = blobServiceClient.createBlobContainer(containerName);

        azureBlobStorageImpl = new AzureBlobStorageImpl(realContainerClient, blobBatchClient);
    }

    @Test
    void shouldSaveBlobSuccessfullyToContainerWithUserFolder() throws IOException {
        // Arrange
        String userId = "12";
        var fileName = "my-test-file.txt";
        var fileContent = "This is some content inside the file.";

        File tempFile = new File(fileName);
        try (FileWriter fileWriter = new FileWriter(tempFile)) {
            fileWriter.write(fileContent);
        }

        try (InputStream fileInputStream = new FileInputStream(tempFile)) {
            // Prepare the DTO
            Storage storage = new Storage(
                    fileInputStream,
                    userId,      // Passing the ID
                    fileName,    // Passing the Filename
                    tempFile.length()
            );

            String returnedUrl = azureBlobStorageImpl.write(storage);


            assertThat(returnedUrl).isNotNull();

            String expectedBlobPath = userId + "/" + fileName;

            BlobClient uploadedBlob = realContainerClient.getBlobClient(expectedBlobPath);

            assertThat(uploadedBlob.exists())
                    .withFailMessage("Blob should exist at path: " + expectedBlobPath)
                    .isTrue();

            String actualContent = new String(uploadedBlob.downloadContent().toBytes());
            assertEquals(fileContent, actualContent);

        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }


    @Test
    void exists_ShouldReturnTrue_WhenFileActuallyExistsInContainer() {
        String userId = "user-99";
        String fileName = "existing-file.txt";
        String relativePath = userId + "/" + fileName;

        realContainerClient.getBlobClient(relativePath)
                .upload(new ByteArrayInputStream("data".getBytes()), 4);

        boolean result = azureBlobStorageImpl.exists(relativePath);

        assertThat(result).isTrue();
    }

    @Test
    void exists_ShouldReturnFalse_WhenFileDoesNotExist() {
        String nonExistentPath = "user-99/ghost-file.txt";

        boolean result = azureBlobStorageImpl.exists(nonExistentPath);

        assertThat(result).isFalse();
    }

    @Test
    void exists_ShouldReturnFalse_WhenInputIsInvalid() {
        assertThat(azureBlobStorageImpl.exists(null)).isFalse();

        assertThat(azureBlobStorageImpl.exists("")).isFalse();
    }



    @Test
    void listFiles_ShouldReturnOnlyFiles_AndIgnoreSubfolders_Integration() {
        String folderName = "user-23";
        // 1. A standard file in the folder (Should be returned)
        String file1 = folderName + "/data.csv";
        // 2. Another standard file (Should be returned)
        String file2 = folderName + "/image.png";
        // 3. A file inside a subfolder (Should NOT be returned directly,
        String nestedFile = folderName + "/subfolder/hidden.txt";

        uploadString(file1);
        uploadString(file2);
        uploadString(nestedFile);


        List<String> results = azureBlobStorageImpl.listFiles("user-23");

        assertThat(results).isNotNull();
        assertThat(results.size()).isEqualTo(2);

        assertThat(results).containsExactlyInAnyOrder(file1, file2);
    }

    @Test
    void listFiles_ShouldReturnEmptyList_WhenFolderIsNew() {
        List<String> results = azureBlobStorageImpl.listFiles("user-1");

        assertThat(results).isNotNull();
        assertThat(results.size()).isEqualTo(0);
    }

    @Test
    void listFiles_ShouldThrowException_WhenContainerMissing_Integration() {
        // 1. Arrange: Create a client for a container that DOES NOT exist
        BlobServiceClient serviceClient = realContainerClient.getServiceClient();
        BlobContainerClient nonExistentContainer = serviceClient.getBlobContainerClient("i-definitely-do-not-exist");

        // 2. Create a temporary manager using this broken client
        AzureBlobStorageImpl brokenManager = new AzureBlobStorageImpl(nonExistentContainer, null);

        // 3. Act & Assert: This will throw a 404 naturally without breaking other tests
        assertThrows(BlobStorageOperationException.class, () ->
                brokenManager.listFiles("user-1")
        );
    }

    @Test
    @DisplayName("Should successfully delete multiple files in a batch")
    void deleteFilesBatch_Success() {
        // Arrange
        String userId = "user123";
        String file1 = "photo.jpg";
        String file2 = "document.pdf";

        uploadFile(userId + "/" + file1, "content1");
        uploadFile(userId + "/" + file2, "content2");

        // Act
        azureBlobStorageImpl.deleteFilesBatch(userId, List.of(file1, file2));

        // Assert
        List<String> remainingBlobs = realContainerClient.listBlobs().stream()
                .map(BlobItem::getName)
                .toList();

        assertThat(remainingBlobs).isEmpty();
    }

    @Test
    @DisplayName("Should handle blob names that already include the path prefix")
    void deleteFilesBatch_HandlesExistingPrefix() {
        // Arrange
        String userId = "user456";
        String fullPath = userId + "/already-prefixed.txt";
        uploadFile(fullPath, "content");

        // Act - Passing the full path instead of just the filename
        azureBlobStorageImpl.deleteFilesBatch(userId, List.of(fullPath));

        // Assert
        assertThat(realContainerClient.getBlobClient(fullPath).exists()).isFalse();
    }

    @Test
    @DisplayName("Should return early and do nothing if the list is empty")
    void deleteFilesBatch_EmptyList() {
        // Act & Assert
        // This confirms no exception is thrown and the logic exits gracefully
        azureBlobStorageImpl.deleteFilesBatch("user123", List.of());
    }


    @Test
    @DisplayName("Should throw custom exception when the batch client fails entirely")
    void deleteFilesBatch_ServiceFailure() {
        // Arrange
        // Create a storage impl with a client pointing to a non-existent container to force a failure
        var badContainerClient = realContainerClient.getServiceClient().getBlobContainerClient("non-existent");
        var storageWithBadClient = new AzureBlobStorageImpl(badContainerClient, blobBatchClient);

        // Act & Assert
        assertThatThrownBy(() -> storageWithBadClient.deleteFilesBatch("user123", List.of("file.txt")))
                .isInstanceOf(BlobStorageOperationException.class)
                .hasMessageContaining("Cloud batch delete failed");
    }

    // Helper method to seed data into Azurite
    private void uploadFile(String path, String content) {
        byte[] data = content.getBytes();
        realContainerClient.getBlobClient(path)
                .upload(new ByteArrayInputStream(data), data.length);
    }

    private void uploadString(String blobPath) {
        realContainerClient.getBlobClient(blobPath)
                .upload(new ByteArrayInputStream("dummy-content".getBytes()), 13, true);
    }


}