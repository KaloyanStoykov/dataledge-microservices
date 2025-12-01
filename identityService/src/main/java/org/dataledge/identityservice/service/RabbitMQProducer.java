package org.dataledge.identityservice.service;

import lombok.extern.slf4j.Slf4j;
import org.dataledge.identityservice.dto.UserDeletedEvent;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RabbitMQProducer {

    private static final String EXCHANGE_NAME = "user.exchange";
    private static final String ROUTING_KEY = "user.deleted";

    private final AmqpTemplate rabbitTemplate;

    @Autowired
    public RabbitMQProducer(AmqpTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendUserDeletedEvent(Integer userId) {
        UserDeletedEvent event = new UserDeletedEvent(userId);

        rabbitTemplate.convertAndSend(EXCHANGE_NAME, ROUTING_KEY, event);

        log.info("User Deleted Event published for User ID: {}", userId);
    }
}
