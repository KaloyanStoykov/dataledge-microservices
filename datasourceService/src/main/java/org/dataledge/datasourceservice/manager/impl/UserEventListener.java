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
    @Autowired
    public UserEventListener(DataSourceRepo dataSourceRepo) {
        this.dataSourceRepo = dataSourceRepo;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    @Transactional
    public void handlerUserDeleted(UserDeletedEvent event){
        log.info("User " + event.getUserId() + " was deleted. Cleaning up datasources...");

        dataSourceRepo.deleteAllByUserId(event.getUserId());
    }
}
