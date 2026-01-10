package org.dataledge.identityservice;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IdentityServiceApplication {

    public static void main(String[] args) {
        // --- Custom .env Loading ---

        // Load the .env file from the current working directory
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        // Set the properties as System Properties so Spring can read them
        System.setProperty("DB_URL", dotenv.get("DB_URL"));
        System.setProperty("DB_USERNAME", dotenv.get("DB_USERNAME"));
        System.setProperty("DB_PASSWORD", dotenv.get("DB_PASSWORD"));
        System.setProperty("JWT_SECRET", dotenv.get("JWT_SECRET"));
        System.setProperty("JWT_EXPIRATION_MS", dotenv.get("JWT_EXPIRATION_MS"));

        System.setProperty("RABBITMQ_USER", dotenv.get("RABBITMQ_USER"));
        System.setProperty("RABBITMQ_PASS", dotenv.get("RABBITMQ_PASS"));
        System.setProperty("RABBITMQ_HOST", dotenv.get("RABBITMQ_HOST"));
        System.setProperty("RABBITMQ_PORT", dotenv.get("RABBITMQ_PORT"));

        // --- End of .env Loading ---

        SpringApplication.run(IdentityServiceApplication.class, args);
    }

}
