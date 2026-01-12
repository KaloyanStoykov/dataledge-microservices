package org.dataledge.datasourceservice;

import org.dataledge.datasourceservice.config.exceptions.InvalidUserException;
import org.dataledge.datasourceservice.data.datasources.DataSource;
import org.dataledge.datasourceservice.data.datasources.DataSourceRepo;
import org.dataledge.datasourceservice.data.filesnaps.BlobMetadata;
import org.dataledge.datasourceservice.data.filesnaps.BlobMetadataRepo;
import org.dataledge.datasourceservice.dto.blobMetadataDTO.GetPagedBlobMetadataResponse;
import org.dataledge.datasourceservice.manager.impl.BlobMetadataManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlobMetadataManagerTest {

    @Mock
    private BlobMetadataRepo blobMetadataRepo;

    @Mock
    private DataSourceRepo dataSourceRepo;

    @InjectMocks
    private BlobMetadataManager blobMetadataManager;

    @Test
    void sanitizeUserId_validId_returnsTrimmedString() {
        String result = blobMetadataManager.sanitizeUserId("  123  ");
        assertEquals("123", result);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void sanitizeUserId_invalidInput_throwsInvalidUserException(String invalidId) {
        assertThrows(InvalidUserException.class, () ->
                blobMetadataManager.sanitizeUserId(invalidId)
        );
    }

    @Test
    void sanitizeUserId_invalidFormat_throwsInvalidUserException() {
        assertThrows(InvalidUserException.class, () ->
                blobMetadataManager.sanitizeUserId("user_123")
        );
    }

    @Test
    void createBlobMetadata_success_savesCorrectData() {
        int userId = 1;
        String fileName = "upload.json";
        DataSource mockDs = new DataSource();
        mockDs.setId(500L);

        ArgumentCaptor<BlobMetadata> captor = ArgumentCaptor.forClass(BlobMetadata.class);

        String result = blobMetadataManager.createBlobMetadata(userId, fileName, mockDs);

        assertEquals("Blob reference set successfully", result);

        verify(blobMetadataRepo).save(captor.capture());

        BlobMetadata capturedMetadata = captor.getValue();
        assertEquals(fileName, capturedMetadata.getFileName());
        assertEquals(userId, capturedMetadata.getUserId());
        assertEquals(mockDs, capturedMetadata.getDataSource());
        assertNotNull(capturedMetadata.getCreated());
    }

    @Test
    void deleteMetadataBatch_Success() {
        int userId = 1;
        List<String> files = List.of("file1.png", "file2.jpg");

        blobMetadataManager.deleteMetadataBatch(userId, files);

        verify(blobMetadataRepo, times(1)).deleteByUserIdAndBlobNames(userId, files);
    }

    @Test
    void deleteMetadataBatch_EmptyList_DoesNotCallRepo() {
        // Act
        blobMetadataManager.deleteMetadataBatch(1, Collections.emptyList());
        blobMetadataManager.deleteMetadataBatch(1, null);

        verifyNoInteractions(blobMetadataRepo);
    }

    @Test
    void deleteMetadataBatch_RepositoryThrowsException_WrapsInRuntimeException() {
        int userId = 1;
        List<String> files = List.of("bad_file.txt");

        doThrow(new RuntimeException("DB Error"))
                .when(blobMetadataRepo).deleteByUserIdAndBlobNames(anyInt(), anyList());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                blobMetadataManager.deleteMetadataBatch(userId, files)
        );

        assertThat(exception.getMessage()).contains("Could not sync metadata deletion");
        assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void getBlobsForDatasources_success_returnsPagedResponse() {
        int userId = 1;
        int dsId = 101;
        Pageable pageable = PageRequest.of(0, 10);

        BlobMetadata meta = new BlobMetadata(1L, "test.json", Instant.now(), userId, null);
        Page<BlobMetadata> page = new PageImpl<>(List.of(meta), pageable, 1);

        when(blobMetadataRepo.findByUserAndDataSource(
                eq(userId),
                anyLong(),
                any(Pageable.class)
        )).thenReturn(page);

        GetPagedBlobMetadataResponse response = blobMetadataManager.getBlobsForDatasources("1", dsId, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getBlobsData().size());
        assertEquals("test.json", response.getBlobsData().get(0).getName());

        verify(blobMetadataRepo).findByUserAndDataSource(eq(userId), anyLong(), any(Pageable.class));
    }

    @Test
    void deleteMetadataBatch_emptyList_doesNothing() {
        blobMetadataManager.deleteMetadataBatch(1, List.of());

        verify(blobMetadataRepo, never()).deleteByUserIdAndBlobNames(anyInt(), anyList());
    }
}
