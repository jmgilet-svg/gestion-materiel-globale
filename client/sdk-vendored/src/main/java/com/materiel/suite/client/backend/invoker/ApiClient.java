package com.materiel.suite.client.backend.invoker;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Simple HTTP client used by vendored SDK. */
public class ApiClient {
    private String basePath = "http://localhost:8080";

    public ApiClient basePath(String basePath) {
        this.basePath = basePath;
        return this;
    }

    public String get(String path) throws Exception {
        URL url = new URL(basePath + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        try (InputStream in = conn.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
