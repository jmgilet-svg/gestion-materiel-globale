package com.materiel.suite.server.api.v2.demo;

import com.materiel.suite.client.model.Client;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.ResourceType;
import com.materiel.suite.server.api.v2.ClientControllerV2;
import com.materiel.suite.server.api.v2.ResourceControllerV2;
import com.materiel.suite.server.api.v2.ResourceTypeControllerV2;
import com.materiel.suite.server.api.v2.interventions.InterventionControllerV2;
import com.materiel.suite.server.api.v2.interventions.InterventionControllerV2.InterventionV2;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Seed “lourd” : 60 ressources (3 types) + ~60 interventions sur 2 semaines. */
@Component
public class DemoPlanningDataLoader {
  private static final String AGENCY = "_default";
  private static final String[] TYPE_CODES = {"CRANE", "TRUCK", "FORKLIFT"};
  private static final String[] TYPE_LABELS = {"Grue", "Camion", "Manutention"};
  private static final String[] TYPE_ICON_KEYS = {"crane", "truck", "forklift"};

  @EventListener
  public void onStart(ContextRefreshedEvent ev){
    try {
      seedResources();
      seedInterventions();
    } catch (Exception ignore) {
    }
  }

  private void seedResources(){
    seedResourceTypes();
    Map<String, Resource> resources = ResourceControllerV2._bucket(AGENCY);
    if (resources.size() >= 60){
      return; // déjà seedé
    }
    int seq = 1;
    Map<String, ResourceType> types = ResourceTypeControllerV2._bucket(AGENCY);
    for (int t = 0; t < TYPE_CODES.length; t++){
      String code = TYPE_CODES[t];
      String label = TYPE_LABELS[t];
      String iconKey = TYPE_ICON_KEYS[t];
      ResourceType type = copyType(types.get(code));
      if (type == null){
        type = createType(code, label, iconKey);
      }
      for (int i = 1; i <= 20; i++){
        Resource r = new Resource();
        String id = UUID.randomUUID().toString();
        safeCall(r, "setId", id);
        String base = label + "-" + String.format("%02d", i);
        safeCall(r, "setName", base);
        safeCall(r, "setLabel", base);
        safeCall(r, "setType", type);
        safeCall(r, "setIcon", iconKey);
        safeCall(r, "setEmail", "res" + (seq++) + "@demo.local");
        String actualId = get(r, "getId");
        resources.put(actualId != null ? actualId : id, r);
      }
    }
  }

  private void seedResourceTypes(){
    Map<String, ResourceType> store = ResourceTypeControllerV2._bucket(AGENCY);
    for (int i = 0; i < TYPE_CODES.length; i++){
      String code = TYPE_CODES[i];
      store.putIfAbsent(code, createType(code, TYPE_LABELS[i], TYPE_ICON_KEYS[i]));
    }
  }

  private ResourceType createType(String code, String label, String iconKey){
    ResourceType type = new ResourceType();
    type.setId(code);
    type.setName(label);
    type.setIconKey(iconKey);
    return type;
  }

  private ResourceType copyType(ResourceType src){
    if (src == null){
      return null;
    }
    ResourceType copy = new ResourceType();
    copy.setId(src.getId());
    copy.setName(src.getName());
    copy.setIconKey(src.getIconKey());
    return copy;
  }

  private void seedInterventions(){
    Map<String, InterventionV2> store = InterventionControllerV2._bucket(AGENCY);
    if (store.size() >= 60){
      return;
    }
    List<Client> clients = new ArrayList<>(getClients());
    List<Resource> resources = new ArrayList<>(ResourceControllerV2._bucket(AGENCY).values());
    if (resources.isEmpty()){
      return;
    }
    if (clients.isEmpty()){
      for (int i = 1; i <= 6; i++){
        Client c = new Client();
        safeCall(c, "setId", null);
        safeCall(c, "setName", "Client Seed " + i);
        String address = "<p>" + (100 + i) + " Avenue Démo<br/>7500" + i + " Paris</p>";
        safeCall(c, "setAddress", address);
        safeCall(c, "setBillingAddress", address);
        c = new ClientControllerV2().save(AGENCY, c);
        clients.add(c);
      }
    }
    Random rnd = new Random(42);
    LocalDate today = LocalDate.now();
    for (int k = 0; k < 60; k++){
      InterventionV2 it = new InterventionV2();
      it.id = UUID.randomUUID().toString();
      int dayOffset = rnd.nextInt(15) - 7;
      LocalDate d = today.plusDays(dayOffset);
      int startHour = 6 + rnd.nextInt(10);
      int durH = 2 + rnd.nextInt(5);
      it.plannedStart = Date.from(d.atTime(startHour, 0).atZone(ZoneId.systemDefault()).toInstant());
      it.plannedEnd = Date.from(d.atTime(Math.min(23, startHour + durH), 0).atZone(ZoneId.systemDefault()).toInstant());
      it.title = pick(new String[]{"Levage", "Livraison", "Déchargement", "Installation"}, rnd)
          + " – Chantier " + (char) ('A' + rnd.nextInt(26));
      it.type = pick(new String[]{"Intervention", "Maintenance", "Urgence"}, rnd);
      it.status = pick(new String[]{"PLANIFIEE", "PLANIFIEE", "TERMINEE"}, rnd);
      it.quoteGenerated = rnd.nextDouble() < 0.45;
      Client cli = clients.get(rnd.nextInt(clients.size()));
      it.clientId = get(cli, "getId");
      Set<String> chosen = new LinkedHashSet<>();
      int n = 1 + rnd.nextInt(3);
      for (int j = 0; j < n; j++){
        Resource r = resources.get(rnd.nextInt(resources.size()));
        String id = get(r, "getId");
        if (id != null){
          chosen.add(id);
        }
      }
      it.resourceIds.addAll(chosen);
      store.put(it.id, it);
    }
  }

  // --- Helpers réflexion sûrs ---
  private static void safeCall(Object target, String setter, Object value){
    try {
      Method m = findSetter(target.getClass(), setter, value);
      if (m != null){
        Object arg = convertValue(value, m.getParameterTypes()[0]);
        m.invoke(target, arg);
      }
    } catch (Exception ignore) {
    }
  }

  private static Object convertValue(Object value, Class<?> targetType){
    if (value == null){
      return null;
    }
    if (targetType.isInstance(value)){
      return value;
    }
    if (value instanceof String s){
      if (targetType == UUID.class){
        try {
          return UUID.fromString(s);
        } catch (IllegalArgumentException ignore) {
          return null;
        }
      }
      if (targetType == ResourceType.class){
        return new ResourceType(s, s);
      }
    }
    return value;
  }

  private static Method findSetter(Class<?> cls, String name, Object value){
    for (Method m : cls.getMethods()){
      if (!m.getName().equals(name) || m.getParameterCount() != 1){
        continue;
      }
      Class<?> pt = m.getParameterTypes()[0];
      if (value == null){
        return m;
      }
      if (pt.isInstance(value)){
        return m;
      }
      if (value instanceof String && (pt == UUID.class || pt == ResourceType.class)){
        return m;
      }
      if (pt == String.class && !(value instanceof String)){
        return null;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private static <T> T get(Object target, String getter){
    try {
      Method m = target.getClass().getMethod(getter);
      Object value = m.invoke(target);
      if (value instanceof UUID uuid){
        return (T) uuid.toString();
      }
      return (T) value;
    } catch (Exception e){
      return null;
    }
  }

  private static <T> T pick(T[] arr, Random rnd){
    return arr[rnd.nextInt(arr.length)];
  }

  private static Collection<Client> getClients(){
    try {
      ClientControllerV2 ctrl = new ClientControllerV2();
      return ctrl.list(AGENCY);
    } catch (Exception e){
      return List.of();
    }
  }
}
