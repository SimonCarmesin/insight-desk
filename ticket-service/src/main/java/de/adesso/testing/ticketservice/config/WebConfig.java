package de.adesso.testing.ticketservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS-Freigabe fuer das Frontend (frontend-service), das die REST-API
 * dieses Service direkt aus dem Browser aufruft (anderer Origin/Port).
 * Origin ist ueber die Env-Variable FRONTEND_ORIGIN konfigurierbar, nicht
 * hardcodiert - Default passt zum lokalen frontend-service (Port 3000).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${frontend.origin:http://localhost:3000}")
    private String frontendOrigin;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(frontendOrigin)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
