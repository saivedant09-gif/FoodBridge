package com.foodbridge.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    @Value("${foodbridge.mongodb.enabled:false}")
    private boolean mongoEnabled;

    @PostConstruct
    public void init() {
        if (mongoEnabled) {
            log.info("MongoDB integration is ENABLED.");
        } else {
            log.info("MongoDB integration is DISABLED (graceful fallback active).");
        }
    }

    @Bean
    public boolean mongoEnabled() {
        return mongoEnabled;
    }
}
