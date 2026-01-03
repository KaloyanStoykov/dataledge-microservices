package org.dataledge.datasourceservice.manager.impl;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.dataledge.datasourceservice.config.exceptions.InvalidUserException;
import org.dataledge.datasourceservice.data.DataType;
import org.dataledge.datasourceservice.data.datasources.DataSource;
import org.dataledge.datasourceservice.data.datasources.DataSourceRepo;
import org.dataledge.datasourceservice.data.filesnaps.BlobMetadata;
import org.dataledge.datasourceservice.data.filesnaps.BlobMetadataRepo;
import org.dataledge.datasourceservice.dto.blobMetadataDTO.BlobMetadataResponse;
import org.dataledge.datasourceservice.dto.blobMetadataDTO.CreateBlobMetadataRequest;
import org.dataledge.datasourceservice.dto.blobMetadataDTO.GetPagedBlobMetadataResponse;
import org.dataledge.datasourceservice.manager.IBlobMetadataManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
public class BlobMetadataManager implements IBlobMetadataManager {
    private final BlobMetadataRepo blobMetadataRepo;
    private final DataSourceRepo dataSourceRepo;


    public BlobMetadataManager(BlobMetadataRepo blobMetadataRepo, DataSourceRepo dataSourceRepo){
        this.blobMetadataRepo = blobMetadataRepo;
        this.dataSourceRepo = dataSourceRepo;
    }

    public String sanitizeUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.error("Invalid user id provided.");
            throw new InvalidUserException("User ID cannot be empty.");
        }

        // Updated safe pattern: only digits (0-9)
        if (!Pattern.matches("^[0-9]+$", userId.trim())) {
            log.error("User ID failed numeric regex pattern.");
            throw new InvalidUserException("Invalid User ID format. Must be an integer.");
        }

        // Trimming before using the result is still a good practice
        return userId.trim();
    }

    @Override
    public GetPagedBlobMetadataResponse getBlobsForDatasources(String userId, int datasourceId, int pageNumber, int pageSize) {
        int parsedUserId = Integer.parseInt(sanitizeUserId(userId));
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        // Calling the custom query method
        Page<BlobMetadata> pageResult = blobMetadataRepo.findByUserAndDataSource(
                parsedUserId,
                datasourceId,
                pageable
        );

        List<BlobMetadataResponse> items = pageResult.getContent()
                .stream()
                .map(x -> new BlobMetadataResponse(x.getId(), x.getFileName(), x.getCreated()))
                .toList();

        return new GetPagedBlobMetadataResponse(
                items,
                pageResult.getTotalElements(),
                pageResult.getNumber(),
                pageResult.getSize()
        );
    }

    @Override
    public String createBlobMetadata(int userId, String fileName, DataSource datasource) {
        BlobMetadata metadata = new BlobMetadata(
                null,
                fileName,
                Instant.now(),
                userId,
                datasource
        );

        blobMetadataRepo.save(metadata);
        return "Blob reference set successfully";
    }

    @Override
    @Transactional
    public void deleteMetadataBatch(int userId, List<String> blobNames) {
        if (blobNames == null || blobNames.isEmpty()) {
            return;
        }

        try {
            blobMetadataRepo.deleteByUserIdAndBlobNames(userId, blobNames);
            log.info("Successfully deleted {} metadata records for user: {}", blobNames.size(), userId);
        } catch (Exception e) {
            log.error("Failed to delete metadata batch for user: {}", userId, e);
            throw new RuntimeException("Could not sync metadata deletion", e);
        }
    }

}
