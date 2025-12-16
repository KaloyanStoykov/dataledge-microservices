package org.dataledge.datasourceservice.manager.impl;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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


        int parsedUserId;
        try {
            parsedUserId = Integer.parseInt(userId);
        } catch (NumberFormatException e) {
            throw new NotFoundException("Invalid user ID: " + userId);
        }

        Page<DataSource> pageResult = dataSourceRepo.findAllByUserId(
                parsedUserId,
                PageRequest.of(pageNumber, pageSize)
        );

        if (pageResult.isEmpty()) {
            // Change the exception message slightly to reflect the filter context
            throw new NotFoundException("No data sources found for this user");
        }

        List<DataSourceResponse> items = pageResult.getContent()
                .stream()
                .map(mapper::toDataSourceResponse)
                .toList();

        return new GetDataSourcesResponse(items, pageResult.getTotalElements(), pageNumber, pageSize);
    }

    /**
     *
     * @param userId - header userId from auth
     * @param request - request to create a new datasource
     * @return create response with message
     */
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

    /**
     *
     * @param userId - the header userId used to identify who sent the request
     * @param id - id of the datasource to delete in the database
     * @return response with success message
     */
    @Override
    public DeleteDataSourceResponse deleteDataSource(String userId, int id) {
        DataSource dataSource = dataSourceRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Unknown datasource id"));

        log.info("Parsing user id for string {}", userId);
        int parsedUserId;
        // Parse userId from header
        try {
            parsedUserId = Integer.parseInt(userId);
        }
        catch (NumberFormatException e) {
            log.error("Parsing: Invalid userID: {}", userId);
            throw new NotFoundException("Invalid user ID: {}" + userId);
        }

        // Check delete is for own datasource
        if(parsedUserId != dataSource.getUserId()) {
            log.error("ID's for users dont match datasource\nRequested id: {}", parsedUserId);
            throw new ForbiddenException("User can delete only own datasource!");
        }

        dataSourceRepo.delete(dataSource);

        return new DeleteDataSourceResponse("Datasource deleted successfully!");
    }




}
