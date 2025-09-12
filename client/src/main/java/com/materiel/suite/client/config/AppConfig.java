package com.materiel.suite.client.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

public class AppConfig {
    private String mode = "mock";
    private String baseUrl = "http://localhost:8080";

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public static Path configPath() {
        return Path.of(System.getProperty("user.home"), ".gestion-materiel", "app.properties");
    }

    public static AppConfig load() {
        AppConfig cfg = new AppConfig();
        File file = configPath().toFile();
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                Properties p = new Properties();
                p.load(in);
                cfg.mode = p.getProperty("app.mode", cfg.mode);
                cfg.baseUrl = p.getProperty("api.baseUrl", cfg.baseUrl);
            } catch (IOException ignored) {}
        } else {
            file.getParentFile().mkdirs();
            try (FileOutputStream out = new FileOutputStream(file)) {
                Properties p = new Properties();
                p.setProperty("app.mode", cfg.mode);
                p.setProperty("api.baseUrl", cfg.baseUrl);
                p.store(out, "Gestion Materiel");
            } catch (IOException ignored) {}
        }
        return cfg;
    }
}
