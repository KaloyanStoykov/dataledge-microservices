package org.dataledge.datasourceservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DatasourceServiceApplication {

    public static void main(String[] args) {
        // No manual System.setProperty needed!
        // Spring Boot automatically maps environment variables to properties.
        SpringApplication.run(DatasourceServiceApplication.class, args);
    }
}
