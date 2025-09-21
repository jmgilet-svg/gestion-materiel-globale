package com.materiel.suite.client.agency;

import com.materiel.suite.client.auth.Agency;
import com.materiel.suite.client.net.SimpleJson;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintient le contexte d'agence courant côté client ainsi qu'un catalogue
 * de fonctionnalités activées par agence.
 */
public final class AgencyContext {
  private static final String DEFAULT_KEY = "_default";

  private static volatile String currentAgencyId;
  private static volatile String currentAgencyLabel;
  private static final Map<String, Set<String>> FEATURES_BY_AGENCY = new ConcurrentHashMap<>();
  private static final Map<String, String> LABEL_BY_ID = new ConcurrentHashMap<>();
  private static final Map<String, String> ID_BY_LABEL = new ConcurrentHashMap<>();

  private AgencyContext(){
  }

  public static String agencyId(){
    return currentAgencyId;
  }

  public static String agencyLabel(){
    return currentAgencyLabel;
  }

  public static void setAgency(Agency agency){
    if (agency == null){
      setAgency(null, null);
      return;
    }
    setAgency(agency.getId(), agency.getName());
  }

  public static void setAgency(String id, String label){
    String normalizedId = normalize(id);
    String normalizedLabel = normalize(label);
    currentAgencyId = normalizedId;
    currentAgencyLabel = normalizedLabel;
    rememberMapping(normalizedId, normalizedLabel);
  }

  public static void clear(){
    currentAgencyId = null;
    currentAgencyLabel = null;
  }

  public static boolean hasFeature(String featureKey){
    if (featureKey == null || featureKey.isBlank()){
      return true;
    }
    String normalized = featureKey.trim().toUpperCase(Locale.ROOT);
    Set<String> features = featuresFor(currentAgencyId);
    if (features.contains(normalized)){
      return true;
    }
    if (features.isEmpty() && currentAgencyLabel != null){
      String altId = ID_BY_LABEL.get(currentAgencyLabel.toUpperCase(Locale.ROOT));
      if (altId != null){
        features = featuresFor(altId);
        if (features.contains(normalized)){
          return true;
        }
      }
    }
    Set<String> defaults = FEATURES_BY_AGENCY.getOrDefault(DEFAULT_KEY, Collections.emptySet());
    return defaults.contains(normalized);
  }

  public static void setFeatures(String agencyId, Collection<String> features){
    String key = normalize(agencyId);
    if (key == null){
      key = DEFAULT_KEY;
    }
    Set<String> values = new HashSet<>();
    if (features != null){
      for (String feature : features){
        if (feature == null || feature.isBlank()){
          continue;
        }
        values.add(feature.trim().toUpperCase(Locale.ROOT));
      }
    }
    FEATURES_BY_AGENCY.put(key, Collections.unmodifiableSet(values));
  }

  public static void loadLocalFallback(){
    try (InputStream is = AgencyContext.class.getResourceAsStream("/agency/features.json")){
      if (is == null){
        return;
      }
      String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      Object parsed = SimpleJson.parse(json);
      Map<String, Object> map = parsed instanceof Map<?,?> m
          ? castMap(m)
          : Map.of();
      for (Map.Entry<String, Object> entry : map.entrySet()){
        String agency = normalize(entry.getKey());
        List<String> features = new ArrayList<>();
        Object value = entry.getValue();
        if (value instanceof Collection<?> collection){
          for (Object item : collection){
            if (item != null){
              features.add(String.valueOf(item));
            }
          }
        }
        setFeatures(agency, features);
      }
    } catch (Exception ignore){
    }
  }

  public static List<String> knownAgencyIds(){
    List<String> ids = new ArrayList<>();
    for (String key : FEATURES_BY_AGENCY.keySet()){
      if (DEFAULT_KEY.equalsIgnoreCase(key)){
        continue;
      }
      ids.add(key);
    }
    ids.sort(String::compareToIgnoreCase);
    return ids;
  }

  public static boolean matchesCurrentAgency(Object bean){
    if (bean == null){
      return true;
    }
    String expectedId = normalize(currentAgencyId);
    String expectedLabel = normalize(currentAgencyLabel);
    if (expectedId == null && expectedLabel == null){
      return true;
    }
    String candidate = extractAgencyValue(bean);
    if (candidate == null || candidate.isBlank()){
      return true;
    }
    return matchesAgencyValue(candidate, expectedId, expectedLabel);
  }

  private static boolean matchesAgencyValue(String candidate, String expectedId, String expectedLabel){
    String normalized = candidate.trim();
    if (expectedId != null && equalsIgnoreCase(normalized, expectedId)){
      return true;
    }
    if (expectedLabel != null && equalsIgnoreCase(normalized, expectedLabel)){
      return true;
    }
    if (expectedId != null){
      String label = LABEL_BY_ID.get(expectedId);
      if (label != null && equalsIgnoreCase(normalized, label)){
        return true;
      }
    }
    if (expectedLabel != null){
      String mappedId = ID_BY_LABEL.get(expectedLabel.toUpperCase(Locale.ROOT));
      if (mappedId != null && equalsIgnoreCase(normalized, mappedId)){
        return true;
      }
    }
    String upperCandidate = normalized.toUpperCase(Locale.ROOT);
    if (expectedId != null && upperCandidate.contains(expectedId.toUpperCase(Locale.ROOT))){
      return true;
    }
    if (expectedLabel != null && upperCandidate.contains(expectedLabel.toUpperCase(Locale.ROOT))){
      return true;
    }
    if (expectedId != null){
      String label = LABEL_BY_ID.get(expectedId);
      if (label != null && upperCandidate.contains(label.toUpperCase(Locale.ROOT))){
        return true;
      }
    }
    if (expectedLabel != null){
      String mappedId = ID_BY_LABEL.get(expectedLabel.toUpperCase(Locale.ROOT));
      if (mappedId != null && upperCandidate.contains(mappedId.toUpperCase(Locale.ROOT))){
        return true;
      }
    }
    return false;
  }

  private static String extractAgencyValue(Object bean){
    if (bean == null){
      return null;
    }
    if (bean instanceof Agency agency){
      return firstNonBlank(agency.getId(), agency.getName());
    }
    for (String method : List.of("getAgencyId", "getAgencyCode", "getAgency", "getAgencyName")){
      try {
        Object value = bean.getClass().getMethod(method).invoke(bean);
        String candidate = convertValue(value);
        if (candidate != null){
          return candidate;
        }
      } catch (ReflectiveOperationException ignore){
      }
    }
    for (String field : List.of("agencyId", "agency", "agencyCode")){
      try {
        var f = bean.getClass().getDeclaredField(field);
        f.setAccessible(true);
        Object value = f.get(bean);
        String candidate = convertValue(value);
        if (candidate != null){
          return candidate;
        }
      } catch (ReflectiveOperationException ignore){
      }
    }
    return convertValue(bean);
  }

  private static String convertValue(Object value){
    if (value == null){
      return null;
    }
    if (value instanceof Agency agency){
      return firstNonBlank(agency.getId(), agency.getName());
    }
    if (value instanceof CharSequence sequence){
      return sequence.toString();
    }
    try {
      var getId = value.getClass().getMethod("getId");
      Object id = getId.invoke(value);
      if (id != null){
        return id.toString();
      }
    } catch (ReflectiveOperationException ignore){
    }
    return value.toString();
  }

  private static void rememberMapping(String id, String label){
    if (id != null && label != null){
      LABEL_BY_ID.put(id, label);
      ID_BY_LABEL.put(label.toUpperCase(Locale.ROOT), id);
    }
  }

  private static Set<String> featuresFor(String agencyId){
    if (agencyId != null){
      String normalized = normalize(agencyId);
      if (normalized != null){
        Set<String> direct = FEATURES_BY_AGENCY.get(normalized);
        if (direct != null){
          return direct;
        }
        Set<String> upper = FEATURES_BY_AGENCY.get(normalized.toUpperCase(Locale.ROOT));
        if (upper != null){
          return upper;
        }
      }
    }
    return FEATURES_BY_AGENCY.getOrDefault(DEFAULT_KEY, Collections.emptySet());
  }

  private static boolean equalsIgnoreCase(String a, String b){
    return Objects.equals(normalize(a), normalize(b));
  }

  private static String normalize(String value){
    if (value == null){
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static String firstNonBlank(String first, String second){
    String normalizedFirst = normalize(first);
    if (normalizedFirst != null){
      return normalizedFirst;
    }
    return normalize(second);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> castMap(Map<?, ?> map){
    Map<String, Object> out = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet()){
      if (entry.getKey() != null){
        out.put(String.valueOf(entry.getKey()), entry.getValue());
      }
    }
    return out;
  }
}
