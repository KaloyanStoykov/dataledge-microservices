package org.dataledge.gateway;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.RestController;

@EnableConfigurationProperties({UriConfiguration.class})
@SpringBootApplication
@RestController
public class GatewayApplication {

    public static void main(String[] args) {
        // Load the .env file from the current working directory
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        System.setProperty("JWT_SECRET", dotenv.get("JWT_SECRET"));
        System.setProperty("JWT_EXPIRATION_MS", dotenv.get("JWT_EXPIRATION_MS"));

        // --- End of .env Loading ---

        SpringApplication.run(GatewayApplication.class, args);
    }

}
