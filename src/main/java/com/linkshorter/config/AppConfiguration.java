package com.linkshorter.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Application configuration loader
 * Reads configuration from application.properties file
 */
public class AppConfiguration {
    private static final String CONFIG_FILE = "application.properties";
    private final Properties properties;

    public AppConfiguration() {
        this.properties = new Properties();
        loadConfiguration();
    }

    private void loadConfiguration() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new RuntimeException("Unable to find " + CONFIG_FILE);
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Error loading configuration", e);
        }
    }

    public long getDefaultTtl() {
        return Long.parseLong(properties.getProperty("link.ttl.default", "86400000"));
    }

    public int getDefaultClickLimit() {
        return Integer.parseInt(properties.getProperty("link.click.limit.default", "100"));
    }

    public String getLinkDomain() {
        return properties.getProperty("link.domain", "clck.ru");
    }

    public int getShortCodeLength() {
        return Integer.parseInt(properties.getProperty("link.code.length", "6"));
    }

    public long getCleanupInterval() {
        return Long.parseLong(properties.getProperty("cleanup.interval", "3600000"));
    }

    public boolean isNotificationsEnabled() {
        return Boolean.parseBoolean(properties.getProperty("notifications.enabled", "true"));
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}

