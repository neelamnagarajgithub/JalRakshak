package org.jalrakshak.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows any origin to call this API's endpoints from a browser. Wide open
 * on purpose for this prototype/demo (no auth, no cookies/credentials sent),
 * so the dashboard works regardless of what host/port it's served from.
 * {@code allowedOriginPatterns("*")} is used (rather than
 * {@code allowedOrigins("*")}) because it is compatible with
 * {@code allowCredentials} if that's ever turned on later; today no
 * credentials are used, so a literal {@code allowedOrigins("*")} would also
 * work. If this API is ever exposed beyond local development, tighten this
 * back down to a specific origin allow-list.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
