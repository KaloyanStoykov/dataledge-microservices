package org.dataledge.datasourceservice;

import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import org.dataledge.datasourceservice.data.DataType;
import org.dataledge.datasourceservice.data.DataTypeRepo;
import org.dataledge.datasourceservice.data.datasources.DataSource;
import org.dataledge.datasourceservice.data.datasources.DataSourceRepo;
import org.dataledge.datasourceservice.data.filesnaps.BlobMetadataRepo;
import org.dataledge.datasourceservice.dto.datasourcesDTO.*;
import org.dataledge.datasourceservice.manager.IAzureBlobStorage;
import org.dataledge.datasourceservice.manager.IBlobMetadataManager;
import org.dataledge.datasourceservice.manager.IDataSourceMapper;
import org.dataledge.datasourceservice.manager.impl.DataSourceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DataSourceManagerTest {
    @Mock
    private DataSourceRepo dataSourceRepo;

    @Mock
    private DataTypeRepo dataTypeRepo;
    @Mock
    private BlobMetadataRepo blobMetadataManager;

    @Mock
    private IAzureBlobStorage azureBlobStorage;

    @Mock
    private IDataSourceMapper mapper;

    @InjectMocks
    private DataSourceManager dataSourceManager;

    private DataSource existingDataSource;
    private UpdateDataSourceRequest updateRequest;
    private DataType newType;

    @BeforeEach
    void setUp() {
        existingDataSource = new DataSource();
        existingDataSource.setId(100L);
        existingDataSource.setUserId(1);
        existingDataSource.setName("Old Name");
        existingDataSource.setUrl("http://example.com");

        updateRequest = new UpdateDataSourceRequest();
        updateRequest.setName("New Name");
        updateRequest.setDescription("New Description");
        updateRequest.setTypeId(200L);

        newType = new DataType();
        newType.setId(200L);
        newType.setName("SQL");
    }

    @Test
    void updateDataSource_Success() {
        // Arrange
        when(dataSourceRepo.findById(100)).thenReturn(Optional.of(existingDataSource));
        when(dataTypeRepo.findById(200L)).thenReturn(Optional.of(newType));
        when(dataSourceRepo.save(any(DataSource.class))).thenReturn(existingDataSource);

        // Act
        UpdateDataSourceResponse response = dataSourceManager.updateDataSource("1", 100, updateRequest);

        // Assert
        assertNotNull(response);
        assertEquals("New Name", response.getName());
        assertEquals(100, response.getId());
        verify(dataSourceRepo).save(existingDataSource);
    }

    @Test
    void updateDataSource_ThrowsForbiddenException_WhenUserNotOwner() {
        // Arrange
        when(dataSourceRepo.findById(100)).thenReturn(Optional.of(existingDataSource));

        // Act & Assert
        assertThrows(ForbiddenException.class, () -> {
            dataSourceManager.updateDataSource("99", 100, updateRequest);
        });
    }

    @Test
    void updateDataSource_ThrowsNotFoundException_WhenUserIdInvalid() {
        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            dataSourceManager.updateDataSource("abc", 100, updateRequest);
        });
    }

    @Test
    void updateDataSource_ThrowsEntityNotFoundException_WhenDataSourceMissing() {
        // Arrange
        when(dataSourceRepo.findById(anyInt())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            dataSourceManager.updateDataSource("1", 100, updateRequest);
        });
    }

    @Test
    void getDataSources_success_returnsData() {
        // Arrange
        int pageNumber = 0;
        int pageSize = 10;
        long totalElements = 2;
        int userIdInt = 1;
        String userIdStr = "1";

        DataType dataType = new DataType(1L, "API", "API to work with datasources", null);

        DataSource entity1 = DataSource.builder().id(1L).name("Source 1").type(dataType).userId(userIdInt).build();
        DataSource entity2 = DataSource.builder().id(2L).name("Source 2").type(dataType).userId(userIdInt).build();

        DataSourceResponse dto1 = new DataSourceResponse(1L, "Source 1", dataType, "Desc", "url", Instant.now(), new Date());
        DataSourceResponse dto2 = new DataSourceResponse(2L, "Source 2", dataType, "Desc", "url", Instant.now(), new Date());

        List<DataSource> entities = List.of(entity1, entity2);

        // Note: The Sort order in the mock must match the Sort order in the Manager code exactly
        // or you can just use any(Pageable.class)
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("created").descending());
        Page<DataSource> page = new PageImpl<>(entities, pageable, totalElements);

        // FIX: Mock the findAll(Specification, Pageable) method
        when(dataSourceRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        when(mapper.toDataSourceResponse(entity1)).thenReturn(dto1);
        when(mapper.toDataSourceResponse(entity2)).thenReturn(dto2);

        // Act
        GetDataSourcesResponse response = dataSourceManager.getDataSources(userIdStr, pageNumber, pageSize, "");

        // Assert
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getTotalCount()).isEqualTo(totalElements);
        assertThat(response.getPage()).isEqualTo(pageNumber);

        // Verify the correct repository method was called
        verify(dataSourceRepo).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getDataSources_throwsNotFound_whenPageIsEmpty() {
        // Arrange
        int userIdInt = 1;
        // Note: We use any() for the Specification and Pageable to avoid exact match issues
        when(dataSourceRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of())); // Return empty list

        // Act & Assert
        assertThrows(NotFoundException.class, () ->
                dataSourceManager.getDataSources("1", 0, 10, null)
        );
    }

    @Test
    void getDataSources_throwsNotFound_whenUserIdIsInvalid() {
        // Arrange
        String invalidUserId = "abc";

        // Act & Assert
        NumberFormatException thrown = assertThrows(NumberFormatException.class, () ->
                dataSourceManager.getDataSources(invalidUserId, 0, 10, "")
        );

        assertThat(thrown.getMessage()).contains(invalidUserId);
        verifyNoInteractions(dataSourceRepo);
    }

    @Test
    void createDataSource_success_returnsData() {
        // Arrange
        CreateDataSourceRequest request = new CreateDataSourceRequest();
        DataType dataType = new DataType(1L, "API", "API to work with datasources", null);

        request.setTypeId(dataType.getId());
        request.setName("Test Name");
        request.setDescription("Test Description");
        request.setUrl("jdbc:postgresql://localhost:5432/test");

        DataSource savedEntity = DataSource.builder()
                .id(1L)
                .name("Test Name")
                .type(dataType)
                .description("Test Description")
                .url("http://test-url.com")
                .created(Instant.now())
                .updated(new Date())
                .userId(1) // explicitly set the userId
                .build();


        when(dataSourceRepo.save(any(DataSource.class))).thenReturn(savedEntity);
        when(dataTypeRepo.findById(1L)).thenReturn(Optional.of(dataType));

        CreateDataSourceResponse response = dataSourceManager.createDataSource("1", request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test Name");
        assertThat(response.getId()).isEqualTo(dataType.getId());

        ArgumentCaptor<DataSource> captor = ArgumentCaptor.forClass(DataSource.class);
        verify(dataSourceRepo, times(1)).save(captor.capture());

        DataSource passedEntity = captor.getValue();
        assertThat(passedEntity.getId()).isNull(); // should be null before save
        assertThat(passedEntity.getName()).isEqualTo("Test Name");
        assertThat(passedEntity.getDescription()).isEqualTo("Test Description");
        assertThat(passedEntity.getUrl()).isEqualTo("jdbc:postgresql://localhost:5432/test");
        assertThat(passedEntity.getCreated()).isNotNull(); // assuming that's what Instant.now() is mapped to
    }

    @Test
    void createDataSource_unknownType_throwsNotFoundException() {
        String userId = "1";
        long nonExistentTypeId = 99L;

        CreateDataSourceRequest request = new CreateDataSourceRequest();
        request.setTypeId(nonExistentTypeId);
        request.setName("Test Name");

        when(dataTypeRepo.findById(nonExistentTypeId)).thenReturn(Optional.empty());

        NotFoundException thrown = assertThrows(NotFoundException.class, () -> dataSourceManager.createDataSource(userId, request));

        assertThat(thrown.getMessage()).isEqualTo("Unknown datasource type");

        verify(dataTypeRepo).findById(nonExistentTypeId);
        verify(dataSourceRepo, never()).save(any(DataSource.class));
    }

    @Test
    void deleteDataSource_success() {
        int dataSourceId = 1;
        DataSource mockEntity = DataSource.builder()
                .id((long) dataSourceId)
                .name("To Be Deleted")
                .userId(1)
                .build();

        when(dataSourceRepo.findById(dataSourceId)).thenReturn(Optional.of(mockEntity));

        DeleteDataSourceResponse response = dataSourceManager.deleteDataSource("1", dataSourceId);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Datasource and all associated cloud files deleted successfully!");

        verify(dataSourceRepo).findById(dataSourceId);
        verify(dataSourceRepo).delete(mockEntity);
    }

    @Test
    void deleteDataSource_invalidUserIdFormat_throwsNotFoundException() {
        int dataSourceId = 1;
        String invalidUserId = "notANumber";

        DataSource mockEntity = DataSource.builder()
                .id((long) dataSourceId)
                .name("To Be Deleted")
                .userId(1)
                .build();

        when(dataSourceRepo.findById(dataSourceId)).thenReturn(Optional.of(mockEntity));

        NumberFormatException thrown = assertThrows(NumberFormatException.class, () -> dataSourceManager.deleteDataSource(invalidUserId, dataSourceId));

        assertThat(thrown.getMessage()).contains(invalidUserId);

        verify(dataSourceRepo, never()).delete(any(DataSource.class));
    }

    @Test
    void deleteDataSource_userIdMismatch_throwsForbiddenException() {
        int dataSourceId = 1;
        String requestedUserId = "10";
        long actualDataSourceOwnerId = 5L; // Different from requestedUserId

        DataSource mockEntity = DataSource.builder()
                .id((long) dataSourceId)
                .name("To Be Deleted")
                .userId((int)actualDataSourceOwnerId)
                .build();

        when(dataSourceRepo.findById(dataSourceId)).thenReturn(Optional.of(mockEntity));

        ForbiddenException thrown = assertThrows(ForbiddenException.class, () -> dataSourceManager.deleteDataSource(requestedUserId, dataSourceId));

        assertThat(thrown.getMessage()).isEqualTo("User can delete only own datasource!");

        verify(dataSourceRepo).findById(dataSourceId);
        verify(dataSourceRepo, never()).delete(any(DataSource.class));
    }

    @Test
    void deleteDataSource_notFound_throwsException() {
        int nonExistentId = 99;

        when(dataSourceRepo.findById(nonExistentId)).thenReturn(Optional.empty());

        NotFoundException thrown = assertThrows(NotFoundException.class, () -> dataSourceManager.deleteDataSource("1", nonExistentId));

        assertThat(thrown.getMessage()).isEqualTo("Unknown datasource id");

        verify(dataSourceRepo).findById(nonExistentId);
        verify(dataSourceRepo, never()).delete(any(DataSource.class));
    }



}
