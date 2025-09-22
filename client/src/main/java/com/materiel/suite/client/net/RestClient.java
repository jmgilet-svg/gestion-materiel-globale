package com.materiel.suite.client.net;

import com.materiel.suite.client.agency.AgencyHttp;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class RestClient {
	private final String baseUrl;
	private final Optional<String> bearer;
	private final HttpClient http;

	public RestClient(String baseUrl, String bearerToken){
		this.baseUrl = baseUrl.endsWith("/")? baseUrl.substring(0, baseUrl.length()-1) : baseUrl;
		this.bearer = (bearerToken==null || bearerToken.isBlank())? Optional.empty() : Optional.of(bearerToken);
		this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
	}

	public String get(String path) throws IOException, InterruptedException {
		return request("GET", path, null, null);
	}

	public String post(String path, String json) throws IOException, InterruptedException {
		return request("POST", path, json, null);
	}

	public String put(String path, String json) throws IOException, InterruptedException {
		return request("PUT", path, json, null);
	}

	public String delete(String path) throws IOException, InterruptedException {
		return request("DELETE", path, null, null);
	}

	public String get(String path, Map<String,String> headers) throws IOException, InterruptedException {
		return request("GET", path, null, headers);
	}

	public String post(String path, String json, Map<String,String> headers) throws IOException, InterruptedException {
		return request("POST", path, json, headers);
	}

	public String put(String path, String json, Map<String,String> headers) throws IOException, InterruptedException {
		return request("PUT", path, json, headers);
	}

        public String delete(String path, Map<String,String> headers) throws IOException, InterruptedException {
                return request("DELETE", path, null, headers);
        }

        public byte[] postForBytes(String path, String json, String accept) throws IOException, InterruptedException {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                                .uri(URI.create(baseUrl + path));
                if (accept == null || accept.isBlank()){
                        builder.header("Accept", "*/*");
                } else {
                        builder.header("Accept", accept);
                }
                builder.header("Content-Type", "application/json");
                AgencyHttp.apply(builder);
                bearer.ifPresent(t -> builder.header("Authorization", "Bearer " + t));
                builder.POST(HttpRequest.BodyPublishers.ofString(json == null ? "{}" : json));
                HttpResponse<byte[]> res = http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
                return ensure200Bytes(res);
        }

        private static String ensure200(HttpResponse<String> res) throws IOException {
                int s = res.statusCode();
                if (s>=200 && s<300) return res.body();
                throw new IOException("HTTP "+s+": "+res.body());
        }

        private static byte[] ensure200Bytes(HttpResponse<byte[]> res) throws IOException {
                int s = res.statusCode();
                if (s>=200 && s<300) return res.body();
                String body = new String(res.body(), StandardCharsets.UTF_8);
                throw new IOException("HTTP "+s+": "+body);
        }

	private String request(String method, String path, String json, Map<String, String> headers)
			throws IOException, InterruptedException {

                final HttpRequest.Builder b = HttpRequest.newBuilder()
                                .uri(URI.create(baseUrl + path))
                                .header("Accept", "application/json");

                AgencyHttp.apply(b);

		switch (method) {
		case "GET"     -> b.GET();
		case "DELETE"  -> b.DELETE();
		case "POST"    -> b.header("Content-Type","application/json")
		.POST(HttpRequest.BodyPublishers.ofString(json == null ? "{}" : json));
		case "PUT"     -> b.header("Content-Type","application/json")
		.PUT(HttpRequest.BodyPublishers.ofString(json == null ? "{}" : json));
		default        -> throw new IllegalArgumentException("Unsupported method: " + method);
		}

		bearer.ifPresent(t -> b.header("Authorization", "Bearer " + t));

		if (headers != null) {
			for (var e : headers.entrySet()) {
				if (e.getValue() != null) b.header(e.getKey(), e.getValue());
			}
		}

		var res = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
		return ensure200(res);
	}
}
