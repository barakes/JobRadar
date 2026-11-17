package com.jobradar.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ADMIN_KEY_SCHEME = "adminKey";

    @Bean
    public OpenAPI jobRadarOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("JobRadar API")
                        .version("0.1.0")
                        .description("""
                                Aggregates job postings from public applicant-tracking systems \
                                (Greenhouse, Lever), keeps them in sync on a schedule, \
                                and offers full-text search with country and location filters.

                                Searching is public. Adding companies and triggering syncs \
                                requires an admin key: click "Authorize" and enter it."""))
                .components(new Components()
                        .addSecuritySchemes(ADMIN_KEY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(AdminApiKeyFilter.HEADER)))
                .addSecurityItem(new SecurityRequirement().addList(ADMIN_KEY_SCHEME));
    }
}