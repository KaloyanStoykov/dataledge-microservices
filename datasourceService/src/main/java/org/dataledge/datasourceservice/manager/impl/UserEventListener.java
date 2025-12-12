package org.dataledge.datasourceservice.manager.impl;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.dataledge.datasourceservice.config.rabbitmq.RabbitConfig;
import org.dataledge.datasourceservice.data.datasources.DataSourceRepo;
import org.dataledge.datasourceservice.dto.rabbitmq.UserDeletedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserEventListener {
    private final DataSourceRepo dataSourceRepo;
    private final AzureBlobRequestManager azureBlobRequestManager;

    @Autowired
    public UserEventListener(DataSourceRepo dataSourceRepo,  AzureBlobRequestManager azureBlobRequestManager) {
        this.dataSourceRepo = dataSourceRepo;
        this.azureBlobRequestManager = azureBlobRequestManager;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void handlerUserDeleted(UserDeletedEvent event) {
        try {
            log.info("User {} was deleted. Cleaning up datasources...", event.getUserId());
            String strUserId = String.valueOf(event.getUserId());

            // Note: It is often safer to keep the DB transaction separate from the Blob storage call
            // to prevent a Blob error from rolling back the DB delete.
            executeCleanup(event.getUserId(), strUserId);

        } catch (Exception e) {
            // Log the error so you know what to fix
            log.error("CRITICAL ERROR: Failed to clean up user {}. Stopping retry loop.", event.getUserId(), e);

            // OPTIONAL: If you have a Dead Letter Queue (DLQ) configured,
            // you can throw a specific exception here to send it there.
            // throw new AmqpRejectAndDontRequeueException(e);
        }
    }

    @Transactional
    public void executeCleanup(int userId, String strUserId) {
        // 1. Delete from DB
        dataSourceRepo.deleteAllByUserId(userId);

        // 2. Get Files
        var allUserBlobs = azureBlobRequestManager.getFiles(strUserId);

        // 3. Delete Blobs
        // If this fails, the DB delete above will roll back!
        azureBlobRequestManager.deleteUserBlobs(strUserId, allUserBlobs);
    }
}
