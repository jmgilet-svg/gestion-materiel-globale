package com.materiel.suite.client.net;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class BackendTransport {
    private final OkHttpClient client = new OkHttpClient();

    public boolean ping(String baseUrl) {
        try {
            Request req = new Request.Builder().url(baseUrl + "/actuator/health").build();
            try (Response res = client.newCall(req).execute()) {
                return res.isSuccessful();
            }
        } catch (IOException e) {
            return false;
        }
    }
}
