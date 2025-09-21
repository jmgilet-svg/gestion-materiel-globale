package com.materiel.suite.client.agency;

import java.net.HttpURLConnection;
import java.net.http.HttpRequest;
import java.util.Map;

/**
 * Utilitaires pour propager l'agence courante dans les entêtes HTTP.
 */
public final class AgencyHttp {
  public static final String HEADER = "X-Agency-Id";

  private AgencyHttp(){
  }

  /**
   * Ajoute l'entête {@code X-Agency-Id} si une agence est sélectionnée.
   *
   * @param builder constructeur de requête HTTP, éventuellement {@code null}
   */
  public static void apply(HttpRequest.Builder builder){
    String agencyId = AgencyContext.agencyId();
    if (builder == null || agencyId == null || agencyId.isBlank()){
      return;
    }
    builder.setHeader(HEADER, agencyId);
  }

  /**
   * Variante pour {@link HttpURLConnection}, utilisée par certains clients.
   *
   * @param connection connexion HTTP éventuellement {@code null}
   */
  public static void apply(HttpURLConnection connection){
    String agencyId = AgencyContext.agencyId();
    if (connection == null || agencyId == null || agencyId.isBlank()){
      return;
    }
    connection.setRequestProperty(HEADER, agencyId);
  }

  /**
   * Variante pour une map d'entêtes. Si la map est non modifiable, l'entête est ignorée.
   *
   * @param headers map potentiellement {@code null}
   */
  public static void apply(Map<String, String> headers){
    String agencyId = AgencyContext.agencyId();
    if (headers == null || agencyId == null || agencyId.isBlank()){
      return;
    }
    try {
      headers.put(HEADER, agencyId);
    } catch (UnsupportedOperationException ignore){
      // Map non modifiable : dans ce cas, l'appelant devra gérer l'entête différemment.
    }
  }
}
