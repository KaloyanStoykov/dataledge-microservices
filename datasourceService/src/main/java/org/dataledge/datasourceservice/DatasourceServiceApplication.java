package org.dataledge.datasourceservice;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DatasourceServiceApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();

        // Server Config
        System.setProperty("SERVER_PORT", dotenv.get("SERVER_PORT"));

        // Eureka Config
        System.setProperty("EUREKA_CLIENT", dotenv.get("EUREKA_CLIENT"));
        System.setProperty("EUREKA_HOSTNAME", dotenv.get("EUREKA_HOSTNAME"));
        System.setProperty("EUREKA_IPADDRESS", dotenv.get("EUREKA_IPADDRESS"));

        // Database Config
        System.setProperty("DATASOURCE_URL", dotenv.get("DATASOURCE_URL"));
        System.setProperty("DATASOURCE_USERNAME", dotenv.get("DATASOURCE_USERNAME"));
        System.setProperty("DATASOURCE_PASSWORD", dotenv.get("DATASOURCE_PASSWORD"));

        // Docker
        System.setProperty("MYSQL_DB", dotenv.get("MYSQL_DB"));
        System.setProperty("MYSQL_PASS", dotenv.get("MYSQL_PASS"));
        System.setProperty("MYSQL_ROOT_PASS", dotenv.get("MYSQL_ROOT_PASS"));
        System.setProperty("MYSQL_USER", dotenv.get("MYSQL_USER"));

        System.setProperty("RABBITMQ_USER", dotenv.get("RABBITMQ_USER"));
        System.setProperty("RABBITMQ_PASS", dotenv.get("RABBITMQ_PASS"));
        System.setProperty("RABBITMQ_HOST", dotenv.get("RABBITMQ_HOST"));
        System.setProperty("RABBITMQ_PORT", dotenv.get("RABBITMQ_PORT"));

        System.setProperty("AZURE_STORAGE_ACCOUNT_NAME", dotenv.get("AZURE_STORAGE_ACCOUNT_NAME"));
        System.setProperty("AZURE_STORAGE_ACCOUNT_ENDPOINT", dotenv.get("AZURE_STORAGE_ACCOUNT_ENDPOINT"));
        System.setProperty("AZURE_FILE_CONTAINER_NAME", dotenv.get("AZURE_FILE_CONTAINER_NAME"));
        System.setProperty("AZURE_CONNECTION_STRING", dotenv.get("AZURE_CONNECTION_STRING"));




        SpringApplication.run(DatasourceServiceApplication.class, args);
    }

}
