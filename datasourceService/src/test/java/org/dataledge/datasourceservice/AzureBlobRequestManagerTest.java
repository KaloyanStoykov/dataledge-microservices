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
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AzureBlobRequestManagerTest {

    @Mock
    private IAzureBlobStorage azureBlobStorage;

    @Mock
    private MultipartFile mockFile;

    // @Spy wraps the real instance created by @InjectMocks.
    // This allows us to stub specific methods (like fetchSecurely) while running the rest of the real code.
    @Spy
    @InjectMocks
    private AzureBlobRequestManager azureBlobRequestManager;

    // -------------------------------------------------------------------
    // Tests for saveAPIContentToBlob (High-level orchestration)
    // -------------------------------------------------------------------

    @Test
    void saveAPIContentToBlob_ShouldSaveSuccessfully_WhenApiReturnsDataAndFileIsNew() throws IOException {
        String userId = "123";
        String fileName = "data.json";
        String apiUrl = "https://fake-api.com/data";
        byte[] mockContent = "{\"some\": \"json data\"}".getBytes();

        // 1. Stub the internal secure fetch method to avoid real network calls
        doReturn(mockContent).when(azureBlobRequestManager).fetchSecurely(anyString());

        // 2. Stub storage interactions
        when(azureBlobStorage.exists(anyString())).thenReturn(false);
        when(azureBlobStorage.write(any(Storage.class))).thenReturn("https://azure.com/blob/url");

        // 3. Act
        String result = azureBlobRequestManager.saveAPIContentToBlob(apiUrl, fileName, userId);

        // 4. Assert
        assertEquals("API content successfully saved!", result);

        // Verify correct storage call
        ArgumentCaptor<Storage> storageCaptor = ArgumentCaptor.forClass(Storage.class);
        verify(azureBlobStorage).write(storageCaptor.capture());

        Storage capturedStorage = storageCaptor.getValue();
        assertEquals(userId, capturedStorage.getUserId());
        assertEquals(fileName, capturedStorage.getFileName());
        assertEquals(mockContent.length, capturedStorage.getContentLength());
    }

    @Test
    void saveAPIContentToBlob_ShouldThrowException_WhenApiCallFails() throws IOException {
        String userId = "123";
        String apiUrl = "https://bad-api.com";
        String fileName = "file.txt";

        // We don't need to mock exists() because the network fail happens first (or we can use lenient)
        lenient().when(azureBlobStorage.exists(anyString())).thenReturn(false);

        // Force the protected method to throw the exception
        doThrow(new BlobStorageOperationException("Failed to call external API"))
                .when(azureBlobRequestManager).fetchSecurely(anyString());

        BlobStorageOperationException ex = assertThrows(BlobStorageOperationException.class,
                () -> azureBlobRequestManager.saveAPIContentToBlob(apiUrl, fileName, userId));

        assertThat(ex.getMessage()).contains("Failed to call external API");
        verify(azureBlobStorage, never()).write(any());
    }

    @Test
    void saveAPIContentToBlob_ShouldThrowException_WhenApiReturnsNull() throws IOException {
        String userId = "12";
        String apiUrl = "https://empty-api.com";
        String fileName = "file.txt";

        lenient().when(azureBlobStorage.exists(anyString())).thenReturn(false);

        // Force the protected method to return empty bytes
        doReturn(new byte[0]).when(azureBlobRequestManager).fetchSecurely(anyString());

        BlobStorageOperationException ex = assertThrows(BlobStorageOperationException.class,
                () -> azureBlobRequestManager.saveAPIContentToBlob(apiUrl, fileName, userId));

        assertThat(ex.getMessage()).contains("API returned no content");
        verify(azureBlobStorage, never()).write(any());
    }

    @Test
    void saveAPIContentToBlob_ShouldThrowException_WhenFileAlreadyExists() throws IOException {
        String userId = "12";
        String fileName = "duplicate.json";
        String apiUrl = "https://any-url-is-fine-here.com";

        // Logic check: If file exists, we shouldn't even attempt the network call
        when(azureBlobStorage.exists(anyString())).thenReturn(true);

        BlobStorageOperationException ex = assertThrows(BlobStorageOperationException.class,
                () -> azureBlobRequestManager.saveAPIContentToBlob(apiUrl, fileName, userId));

        assertThat(ex.getMessage()).contains("File already exists");
        verify(azureBlobStorage, never()).write(any());
    }

    // -------------------------------------------------------------------
    // Tests for fetchSecurely (Low-level network/security logic)
    // -------------------------------------------------------------------

    @Test
    void fetchSecurely_ShouldThrowException_WhenProtocolIsNotHttps() {
        String unsafeUrl = "http://google.com";

        BlobStorageOperationException ex = assertThrows(BlobStorageOperationException.class,
                () -> azureBlobRequestManager.fetchSecurely(unsafeUrl));

        assertEquals("Only HTTPS allowed", ex.getMessage());
    }

    @Test
    void fetchSecurely_ShouldThrowException_WhenIpIsInternal() throws Exception {
        String url = "https://internal-service.com";

        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        doReturn(loopback).when(azureBlobRequestManager).resolveHost(anyString());

        BlobStorageOperationException ex = assertThrows(BlobStorageOperationException.class,
                () -> azureBlobRequestManager.fetchSecurely(url));

        assertEquals("Could not validate host IP", ex.getMessage());
    }

    @Test
    void fetchSecurely_ShouldReturnContent_WhenCheckPassesAndApiWorks() throws Exception {
        String url = "https://good-api.com/file.json";
        String expectedContent = "{\"status\":\"ok\"}";

        InetAddress publicIp = InetAddress.getByName("8.8.8.8");
        doReturn(publicIp).when(azureBlobRequestManager).resolveHost(anyString());

        HttpURLConnection mockConnection = mock(HttpURLConnection.class);
        doReturn(mockConnection).when(azureBlobRequestManager).createConnection(any(URL.class));

        when(mockConnection.getResponseCode()).thenReturn(200);
        when(mockConnection.getInputStream()).thenReturn(new ByteArrayInputStream(expectedContent.getBytes()));

        byte[] result = azureBlobRequestManager.fetchSecurely(url);

        assertNotNull(result);
        assertEquals(expectedContent, new String(result));
    }

    @Test
    void fetchSecurely_ShouldThrowException_WhenApiReturnsNon200() throws Exception {
        String url = "https://broken-api.com";

        InetAddress publicIp = InetAddress.getByName("8.8.8.8");
        doReturn(publicIp).when(azureBlobRequestManager).resolveHost(anyString());

        HttpURLConnection mockConnection = mock(HttpURLConnection.class);
        doReturn(mockConnection).when(azureBlobRequestManager).createConnection(any(URL.class));

        when(mockConnection.getResponseCode()).thenReturn(500);

        BlobStorageOperationException ex = assertThrows(BlobStorageOperationException.class,
                () -> azureBlobRequestManager.fetchSecurely(url));

        assertTrue(ex.getMessage().contains("Code: 500"));
    }


    @Test
    void writeFileToBlob_ShouldSave_WhenCustomNameProvided_AndFileIsNew() throws IOException {
        String userId = "15";
        String requestedName = "custom-name.pdf";
        byte[] content = "dummy-pdf-content".getBytes();

        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(content));
        when(mockFile.getSize()).thenReturn((long) content.length);

        when(azureBlobStorage.exists(anyString())).thenReturn(false);
        when(azureBlobStorage.write(any(Storage.class))).thenReturn(userId + "/" + requestedName);

        String result = azureBlobRequestManager.writeFileToBlob(mockFile, requestedName, userId);

        assertThat(result).isEqualTo("File created successfully!");

        // Use contains logic or exact match depending on how your service constructs the path
        verify(azureBlobStorage).exists(argThat(path -> path.endsWith(requestedName)));

        ArgumentCaptor<Storage> captor = ArgumentCaptor.forClass(Storage.class);
        verify(azureBlobStorage).write(captor.capture());

        Storage capturedStorage = captor.getValue();
        assertThat(capturedStorage.getFileName()).isEqualTo(requestedName);
        assertThat(capturedStorage.getContentLength()).isEqualTo(content.length);
    }

    @Test
    void writeFileToBlob_ShouldUseOriginalName_WhenRequestedNameIsNull() throws IOException {
        String userId = "13";
        String originalName = "original.jpg";

        when(mockFile.getOriginalFilename()).thenReturn(originalName);
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(azureBlobStorage.exists(anyString())).thenReturn(false);

        azureBlobRequestManager.writeFileToBlob(mockFile, null, userId);

        ArgumentCaptor<Storage> captor = ArgumentCaptor.forClass(Storage.class);
        verify(azureBlobStorage).write(captor.capture());
        assertThat(captor.getValue().getFileName()).isEqualTo(originalName);
    }

    @Test
    void writeFileToBlob_ShouldThrowException_WhenNoFilenameAvailable() {
        when(mockFile.getOriginalFilename()).thenReturn(null);

        BlobStorageOperationException ex = assertThrows(BlobStorageOperationException.class,
                () -> azureBlobRequestManager.writeFileToBlob(mockFile, "", "1"));

        assertThat(ex.getMessage()).contains("File name must be provided");
        verifyNoInteractions(azureBlobStorage);
    }

    @Test
    void writeFileToBlob_ShouldThrowException_WhenFileAlreadyExists() throws IOException {
        String userId = "1";
        String fileName = "duplicate.txt";

        when(azureBlobStorage.exists(anyString())).thenReturn(true);

        BlobStorageOperationException ex = assertThrows(BlobStorageOperationException.class,
                () -> azureBlobRequestManager.writeFileToBlob(mockFile, fileName, userId));

        assertThat(ex.getMessage()).contains("File already exists");
        verify(azureBlobStorage, never()).write(any());
    }

    @Test
    void writeFileToBlob_ShouldThrowException_WhenInputStreamFails() throws IOException {
        String userId = "1";
        String fileName = "corrupt.txt";

        when(azureBlobStorage.exists(anyString())).thenReturn(false);
        when(mockFile.getInputStream()).thenThrow(new IOException("Stream broken"));

        BlobStorageOperationException ex = assertThrows(BlobStorageOperationException.class,
                () -> azureBlobRequestManager.writeFileToBlob(mockFile, fileName, userId));

        assertThat(ex.getMessage()).contains("Error processing file upload");
    }


    @Test
    void sanitizeUserId_ShouldReturnId_WhenInputIsValid() {
        String result = azureBlobRequestManager.sanitizeUserId("12345");
        assertThat(result).isEqualTo("12345");
    }

    @Test
    void sanitizeUserId_ShouldTrimSpaces_WhenInputHasWhitespace() {
        String result = azureBlobRequestManager.sanitizeUserId("  9988  ");
        assertThat(result).isEqualTo("9988");
    }

    @Test
    void sanitizeUserId_ShouldThrowException_WhenIdIsNull() {
        InvalidUserException ex = assertThrows(InvalidUserException.class,
                () -> azureBlobRequestManager.sanitizeUserId(null));
        assertThat(ex.getMessage()).isEqualTo("User ID cannot be empty.");
    }

    @Test
    void sanitizeUserId_ShouldThrowException_WhenIdIsEmpty() {
        InvalidUserException ex = assertThrows(InvalidUserException.class,
                () -> azureBlobRequestManager.sanitizeUserId(""));
        assertThat(ex.getMessage()).isEqualTo("User ID cannot be empty.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12a34", "12-34", "12.34", "user_1"})
    void sanitizeUserId_ShouldThrowException_ForNonNumericFormats(String invalidInput) {
        InvalidUserException ex = assertThrows(InvalidUserException.class,
                () -> azureBlobRequestManager.sanitizeUserId(invalidInput));
        assertThat(ex.getMessage()).contains("Invalid User ID format");
    }
}