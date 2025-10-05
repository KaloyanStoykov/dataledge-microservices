package org.dataledge.datasourceservice.manager.impl;

import jakarta.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;
import org.dataledge.datasourceservice.data.DataType;
import org.dataledge.datasourceservice.data.DataTypeRepo;
import org.dataledge.datasourceservice.data.datasources.DataSource;
import org.dataledge.datasourceservice.data.datasources.DataSourceRepo;
import org.dataledge.datasourceservice.dto.datasourcesDTO.CreateDataSourceRequest;
import org.dataledge.datasourceservice.dto.datasourcesDTO.CreateDataSourceResponse;
import org.dataledge.datasourceservice.dto.datasourcesDTO.DataSourceResponse;
import org.dataledge.datasourceservice.dto.datasourcesDTO.GetDataSourcesResponse;
import org.dataledge.datasourceservice.manager.IDataSourceMapper;
import org.dataledge.datasourceservice.manager.IDataSourceManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @author Kaloyan Stoykov
 */
@Service
@AllArgsConstructor
public class DataSourceManager implements IDataSourceManager {

    // JPA Pageable repository
    private final DataSourceRepo dataSourceRepo;
    private final DataTypeRepo dataTypeRepo;
    // Mapper to responses
    private final IDataSourceMapper mapper;

    /**
     * @param pageNumber  contains the pageNumber and pageSize properties for repository. pageNumber is zero-based
     * @param pageSize refers to count of elements to get.
     * @return the response object containing List<> of data sources
     * @throws NotFoundException when no items were found from the repository
     */
    @Override
    public GetDataSourcesResponse getDataSources(int pageNumber, int pageSize) {

        Page<DataSource> pageResult = dataSourceRepo.findAll(PageRequest.of(pageNumber, pageSize));

        if (pageResult.isEmpty()) {
            throw new NotFoundException("No data sources found");
        }

        List<DataSourceResponse> items = pageResult.getContent()
                .stream()
                .map(mapper::toDataSourceResponse)
                .toList();

        return new GetDataSourcesResponse(items, pageResult.getTotalElements(), pageNumber, pageSize);

    }

    @Override
    public CreateDataSourceResponse createDataSource(CreateDataSourceRequest request) {
        Optional<DataType> type = dataTypeRepo.findById(request.getTypeId());

        if(type.isPresent()) {
            DataSource dataSource = new DataSource(null, request.getName(), type.get(), request.getDescription(), request.getUrl(), Instant.now(), null);
            DataSource entity = dataSourceRepo.save(dataSource);
            return new CreateDataSourceResponse(entity.getId(), entity.getName());
        }

        throw new NotFoundException("Unknown datasource type");


    }



}
