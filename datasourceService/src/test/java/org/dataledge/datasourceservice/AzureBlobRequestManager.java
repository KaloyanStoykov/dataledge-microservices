package org.dataledge.datasourceservice;

import org.dataledge.datasourceservice.config.exceptions.BlobStorageOperationException;
import org.dataledge.datasourceservice.config.exceptions.InvalidUserException;
import org.dataledge.datasourceservice.dto.Storage;
import org.dataledge.datasourceservice.manager.IAzureBlobStorage;
import org.dataledge.datasourceservice.manager.impl.AzureBlobRequestManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiStorageServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private IAzureBlobStorage azureBlobStorage;

    @InjectMocks
    private AzureBlobRequestManager azureBlobRequestManager;
    @Mock
    private MultipartFile mockFile;

    @Test
    void saveAPIContentToBlob_ShouldSaveSuccessfully_WhenApiReturnsDataAndFileIsNew() throws IOException {
        // 1. ARRANGE
        String userId = "123";
        String fileName = "data.json";
        String apiUrl = "http://fake-api.com/data";
        String mockApiResponse = "{\"key\": \"value\"}";

        // Mock the API call
        when(restTemplate.getForObject(apiUrl, String.class)).thenReturn(mockApiResponse);

        // Mock the logic: File does NOT exist yet
        when(azureBlobStorage.exists(anyString())).thenReturn(false);

        // Mock the write operation
        when(azureBlobStorage.write(any(Storage.class))).thenReturn("user123/data.json");

        // 2. ACT
        String result = azureBlobRequestManager.saveAPIContentToBlob(apiUrl, fileName, userId);

        // 3. ASSERT
        assertThat(result).isEqualTo("API content successfully saved!");

        // Verify that the data passed to the write method matches the API response
        ArgumentCaptor<Storage> storageCaptor = ArgumentCaptor.forClass(Storage.class);
        verify(azureBlobStorage).write(storageCaptor.capture());

        Storage capturedStorage = storageCaptor.getValue();
        assertThat(capturedStorage.getFileName()).isEqualTo(fileName);

        // Read the stream from the captured storage to ensure content matches
        String actualContent = new String(capturedStorage.getFileData().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(actualContent).isEqualTo(mockApiResponse);
    }

    @Test
    void saveAPIContentToBlob_ShouldThrowException_WhenApiCallFails() {
        // 1. ARRANGE
        String userId = "123";
        String apiUrl = "http://bad-api.com";

        // Simulate API failure (e.g., 500 error or network timeout)
        when(restTemplate.getForObject(eq(apiUrl), eq(String.class)))
                .thenThrow(new RuntimeException("Network Error"));

        // 2. ACT & ASSERT
        BlobStorageOperationException ex = assertThrows(BlobStorageOperationException.class, () -> azureBlobRequestManager.saveAPIContentToBlob(apiUrl, "file.txt", userId));

        assertThat(ex.getMessage()).contains("Failed to call external API");

        // Ensure we never tried to write to Azure if API failed
        verifyNoInteractions(azureBlobStorage);
    }

    @Test
    void saveAPIContentToBlob_ShouldThrowException_WhenApiReturnsNull() {
        // 1. ARRANGE
        String userId = "12";
        String apiUrl = "http://empty-api.com";

        when(restTemplate.getForObject(apiUrl, String.class)).thenReturn(null);

        // 2. ACT & ASSERT
        BlobStorageOperationException ex = assertThrows(BlobStorageOperationException.class, () -> azureBlobRequestManager.saveAPIContentToBlob(apiUrl, "file.txt", userId));

        assertThat(ex.getMessage()).contains("API returned no content");
        verifyNoInteractions(azureBlobStorage);
    }

    @Test
    void saveAPIContentToBlob_ShouldThrowException_WhenFileAlreadyExists() throws IOException {
        // 1. ARRANGE
        String userId = "12";
        String fileName = "duplicate.json";
        String apiUrl = "http://fake-api.com";

        when(restTemplate.getForObject(apiUrl, String.class)).thenReturn("some content");

        // Simulate that the file ALREADY exists
        when(azureBlobStorage.exists(anyString())).thenReturn(true);

        // 2. ACT & ASSERT
        BlobStorageOperationException ex = assertThrows(BlobStorageOperationException.class, () -> azureBlobRequestManager.saveAPIContentToBlob(apiUrl, fileName, userId));

        assertThat(ex.getMessage()).contains("File already exists");

        // CRITICAL: Ensure we did NOT overwrite the file
        verify(azureBlobStorage, never()).write(any());
    }

    @Test
    void writeFileToBlob_ShouldSave_WhenCustomNameProvided_AndFileIsNew() throws IOException {
        // 1. ARRANGE
        String userId = "15";
        String requestedName = "custom-name.pdf";
        byte[] content = "dummy-pdf-content".getBytes();

        // Mock MultipartFile behavior
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));
        when(mockFile.getSize()).thenReturn((long) content.length);

        // Mock Storage behavior
        when(azureBlobStorage.exists(anyString())).thenReturn(false); // File doesn't exist
        when(azureBlobStorage.write(any(Storage.class))).thenReturn(userId + "/" + requestedName);

        // 2. ACT
        String result = azureBlobRequestManager.writeFileToBlob(mockFile, requestedName, userId);

        // 3. ASSERT
        assertThat(result).isEqualTo("File created successfully!");

        // Verify we checked for the CUSTOM name, not the original name
        verify(azureBlobStorage).exists(contains(requestedName));

        // Verify the DTO contained the right data
        ArgumentCaptor<Storage> captor = ArgumentCaptor.forClass(Storage.class);
        verify(azureBlobStorage).write(captor.capture());

        Storage capturedStorage = captor.getValue();
        assertThat(capturedStorage.getFileName()).isEqualTo(requestedName);
        assertThat(capturedStorage.getContentLength()).isEqualTo(content.length);
    }

    @Test
    void writeFileToBlob_ShouldUseOriginalName_WhenRequestedNameIsNull() throws IOException {
        // 1. ARRANGE
        String userId = "13";
        String originalName = "original.jpg";

        when(mockFile.getOriginalFilename()).thenReturn(originalName);
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(azureBlobStorage.exists(anyString())).thenReturn(false);

        // 2. ACT
        // Pass null as the requestedFileName
        azureBlobRequestManager.writeFileToBlob(mockFile, null, userId);

        // 3. ASSERT
        // Verify we checked for the ORIGINAL name
        verify(azureBlobStorage).exists(contains(originalName));

        ArgumentCaptor<Storage> captor = ArgumentCaptor.forClass(Storage.class);
        verify(azureBlobStorage).write(captor.capture());
        assertThat(captor.getValue().getFileName()).isEqualTo(originalName);
    }

    @Test
    void writeFileToBlob_ShouldThrowException_WhenNoFilenameAvailable() {
        // 1. ARRANGE
        // Requested name is empty, AND original filename is null (edge case)
        when(mockFile.getOriginalFilename()).thenReturn(null);

        // 2. ACT & ASSERT
        BlobStorageOperationException ex = assertThrows(BlobStorageOperationException.class, () -> azureBlobRequestManager.writeFileToBlob(mockFile, "", "1"));

        assertThat(ex.getMessage()).contains("File name must be provided");
        verifyNoInteractions(azureBlobStorage);
    }

    @Test
    void writeFileToBlob_ShouldThrowException_WhenFileAlreadyExists() throws IOException {
        // 1. ARRANGE
        String userId = "1";
        String fileName = "duplicate.txt";

        when(azureBlobStorage.exists(anyString())).thenReturn(true); // Return TRUE

        // 2. ACT & ASSERT
        BlobStorageOperationException ex = assertThrows(BlobStorageOperationException.class, () -> azureBlobRequestManager.writeFileToBlob(mockFile, fileName, userId));

        assertThat(ex.getMessage()).contains("File already exists");

        // Critical: Make sure we didn't try to upload data
        verify(azureBlobStorage, never()).write(any());
    }

    @Test
    void writeFileToBlob_ShouldThrowException_WhenInputStreamFails() throws IOException {
        // 1. ARRANGE
        String userId = "1";
        String fileName = "corrupt.txt";

        when(azureBlobStorage.exists(anyString())).thenReturn(false);

        // Simulate a broken file stream
        when(mockFile.getInputStream()).thenThrow(new IOException("Stream broken"));

        // 2. ACT & ASSERT
        BlobStorageOperationException ex = assertThrows(BlobStorageOperationException.class, () -> azureBlobRequestManager.writeFileToBlob(mockFile, fileName, userId));

        assertThat(ex.getMessage()).contains("Error processing file upload");
    }

    @Test
    void sanitizeUserId_ShouldReturnId_WhenInputIsValid() {
        // Act
        String result = azureBlobRequestManager.sanitizeUserId("12345");

        // Assert
        assertThat(result).isEqualTo("12345");
    }

    @Test
    void sanitizeUserId_ShouldTrimSpaces_WhenInputHasWhitespace() {
        // Act
        String result = azureBlobRequestManager.sanitizeUserId("  9988  ");

        // Assert
        assertThat(result).isEqualTo("9988");
    }

    // --- SAD PATH TESTS (It throws exceptions) ---

    @Test
    void sanitizeUserId_ShouldThrowException_WhenIdIsNull() {
        InvalidUserException ex = assertThrows(InvalidUserException.class, () -> azureBlobRequestManager.sanitizeUserId(null));

        assertThat(ex.getMessage()).isEqualTo("User ID cannot be empty.");
    }

    @Test
    void sanitizeUserId_ShouldThrowException_WhenIdIsEmpty() {
        InvalidUserException ex = assertThrows(InvalidUserException.class, () -> azureBlobRequestManager.sanitizeUserId(""));

        assertThat(ex.getMessage()).isEqualTo("User ID cannot be empty.");
    }


    @ParameterizedTest
    @ValueSource(strings = {"abc", "12a34", "12-34", "12.34", "user_1"})
    void sanitizeUserId_ShouldThrowException_ForNonNumericFormats(String invalidInput) {
        InvalidUserException ex = assertThrows(InvalidUserException.class, () -> azureBlobRequestManager.sanitizeUserId(invalidInput));

        assertThat(ex.getMessage()).contains("Invalid User ID format");
    }
}
