package com.jobradar.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI jobRadarOpenApi() {
        return new OpenAPI().info(new Info()
                .title("JobRadar API")
                .version("0.1.0")
                .description("""
                        Aggregates job postings from public applicant-tracking systems \
                        (Greenhouse, Lever), keeps them in sync on a schedule, \
                        and offers full-text search with country and location filters."""));
    }
}