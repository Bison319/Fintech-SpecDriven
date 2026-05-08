package com.fintech.fraud.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fraudServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fraud Service API")
                        .description("Rule-based fraud detection and risk scoring")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Fintech Platform Team")));
    }
}
