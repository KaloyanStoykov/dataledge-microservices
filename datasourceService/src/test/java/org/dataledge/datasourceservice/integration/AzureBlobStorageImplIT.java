package org.dataledge.datasourceservice.integration;

import com.azure.storage.blob.*;
import com.azure.storage.blob.batch.BlobBatchClient;
import com.azure.storage.blob.batch.BlobBatchClientBuilder;
import net.bytebuddy.utility.RandomString;
import org.dataledge.datasourceservice.config.exceptions.BlobStorageOperationException;
import org.dataledge.datasourceservice.dto.Storage;
import org.dataledge.datasourceservice.manager.impl.AzureBlobStorageImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import java.io.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.GenericContainer;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

    private AzureBlobStorageImpl azureBlobStorageImpl;

    @BeforeEach
    void setUp() {
        Integer blobPort = AZURITE_CONTAINER.getMappedPort(10000);
        String connectionString = String.format(
                "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:%s/devstoreaccount1;",
                blobPort
        );

        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        BlobBatchClient blobBatchClient = new BlobBatchClientBuilder(blobServiceClient).buildClient();

        String containerName = RandomString.make().toLowerCase();
        realContainerClient = blobServiceClient.createBlobContainer(containerName);


        azureBlobStorageImpl = new AzureBlobStorageImpl(
                realContainerClient,
                blobBatchClient
        );
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
        // 1. ARRANGE
        // Create a unique folder name so this test doesn't clash with others
        String folderName = "user-23";
        // We expect the service to look for "folderName/"
        // Let's seed the container with 3 items:
        // 1. A standard file in the folder (Should be returned)
        String file1 = folderName + "/data.csv";
        // 2. Another standard file (Should be returned)
        String file2 = folderName + "/image.png";
        // 3. A file inside a subfolder (Should NOT be returned directly,
        //    and the "subfolder/" prefix should be filtered out by your code)
        String nestedFile = folderName + "/subfolder/hidden.txt";

        // Helper to upload content quickly using the Raw Client
        uploadString(file1);
        uploadString(file2);
        uploadString(nestedFile);


        // 2. ACT
        List<String> results = azureBlobStorageImpl.listFiles("user-23");

        // 3. ASSERT
        // We expect exactly 2 items.
        // The nested file is hidden because of hierarchy.
        // The "subfolder/" entry is hidden because your code filters (!isPrefix).
        assertThat(results).isNotNull();
        assertThat(results.size()).isEqualTo(2);

        // AssertJ helper to check contents regardless of order
        assertThat(results).containsExactlyInAnyOrder(file1, file2);
    }

    @Test
    void listFiles_ShouldReturnEmptyList_WhenFolderIsNew() {
        // 2. ACT
        List<String> results = azureBlobStorageImpl.listFiles("user-1");

        // 3. ASSERT
        assertThat(results).isNotNull();
        assertThat(results.size()).isEqualTo(0);
    }

    @Test
    void listFiles_ShouldThrowException_WhenContainerMissing_Integration() {
        // 1. ARRANGE
        // We purposefully "break" the environment by deleting the container
        realContainerClient.delete();

        try {

            // 2. ACT & ASSERT
            // Azure SDK throws BlobStorageException (404) -> Your code wraps it in BlobStorageOperationException
            assertThrows(BlobStorageOperationException.class, () -> azureBlobStorageImpl.listFiles("user-1"));

        } finally {
            // 3. CLEANUP (CRITICAL)
            // We MUST recreate the container, otherwise all subsequent tests in this class will fail!
            realContainerClient.create();
        }
    }

    @Test
    void deleteFilesBatch_ShouldDeleteOnlySpecificUserFiles_Integration() {
        // 1. ARRANGE
        String userId = "user-delete-test";

        // Create 3 files:
        // Two belong to the user
        String file1 = userId + "/doc1.txt";
        String file2 = userId + "/doc2.txt";
        // One belongs to a DIFFERENT user (Security Check)
        String otherUserFile = "user-other/secret.txt";

        uploadString(file1);
        uploadString(file2);
        uploadString(otherUserFile);

        // Input list contains the user's files AND the other user's file (simulating a malicious request)
        List<String> filesToDelete = List.of(file1, file2, otherUserFile);

        // 2. ACT
        azureBlobStorageImpl.deleteFilesBatch(userId, filesToDelete);

        // 3. ASSERT
        // The user's files should be gone
        assertThat(realContainerClient.getBlobClient(file1).exists()).isFalse();
        assertThat(realContainerClient.getBlobClient(file2).exists()).isFalse();

        // The OTHER user's file must still exist (because of the startsWith filter in your code)
        assertThat(realContainerClient.getBlobClient(otherUserFile).exists()).isTrue();
    }

    @Test
    void deleteFilesBatch_ShouldDoNothing_WhenListIsEmpty() {
        // 1. ARRANGE
        String userId = "user-empty-batch";
        String file1 = userId + "/keep-me.txt";
        uploadString(file1);

        // 2. ACT
        azureBlobStorageImpl.deleteFilesBatch(userId, List.of());

        // 3. ASSERT
        // File should still exist because list was empty
        assertThat(realContainerClient.getBlobClient(file1).exists()).isTrue();
    }

    @Test
    void deleteFilesBatch_ShouldThrowException_WhenContainerMissing() {
        // 1. ARRANGE
        // Simulate a major failure (container deleted)
        realContainerClient.delete();

        try {
            String userId = "user-fail";
            List<String> files = List.of(userId + "/file1.txt");

            // 2. ACT & ASSERT
            // The batch client will attempt to execute and fail because the container is gone
            assertThrows(BlobStorageOperationException.class, () ->
                    azureBlobStorageImpl.deleteFilesBatch(userId, files)
            );

        } finally {
            // 3. CLEANUP
            // Must recreate container for other tests
            realContainerClient.create();
        }
    }

    private void uploadString(String blobPath) {
        realContainerClient.getBlobClient(blobPath)
                .upload(new ByteArrayInputStream("dummy-content".getBytes()), 13, true);
    }


}