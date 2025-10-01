package org.dataledge.datasourceservice;

import org.dataledge.datasourceservice.data.DataSource;
import org.dataledge.datasourceservice.data.DataSourceRepo;
import org.dataledge.datasourceservice.dto.DataSourceResponse;
import org.dataledge.datasourceservice.dto.GetDataSourcesResponse;
import org.dataledge.datasourceservice.manager.IDataSourceMapper;
import org.dataledge.datasourceservice.manager.impl.DataSourceManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DataServiceTest {
    @Mock
    private DataSourceRepo dataSourceRepo;

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

        // Mock entities and DTOs
        DataSource entity1 = new DataSource(1L, "Customer DB", "PostgresSQL", "Customer DB for Censly.", "jdbc:postgresql://localhost:5432/customers", Instant.now(), Date.from(Instant.parse("2019-04-20T00:00:00Z")));
        DataSource entity2 = new DataSource(2L, "Customer DB2", "PostgresSQL", "Customer DB for Censly.", "jdbc:postgresql://localhost:5432/customers", Instant.now(), Date.from(Instant.parse("2019-04-20T00:00:00Z")));
        DataSourceResponse dto1 = new DataSourceResponse(1L, "Customer DB", "PostgresSQL", "Customer DB for Censly.", "jdbc:postgresql://localhost:5432/customers", Instant.now(), Date.from(Instant.parse("2019-04-20T00:00:00Z")));
        DataSourceResponse dto2 = new DataSourceResponse(2L, "Customer DB2", "PostgresSQL", "Customer DB for Censly.", "jdbc:postgresql://localhost:5432/customers", Instant.now(), Date.from(Instant.parse("2019-04-20T00:00:00Z")));

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
        assertThat(response.getTotalCount()).isEqualTo(totalElements);
        assertThat(response.getPage()).isEqualTo(pageNumber);
        assertThat(response.getPageSize()).isEqualTo(pageSize);

        // Verify
        verify(dataSourceRepo).findAll(PageRequest.of(pageNumber, pageSize));
        verify(mapper).toDataSourceResponse(entity1);
        verify(mapper).toDataSourceResponse(entity2);
    }



}
