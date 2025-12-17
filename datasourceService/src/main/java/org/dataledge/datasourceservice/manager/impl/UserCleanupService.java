package org.dataledge.datasourceservice.manager.impl;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.dataledge.datasourceservice.data.datasources.DataSourceRepo;
import org.dataledge.datasourceservice.data.filesnaps.BlobMetadataRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserCleanupService {

    private DataSourceRepo dataSourceRepo;

    private BlobMetadataRepo blobMetadataRepo;

    @Transactional
    public void executeDbCleanup(int userId) {
        blobMetadataRepo.deleteAllByUserId(userId);
        dataSourceRepo.deleteAllByUserId(userId);
    }
}