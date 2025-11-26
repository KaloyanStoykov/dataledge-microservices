package org.dataledge.datasourceservice.manager.impl;

import jakarta.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;
import org.dataledge.datasourceservice.data.DataType;
import org.dataledge.datasourceservice.data.DataTypeRepo;
import org.dataledge.datasourceservice.data.datasources.DataSource;
import org.dataledge.datasourceservice.data.datasources.DataSourceRepo;
import org.dataledge.datasourceservice.dto.datasourcesDTO.*;
import org.dataledge.datasourceservice.manager.IDataSourceMapper;
import org.dataledge.datasourceservice.manager.IDataSourceManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author Kaloyan Stoykov
 * Handles business logic for deletion and creation of datasources
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
    public GetDataSourcesResponse getDataSources(String userId, int pageNumber, int pageSize) {

        Page<DataSource> pageResult = dataSourceRepo.findAllByUserId(
                Integer.parseInt(userId),
                PageRequest.of(pageNumber, pageSize)
        );



        if (pageResult.isEmpty()) {
            // Change the exception message slightly to reflect the filter context
            throw new NotFoundException("No data sources found ");
        }

        List<DataSourceResponse> items = pageResult.getContent()
                .stream()
                .map(mapper::toDataSourceResponse)
                .toList();

        return new GetDataSourcesResponse(items, pageResult.getTotalElements(), pageNumber, pageSize);
    }


    @Override
    public CreateDataSourceResponse createDataSource(String userId, CreateDataSourceRequest request) {
        Optional<DataType> type = dataTypeRepo.findById(request.getTypeId());
        int uId = Integer.parseInt(userId);

        if(type.isPresent()) {
            DataSource dataSource = DataSource.builder()
                    .id(null)
                    .name(request.getName())
                    .type(type.get())
                    .description(request.getDescription())
                    .url(request.getUrl())
                    .created(Instant.now())
                    .updated(Date.from(Instant.now()))
                    .userId(uId)
                    .build();
            DataSource entity = dataSourceRepo.save(dataSource);
            return new CreateDataSourceResponse(entity.getId(), entity.getName());
        }

        throw new NotFoundException("Unknown datasource type");


    }

    @Override
    public DeleteDataSourceResponse deleteDataSource(int id) {
        DataSource dataSource = dataSourceRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Unknown datasource id"));

        dataSourceRepo.delete(dataSource);

        return new DeleteDataSourceResponse("Datasource deleted successfully!");
    }


}
