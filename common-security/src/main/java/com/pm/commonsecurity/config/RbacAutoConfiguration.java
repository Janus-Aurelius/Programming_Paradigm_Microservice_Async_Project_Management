package com.pm.commonsecurity.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Auto-configuration for RBAC security components.
 * This configuration loads permissions.yml and enables permission evaluation.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RbacConfigurationProperties.class)
@ComponentScan(basePackages = "com.pm.commonsecurity.security")
@PropertySource(value = "classpath:permissions.yml", factory = YamlPropertySourceFactory.class)
public class RbacAutoConfiguration {
    
    public RbacAutoConfiguration() {
        log.info("Initializing RBAC Auto Configuration");
    }
}
