package org.dataledge.datasourceservice.manager.impl;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dataledge.datasourceservice.config.exceptions.BlobStorageOperationException;
import org.dataledge.datasourceservice.data.DataType;
import org.dataledge.datasourceservice.data.DataTypeRepo;
import org.dataledge.datasourceservice.data.datasources.DataSource;
import org.dataledge.datasourceservice.data.datasources.DataSourceRepo;
import org.dataledge.datasourceservice.data.datasources.DataSourceSpecs;
import org.dataledge.datasourceservice.data.filesnaps.BlobMetadataRepo;
import org.dataledge.datasourceservice.dto.datasourcesDTO.*;
import org.dataledge.datasourceservice.manager.IAzureBlobStorage;
import org.dataledge.datasourceservice.manager.IBlobMetadataManager;
import org.dataledge.datasourceservice.manager.IDataSourceMapper;
import org.dataledge.datasourceservice.manager.IDataSourceManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
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
    private final BlobMetadataRepo blobMetadataManager;
    private final DataTypeRepo dataTypeRepo;
    private final IAzureBlobStorage azureBlobStorage;
    // Mapper to responses
    private final IDataSourceMapper mapper;

    /**
     * @param pageNumber  contains the pageNumber and pageSize properties for repository. pageNumber is zero-based
     * @param pageSize refers to count of elements to get.
     * @return the response object containing List<> of data sources
     * @throws NotFoundException when no items were found from the repository
     */
    @Override
    public GetDataSourcesResponse getDataSources(String userId, int pageNumber, int pageSize, String searchTerm) {
        int parsedUserId = Integer.parseInt(userId);

        // Build the Specification
        Specification<DataSource> spec = DataSourceSpecs.search(parsedUserId, searchTerm);

        // Fetch the page
        Page<DataSource> pageResult = dataSourceRepo.findAll(
                spec,
                PageRequest.of(pageNumber, pageSize, Sort.by("created").descending())
        );

        // Optional: Only throw 404 if the user has ZERO records total (no search applied)
        if (pageResult.isEmpty() && (searchTerm == null || searchTerm.isBlank())) {
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
    @Transactional
    public DeleteDataSourceResponse deleteDataSource(String userId, int id) {
        // 1. Find and Verify Ownership
        DataSource dataSource = dataSourceRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Unknown datasource id"));

        int parsedUserId = Integer.parseInt(userId);
        if (parsedUserId != dataSource.getUserId()) {
            throw new ForbiddenException("User can delete only own datasource!");
        }

        // 2. Identify all associated files
        List<String> fileNames = blobMetadataManager.findAllFileNamesByUserAndDataSource(parsedUserId, id);

        if (!fileNames.isEmpty()) {
            log.info("Deleting {} associated files for datasource {}", fileNames.size(), id);

            // 3. Delete from BlobMetadata Table
            blobMetadataManager.deleteByUserIdAndBlobNames(parsedUserId, fileNames);

            // 4. Delete from Azure Storage
            try {
                azureBlobStorage.deleteFilesBatch(String.valueOf(parsedUserId), fileNames);
            } catch (Exception e) {
                log.error("Failed to delete files from Azure for DS {}: {}", id, e.getMessage());
                // Depending on your policy, you might want to throw an exception here to rollback everything
                throw new BlobStorageOperationException("Could not clean up cloud storage, aborting deletion.");
            }
        }

        // 5. Final Step: Delete the Datasource record
        dataSourceRepo.delete(dataSource);

        return new DeleteDataSourceResponse("Datasource and all associated cloud files deleted successfully!");
    }

    @Override
    public UpdateDataSourceResponse updateDataSource(String userId, int id, UpdateDataSourceRequest updateRequest) {
        // 1. Find the existing DataSource or throw an exception
        DataSource existingDataSource = dataSourceRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("DataSource not found with id: " + id));

        int parsedUserId;
        // Parse userId from header
        try {
            parsedUserId = Integer.parseInt(userId);
        }
        catch (NumberFormatException e) {
            log.error("Parsing: Invalid userID: {}", userId);
            throw new NotFoundException("Invalid user ID: {}" + userId);
        }


        // 2. Optional: Verify ownership if userId is required for security
        if(existingDataSource.getUserId() != parsedUserId) {
            throw new ForbiddenException("Users can update only own datasource!");
        }

        // 3. Update basic fields from the request
        existingDataSource.setName(updateRequest.getName());
        existingDataSource.setDescription(updateRequest.getDescription());
        // Add other fields as necessary (e.g., connection details, status)

        // 4. Handle Relationship Update (DataType)
        // Assuming the request contains a typeId to link to a different DataType
        if (updateRequest.getTypeId() != null) {
            DataType newType = dataTypeRepo.findById(updateRequest.getTypeId())
                    .orElseThrow(() -> new EntityNotFoundException("DataType not found"));
            existingDataSource.setType(newType);
        }

        // 5. Persist the changes
        DataSource updatedDataSource = dataSourceRepo.save(existingDataSource);

        // 6. Map to response DTO and return
        return new UpdateDataSourceResponse(
                updatedDataSource.getId().intValue(),
                updatedDataSource.getName(),
                updatedDataSource.getDescription(),
                updatedDataSource.getUrl(),
                LocalDateTime.now()
        );
    }




}
