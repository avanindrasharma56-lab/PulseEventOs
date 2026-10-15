package com.eventflow.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "PulseEvent OS — Enterprise Event Orchestration Platform API",
                version = "1.0.0",
                description = "Production REST API documentation for PulseEvent OS. Covers Authentication, Workflow State Machine (Draft -> Pending -> Approved -> Published), Pessimistic Write Inventory Locking, ZXing Base64 QR Passes, Door Check-In HUD, Analytics, and RFC 4180 CSV Attendee Export.",
                contact = @Contact(
                        name = "PulseEvent Core Architecture",
                        email = "dev@pulsevent.io",
                        url = "http://localhost:5173"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8085", description = "Local Development Backend"),
                @Server(url = "/", description = "Reverse Proxy / Production Gateway")
        },
        security = {
                @SecurityRequirement(name = "BearerAuth")
        }
)
@SecurityScheme(
        name = "BearerAuth",
        description = "Enter your JWT token obtained from POST /api/auth/login or POST /api/auth/register",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
