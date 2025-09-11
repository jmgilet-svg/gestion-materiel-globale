package com.materiel.suite.client.net;

import com.materiel.suite.client.config.AppConfig;

public class ServiceFactory {
    private static AppConfig config;
    private static BackendTransport backendTransport;

    public static void init(AppConfig cfg) {
        config = cfg;
        if ("backend".equalsIgnoreCase(cfg.getMode())) {
            backendTransport = new BackendTransport();
        } else {
            backendTransport = null;
        }
    }

    public static AppConfig getConfig() { return config; }
    public static BackendTransport getBackendTransport() { return backendTransport; }
}
