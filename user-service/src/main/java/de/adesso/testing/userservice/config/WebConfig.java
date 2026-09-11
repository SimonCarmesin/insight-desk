package de.adesso.testing.userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS-Freigabe fuer das Frontend (frontend-service): laedt u.a. die
 * Benutzerliste fuer die Ticket-Zuweisung direkt per Browser-Fetch von hier.
 * Origin ueber Env-Variable FRONTEND_ORIGIN konfigurierbar, nicht hardcodiert.
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
