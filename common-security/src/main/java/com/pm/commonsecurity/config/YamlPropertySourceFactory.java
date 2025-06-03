package com.pm.commonsecurity.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.lang.Nullable;

import java.util.Objects;
import java.util.Properties;

/**
 * Custom PropertySourceFactory to load YAML files as property sources.
 * This enables loading permissions.yml using @PropertySource annotation.
 */
public class YamlPropertySourceFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(@Nullable String name, EncodedResource resource) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource.getResource());
        
        Properties properties = factory.getObject();
        
        String sourceName = name != null ? name : resource.getResource().getFilename();
        
        return new PropertiesPropertySource(Objects.requireNonNull(sourceName), 
                                          Objects.requireNonNull(properties));
    }
}
