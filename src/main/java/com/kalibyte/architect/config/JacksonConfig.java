package com.kalibyte.architect.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper auditObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Registers support for Java 8 dates/times like LocalDateTime used in your AuditLog
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}