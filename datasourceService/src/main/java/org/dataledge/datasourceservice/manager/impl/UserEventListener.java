package org.dataledge.datasourceservice.manager.impl;

import jakarta.transaction.Transactional;
import org.dataledge.datasourceservice.config.rabbitmq.RabbitConfig;
import org.dataledge.datasourceservice.data.datasources.DataSourceRepo;
import org.dataledge.datasourceservice.dto.rabbitmq.UserDeletedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserEventListener {
    private DataSourceRepo dataSourceRepo;
    @Autowired
    public UserEventListener(DataSourceRepo dataSourceRepo) {
        this.dataSourceRepo = dataSourceRepo;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    @Transactional
    public void handlerUserDeleted(UserDeletedEvent event){
        System.out.println("User " + event.getUserId() + " was deleted. Cleaning up datasources...");

        dataSourceRepo.deleteAllByUserId(event.getUserId());
    }
}
