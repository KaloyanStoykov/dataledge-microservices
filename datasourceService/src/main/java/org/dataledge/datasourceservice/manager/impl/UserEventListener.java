package org.dataledge.datasourceservice.manager.impl;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.dataledge.datasourceservice.config.rabbitmq.RabbitConfig;
import org.dataledge.datasourceservice.data.datasources.DataSourceRepo;
import org.dataledge.datasourceservice.data.filesnaps.BlobMetadataRepo;
import org.dataledge.datasourceservice.dto.rabbitmq.UserDeletedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserEventListener {
    private final UserCleanupService userCleanupService;
    private final AzureBlobRequestManager azureBlobRequestManager;

    public UserEventListener(UserCleanupService userCleanupService,  AzureBlobRequestManager azureBlobRequestManager, BlobMetadataRepo repo) {
        this.userCleanupService = userCleanupService;
        this.azureBlobRequestManager = azureBlobRequestManager;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void handlerUserDeleted(UserDeletedEvent event) {
        try {
            log.info("User {} was deleted. Cleaning up...", event.getUserId());
            String strUserId = String.valueOf(event.getUserId());

            userCleanupService.executeDbCleanup(event.getUserId());

            try {
                var allUserBlobs = azureBlobRequestManager.getFiles(strUserId);
                azureBlobRequestManager.deleteUserBlobs(strUserId, allUserBlobs);
            } catch (Exception blobEx) {
                log.error("DB cleanup succeeded, but Blob cleanup failed for user {}", event.getUserId(), blobEx);
            }

        } catch (Exception e) {
            log.error("CRITICAL ERROR: Failed to clean up user {}.", event.getUserId(), e);
        }
    }


}
