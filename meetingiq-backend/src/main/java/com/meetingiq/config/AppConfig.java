package com.meetingiq.config;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(MeetingIqProperties.class)
public class AppConfig {

    @Bean
    WebMvcConfigurer corsConfigurer() {
        List<String> allowedOrigins = List.of(
                "http://localhost:5173",
                "https://meetingiq-iota.vercel.app",
                "https://meetingiq-git-main-premaa13s-projects.vercel.app");

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(allowedOrigins.toArray(String[]::new))
                        .allowedMethods("*")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
