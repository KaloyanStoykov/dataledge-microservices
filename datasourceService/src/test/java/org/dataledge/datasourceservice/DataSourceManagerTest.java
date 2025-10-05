package org.dataledge.datasourceservice;

import jakarta.ws.rs.NotFoundException;
import org.dataledge.datasourceservice.data.DataType;
import org.dataledge.datasourceservice.data.DataTypeRepo;
import org.dataledge.datasourceservice.data.datasources.DataSource;
import org.dataledge.datasourceservice.data.datasources.DataSourceRepo;
import org.dataledge.datasourceservice.dto.datasourcesDTO.CreateDataSourceRequest;
import org.dataledge.datasourceservice.dto.datasourcesDTO.CreateDataSourceResponse;
import org.dataledge.datasourceservice.dto.datasourcesDTO.DataSourceResponse;
import org.dataledge.datasourceservice.dto.datasourcesDTO.GetDataSourcesResponse;
import org.dataledge.datasourceservice.manager.IDataSourceMapper;
import org.dataledge.datasourceservice.manager.impl.DataSourceManager;
import org.dataledge.datasourceservice.manager.impl.DataSourceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DataSourceManagerTest {
    @Mock
    private DataSourceRepo dataSourceRepo;

    @Mock
    private DataTypeRepo dataTypeRepo;

    @Mock
    private IDataSourceMapper mapper;

    @InjectMocks
    private DataSourceManager dataSourceManager;

    @Test
    void getDataSources_success_returnsData() {
        // Arrange
        int pageNumber = 0;
        int pageSize = 10;
        long totalElements = 25;

        DataType dataType = new DataType(1L, "API", "API to work with datasources", null);


        // Mock entities and DTOs
        DataSource entity1 = new DataSource(1L, "Customer DB", dataType, "Customer DB for Censly.", "jdbc:postgresql://localhost:5432/customers", Instant.now(), Date.from(Instant.parse("2019-04-20T00:00:00Z")));
        DataSource entity2 = new DataSource(2L, "Customer DB2", dataType, "Customer DB for Censly.", "jdbc:postgresql://localhost:5432/customers", Instant.now(), Date.from(Instant.parse("2019-04-20T00:00:00Z")));
        DataSourceResponse dto1 = new DataSourceResponse(1L, "Customer DB", dataType, "Customer DB for Censly.", "jdbc:postgresql://localhost:5432/customers", Instant.now(), Date.from(Instant.parse("2019-04-20T00:00:00Z")));
        DataSourceResponse dto2 = new DataSourceResponse(2L, "Customer DB2", dataType, "Customer DB for Censly.", "jdbc:postgresql://localhost:5432/customers", Instant.now(), Date.from(Instant.parse("2019-04-20T00:00:00Z")));

        List<DataSource> entities = List.of(entity1, entity2);
        Page<DataSource> page = new PageImpl<>(entities, PageRequest.of(pageNumber, pageSize), totalElements);

        // Mock repository call
        when(dataSourceRepo.findAll(PageRequest.of(pageNumber, pageSize)))
                .thenReturn(page);

        // Mock mapping
        when(mapper.toDataSourceResponse(entity1)).thenReturn(dto1);
        when(mapper.toDataSourceResponse(entity2)).thenReturn(dto2);

        // Act
        GetDataSourcesResponse response = dataSourceManager.getDataSources(pageNumber, pageSize);

        // Assert
        assertThat(response.getItems()).hasSize(2);
        for(var  entity : response.getItems()) {
            assertThat(entity.getType()).isEqualTo(dataType);
        }
        assertThat(response.getTotalCount()).isEqualTo(totalElements);
        assertThat(response.getPage()).isEqualTo(pageNumber);
        assertThat(response.getPageSize()).isEqualTo(pageSize);

        // Verify
        verify(dataSourceRepo).findAll(PageRequest.of(pageNumber, pageSize));
        verify(mapper).toDataSourceResponse(entity1);
        verify(mapper).toDataSourceResponse(entity2);
    }

    @Test
    void getDataSources_throwException_returnsEmptyList() {
        int pageNumber = 0;
        int pageSize = 10;
        long totalElements = 25;

        Page<DataSource> page = new PageImpl<>(List.of(), PageRequest.of(pageNumber, pageSize), totalElements);

        when(dataSourceRepo.findAll(PageRequest.of(pageNumber, pageSize))).thenReturn(page);

        assertThrows(NotFoundException.class, () -> dataSourceManager.getDataSources(pageNumber, pageSize));

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

        DataSource savedEntity = new DataSource(
                1L,
                request.getName(),
                dataType,
                request.getDescription(),
                request.getUrl(),
                Instant.now(),
                null
        );


        when(dataSourceRepo.save(any(DataSource.class))).thenReturn(savedEntity);
        when(dataTypeRepo.findById(1L)).thenReturn(Optional.of(dataType));

        CreateDataSourceResponse response = dataSourceManager.createDataSource(request);

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



}
