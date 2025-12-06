package org.dataledge.datasourceservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration // Marks this class as a source of bean definitions
public class AppConfig {

    /**
     * Defines a RestTemplate bean.
     * Used for API requests and file uploads to Azure.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
