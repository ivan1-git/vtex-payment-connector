package dev.ivanjimenez.connector.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    // Inyectar el reloj hace que las pruebas de pagos asíncronos no dependan de Thread.sleep
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RestClient callbackRestClient(RestClient.Builder builder) {
        return builder.build();
    }
}
