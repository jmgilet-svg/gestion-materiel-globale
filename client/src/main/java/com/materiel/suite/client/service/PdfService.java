package com.materiel.suite.client.service;

import java.util.Map;

/** Façade client pour le rendu HTML -> PDF via l'API backend. */
public interface PdfService {
  byte[] render(String html, Map<String, String> inlineImages, String baseUrl);
}
