package com.example.transporterassignment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Transporter Lane Assignment API")
                        .version("1.0.0")
                        .description("REST backend for optimal transporter allocation on logistics trade lanes under cost minimization, capacity, and coverage constraints."));
    }
}
