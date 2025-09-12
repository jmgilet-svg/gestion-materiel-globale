package com.materiel.suite.client.net;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
    HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(baseUrl+path)).GET();
    bearer.ifPresent(t -> b.header("Authorization","Bearer "+t));
    b.header("Accept","application/json");
    var res = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    return ensureOk(res);
  }

  public String post(String path, String json) throws IOException, InterruptedException {
    HttpRequest.Builder b = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl+path))
        .header("Content-Type","application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json==null?"{}":json));
    bearer.ifPresent(t -> b.header("Authorization","Bearer "+t));
    var res = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    return ensureOk(res);
  }

  public String put(String path, String json) throws IOException, InterruptedException {
    HttpRequest.Builder b = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl+path))
        .header("Content-Type","application/json")
        .PUT(HttpRequest.BodyPublishers.ofString(json==null?"{}":json));
    bearer.ifPresent(t -> b.header("Authorization","Bearer "+t));
    var res = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    return ensureOk(res);
  }

  public String delete(String path) throws IOException, InterruptedException {
    HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create(baseUrl+path)).DELETE();
    bearer.ifPresent(t -> b.header("Authorization","Bearer "+t));
    var res = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    return ensureOk(res);
  }

  private static String ensureOk(HttpResponse<String> res) throws IOException {
    int s = res.statusCode();
    if (s>=200 && s<300) return res.body();
    throw new IOException("HTTP "+s+": "+res.body());
  }
}
