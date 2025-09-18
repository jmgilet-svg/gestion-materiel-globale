package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.Contact;
import com.materiel.suite.client.model.Conflict;
import com.materiel.suite.client.model.Intervention;
import com.materiel.suite.client.model.InterventionType;
import com.materiel.suite.client.model.Resource;
import com.materiel.suite.client.model.ResourceRef;
import com.materiel.suite.client.model.ResourceType;
import com.materiel.suite.client.model.Unavailability;
import com.materiel.suite.client.service.PlanningService;
import com.materiel.suite.client.service.PlanningValidation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MockPlanningService implements PlanningService {
  private final Map<UUID, Resource> resources = new ConcurrentHashMap<>();
  private final Map<String, ResourceType> resourceTypes = new LinkedHashMap<>();
  private final Map<UUID, Intervention> interventions = new ConcurrentHashMap<>();

  private static final String[] RESOURCE_STATES = { "Disponible", "Occupée", "En maintenance" };
  private static final String[] RESOURCE_COLORS = { "#5E81AC", "#A3BE8C", "#EBCB8B", "#D08770", "#88C0D0", "#B48EAD", "#BF616A" };
  private static final String[] INTERVENTION_STATUSES = { "PLANNED", "CONFIRMED", "TENTATIVE", "DONE" };
  private static final String[] INTERVENTION_AGENCIES = { "Agence Lyon", "Agence Paris" };
  private static final String[] CLIENT_NAMES = { "Durand BTP", "MontBlanc Levage", "Atelier Urbain", "Sogena Logistique", "Structura", "BTP Horizon", "EuroChantier" };
  private static final String[] SITE_KINDS = { "Chantier", "Zone logistique", "Usine", "Site portuaire", "Parc expo", "Plateforme" };
  private static final String[] CITY_NAMES = { "Lyon", "Villeurbanne", "Grenoble", "Saint-Étienne", "Valence", "Chambéry", "Dijon", "Annecy" };
  private static final String[] DRIVER_FIRST_NAMES = { "Bernard", "Camille", "Nadia", "Léo", "Agnès", "Hugo", "Sonia", "Mehdi" };
  private static final String[] CONTACT_FIRST_NAMES = { "Julie", "Karim", "Laura", "Mathieu", "Nora", "Thierry", "Lucie", "Romain", "Amélie", "Yanis" };
  private static final String[] CONTACT_LAST_NAMES = { "Durand", "Martel", "Nguyen", "Giraud", "Lopez", "Fabre", "Bernier", "Morel", "Lefèvre", "Perrin" };
  private static final String[] CONTACT_ROLES = { "Chef de chantier", "Conducteur de travaux", "Responsable HSE", "Coordinateur logistique" };
  private static final String[] TRUCK_MODELS = { "Actros 26t", "Arocs 8x4", "FH16 650", "TGS 35.510", "Range C 460", "CF 480" };
  private static final String[] CRANE_MODELS = { "LTM 1050-3.1", "GMK 4100L-1", "AC 100-4L", "LTM 1150-5.3", "GMK 5250L", "AC 80-4" };
  private static final String[] HANDLING_EQUIPMENT = { "Chariot 3T", "Gerbeur 2T", "Transpalette motorisé", "Chariot 5T", "Mini-grue atelier" };
  private static final String[] STREET_NAMES = { "rue des Forges", "avenue du Stade", "boulevard Lumière", "chemin des Artisans", "route de la Plaine", "quai Industriel", "impasse des Frères Lumière" };
  private static final String[] INTERVENTION_COLORS = { "#5E81AC", "#A3BE8C", "#EBCB8B", "#BF616A", "#88C0D0", "#D08770", "#B48EAD" };
  private static final String[] INTERNAL_NOTES = { "Prévoir badge d'accès et EPI.", "Contrôler arrimage avant départ.", "Informer le client 24h à l'avance." };
  private static final String[] CLOSING_NOTES = { "RAS, intervention conforme.", "Client satisfait, photos envoyées.", "Retard mineur, prévenir la facturation." };
  private static final String[] UNAVAILABILITY_REASONS = { "Maintenance planifiée", "Contrôle technique", "Nettoyage cabine" };
  private static final Map<String, String> TYPE_TO_RESOURCE = Map.of(
      "LIFT", "CRANE",
      "TRANSPORT", "TRUCK",
      "MANUT", "FORKLIFT"
  );

  public MockPlanningService(){
    initResourceTypes();
    if (resources.isEmpty()){
      seedResources();
    }
    if (interventions.isEmpty()){
      seedInterventions();
    }
  }

  private void initResourceTypes(){
    if (!resourceTypes.isEmpty()){
      return;
    }
    resourceTypes.put("CRANE", new ResourceType("CRANE", "Grue mobile", "crane"));
    resourceTypes.put("TRUCK", new ResourceType("TRUCK", "Camion plateau", "truck"));
    resourceTypes.put("FORKLIFT", new ResourceType("FORKLIFT", "Chariot élévateur", "forklift"));
    resourceTypes.put("CONTAINER", new ResourceType("CONTAINER", "Conteneur", "container"));
    resourceTypes.put("GENERIC", new ResourceType("GENERIC", "Ressource", "tag"));
  }

  private void seedResources(){
    Random rnd = new Random(42);
    ResourceType[] baseTypes = {
        resourceTypes.get("CRANE"),
        resourceTypes.get("TRUCK"),
        resourceTypes.get("FORKLIFT"),
        resourceTypes.get("CONTAINER")
    };
    int globalIndex = 0;
    for (ResourceType type : baseTypes){
      if (type == null){
        continue;
      }
      for (int seq = 1; seq <= 15; seq++){
        Resource resource = createResource(type, seq, globalIndex++, rnd);
        resources.put(resource.getId(), resource);
      }
    }
    ResourceType truck = resourceTypes.get("TRUCK");
    for (int extra = 1; extra <= 5; extra++){
      Resource resource = createResource(truck, 15 + extra, globalIndex++, rnd);
      resource.setName("Convoi exceptionnel #" + extra);
      resource.setState("Disponible");
      resource.setUnitPriceHt(BigDecimal.valueOf(850 + extra * 40L).setScale(2, RoundingMode.HALF_UP));
      resource.setCapacity(2);
      resource.setTags("camion,convoi");
      resource.setWeeklyUnavailability("SAT 06:00-12:00");
      resource.getUnavailabilities().clear();
      resources.put(resource.getId(), resource);
    }
  }

  private Resource createResource(ResourceType baseType, int sequence, int globalIndex, Random rnd){
    ResourceType effectiveType = baseType != null ? copyType(baseType) : copyType(resourceTypes.get("GENERIC"));
    Resource resource = new Resource(UUID.randomUUID(), nameFor(effectiveType, sequence));
    resource.setType(effectiveType);
    resource.setState(RESOURCE_STATES[rnd.nextInt(RESOURCE_STATES.length)]);
    resource.setUnitPriceHt(priceFor(effectiveType, sequence, rnd));
    resource.setCapacity(capacityFor(effectiveType, rnd));
    resource.setTags(tagsFor(effectiveType, rnd));
    resource.setWeeklyUnavailability(weeklyPattern(rnd));
    resource.setColor(RESOURCE_COLORS[globalIndex % RESOURCE_COLORS.length]);
    if (rnd.nextInt(4) == 0){
      LocalDate lastCheck = LocalDate.now().minusDays(rnd.nextInt(180));
      resource.setNotes("Dernier contrôle: " + lastCheck);
    }
    maybeAddUnavailability(resource, rnd);
    return resource;
  }

  private String nameFor(ResourceType type, int sequence){
    if (type == null || type.getCode() == null){
      return "Ressource " + sequence;
    }
    return switch (type.getCode()){
      case "CRANE" -> "Grue LTM " + (900 + sequence * 5);
      case "TRUCK" -> "Camion plateau " + (16 + (sequence % 4) * 4) + "T";
      case "FORKLIFT" -> "Chariot " + (2 + (sequence % 6)) + "T";
      case "CONTAINER" -> "Module conteneur " + (10 + (sequence % 4) * 5) + "ft";
      default -> "Ressource " + sequence;
    };
  }

  private BigDecimal priceFor(ResourceType type, int sequence, Random rnd){
    String code = type != null ? type.getCode() : "GENERIC";
    double base = switch (code){
      case "CRANE" -> 900 + sequence * 20;
      case "TRUCK" -> 400 + (sequence % 8) * 30;
      case "FORKLIFT" -> 150 + (sequence % 6) * 25;
      case "CONTAINER" -> 50 + (sequence % 5) * 10;
      default -> 120;
    };
    double jitter = rnd.nextInt(30) - 15;
    return BigDecimal.valueOf(Math.max(30, base + jitter)).setScale(2, RoundingMode.HALF_UP);
  }

  private int capacityFor(ResourceType type, Random rnd){
    if (type == null || type.getCode() == null){
      return 1;
    }
    return switch (type.getCode()){
      case "CRANE" -> 1 + rnd.nextInt(2);
      case "TRUCK" -> 1 + rnd.nextInt(2);
      case "FORKLIFT" -> 1;
      case "CONTAINER" -> 2 + rnd.nextInt(2);
      default -> 1;
    };
  }

  private String tagsFor(ResourceType type, Random rnd){
    if (type == null || type.getCode() == null){
      return "ressource";
    }
    return switch (type.getCode()){
      case "CRANE" -> "grue," + (80 + rnd.nextInt(40)) + "t";
      case "TRUCK" -> "camion," + (16 + rnd.nextInt(12)) + "t";
      case "FORKLIFT" -> "chariot," + (2 + rnd.nextInt(5)) + "t";
      case "CONTAINER" -> "conteneur," + (10 + rnd.nextInt(10)) + "ft";
      default -> "ressource";
    };
  }

  private String weeklyPattern(Random rnd){
    if (rnd.nextInt(3) != 0){
      return null;
    }
    String[] days = { "MON", "TUE", "WED", "THU", "FRI" };
    String firstDay = days[rnd.nextInt(days.length)];
    int start = 7 + rnd.nextInt(5);
    int end = start + 3 + rnd.nextInt(3);
    String first = firstDay + " " + twoDigits(start) + ":00-" + twoDigits(end) + ":00";
    if (rnd.nextBoolean()){
      return first;
    }
    String secondDay = days[rnd.nextInt(days.length)];
    while (secondDay.equals(firstDay)){
      secondDay = days[rnd.nextInt(days.length)];
    }
    int start2 = 13 + rnd.nextInt(3);
    int end2 = start2 + 3 + rnd.nextInt(2);
    return first + "; " + secondDay + " " + twoDigits(start2) + ":00-" + twoDigits(end2) + ":00";
  }

  private String twoDigits(int value){
    return value < 10 ? "0" + value : Integer.toString(value);
  }

  private void maybeAddUnavailability(Resource resource, Random rnd){
    if (rnd.nextInt(4) != 0){
      return;
    }
    LocalDate base = LocalDate.now().plusDays(rnd.nextInt(21));
    LocalDateTime start = base.atTime(7 + rnd.nextInt(8), rnd.nextBoolean() ? 0 : 30);
    LocalDateTime end = start.plusHours(2 + rnd.nextInt(4));
    String reason = UNAVAILABILITY_REASONS[rnd.nextInt(UNAVAILABILITY_REASONS.length)];
    resource.getUnavailabilities().add(new Unavailability(UUID.randomUUID(), start, end, reason));
  }

  private void seedInterventions(){
    if (resources.isEmpty()){
      return;
    }
    Random rnd = new Random(123);
    List<Resource> resourceList = new ArrayList<>(resources.values());
    Map<String, List<Resource>> byType = new HashMap<>();
    for (Resource resource : resourceList){
      String code = resource.getType() != null ? resource.getType().getCode() : "GENERIC";
      byType.computeIfAbsent(code, key -> new ArrayList<>()).add(resource);
    }
    List<InterventionType> types = List.of(
        new InterventionType("LIFT", "Levage", "crane"),
        new InterventionType("TRANSPORT", "Transport", "truck"),
        new InterventionType("MANUT", "Manutention", "forklift")
    );
    LocalDate startWeek = LocalDate.now().with(DayOfWeek.MONDAY);
    LocalDate endWeek = startWeek.plusWeeks(2).with(DayOfWeek.SUNDAY);
    int span = (int) ChronoUnit.DAYS.between(startWeek, endWeek) + 1;
    for (int idx = 0; idx < 60; idx++){
      InterventionType type = types.get(rnd.nextInt(types.size()));
      String target = TYPE_TO_RESOURCE.getOrDefault(type.getCode(), "GENERIC");
      List<Resource> candidates = byType.get(target);
      if (candidates == null || candidates.isEmpty()){
        candidates = resourceList;
      }
      Resource primary = candidates.get(rnd.nextInt(candidates.size()));
      LocalDate day = startWeek.plusDays(rnd.nextInt(span));
      int startHour = 7 + rnd.nextInt(8);
      int startMinute = rnd.nextBoolean() ? 0 : 30;
      LocalDateTime start = day.atTime(startHour, startMinute);
      LocalDateTime end = start.plusHours(2 + rnd.nextInt(5));
      String color = INTERVENTION_COLORS[idx % INTERVENTION_COLORS.length];
      String label = labelFor(type, idx);
      Intervention intervention = new Intervention(UUID.randomUUID(), primary.getId(), label, start, end, color);
      attachPrimary(intervention, primary);
      decorateIntervention(intervention, type, primary, resourceList, rnd);
      add(intervention);
    }
  }

  private void decorateIntervention(Intervention intervention, InterventionType type, Resource primary, List<Resource> allResources, Random rnd){
    intervention.setType(type);
    String client = CLIENT_NAMES[rnd.nextInt(CLIENT_NAMES.length)];
    String city = CITY_NAMES[rnd.nextInt(CITY_NAMES.length)];
    String site = SITE_KINDS[rnd.nextInt(SITE_KINDS.length)] + " " + city;
    intervention.setClientName(client);
    intervention.setSiteLabel(site);
    intervention.setAddress(sampleAddress(rnd, city));
    intervention.setAgency(INTERVENTION_AGENCIES[rnd.nextInt(INTERVENTION_AGENCIES.length)]);
    String status = INTERVENTION_STATUSES[rnd.nextInt(INTERVENTION_STATUSES.length)];
    intervention.setStatus(status);
    intervention.setDescription("Opération " + type.getLabel() + " pour " + client + ".");
    String driver = randomDriver(rnd);
    intervention.setDriverName(driver);
    String typeCode = type.getCode();
    if ("LIFT".equals(typeCode)){
      intervention.setCraneName(primary.getName());
      intervention.setTruckName(TRUCK_MODELS[rnd.nextInt(TRUCK_MODELS.length)]);
    } else if ("TRANSPORT".equals(typeCode)){
      intervention.setTruckName(primary.getName());
      if (rnd.nextBoolean()){
        intervention.setCraneName(CRANE_MODELS[rnd.nextInt(CRANE_MODELS.length)]);
      }
    } else {
      intervention.setCraneName(primary.getName());
      intervention.setTruckName(HANDLING_EQUIPMENT[rnd.nextInt(HANDLING_EQUIPMENT.length)]);
    }
    if (rnd.nextInt(4) == 0){
      intervention.setInternalNote(INTERNAL_NOTES[rnd.nextInt(INTERNAL_NOTES.length)]);
    }
    intervention.setFavorite(rnd.nextInt(6) == 0);
    intervention.setLocked(rnd.nextInt(10) == 0);
    if (rnd.nextInt(4) == 0){
      intervention.setQuoteNumber(String.format(Locale.ROOT, "Q-2025-%03d", 150 + rnd.nextInt(400)));
    }
    if (rnd.nextInt(5) == 0){
      intervention.setOrderNumber(String.format(Locale.ROOT, "CMD-2025-%04d", 50 + rnd.nextInt(300)));
    }
    if (rnd.nextInt(6) == 0){
      intervention.setDeliveryNumber(String.format(Locale.ROOT, "BL-2025-%04d", 100 + rnd.nextInt(400)));
    }
    if (rnd.nextInt(7) == 0){
      intervention.setInvoiceNumber(String.format(Locale.ROOT, "FAC-2025-%04d", 40 + rnd.nextInt(200)));
    }
    if ("DONE".equals(status)){
      intervention.setActualStart(intervention.getDateHeureDebut().plusMinutes(rnd.nextInt(20)));
      intervention.setActualEnd(intervention.getDateHeureFin().plusMinutes(rnd.nextInt(30)));
      intervention.setClosingNote(CLOSING_NOTES[rnd.nextInt(CLOSING_NOTES.length)]);
    } else if (rnd.nextInt(8) == 0){
      intervention.setClosingNote("Compte rendu en attente.");
    }
    if (rnd.nextInt(4) == 0){
      intervention.setContacts(randomContacts(rnd, client));
    }
    if (rnd.nextInt(3) == 0){
      addExtraResources(intervention, allResources, primary.getId(), rnd);
    }
  }

  private String labelFor(InterventionType type, int index){
    if (type == null || type.getCode() == null){
      return "Intervention #" + (1000 + index);
    }
    return switch (type.getCode()){
      case "LIFT" -> "Levage — chantier #" + (1000 + index);
      case "TRANSPORT" -> "Transport — lot #" + (2000 + index);
      case "MANUT" -> "Manutention — opération #" + (3000 + index);
      default -> "Intervention #" + (4000 + index);
    };
  }

  private String sampleAddress(Random rnd, String city){
    String street = STREET_NAMES[rnd.nextInt(STREET_NAMES.length)];
    int number = 5 + rnd.nextInt(120);
    return number + " " + street + ", " + city;
  }

  private void addExtraResources(Intervention intervention, List<Resource> allResources, UUID primaryId, Random rnd){
    if (allResources.size() <= 1){
      return;
    }
    int extras = 1 + rnd.nextInt(Math.min(2, allResources.size() - 1));
    Set<UUID> used = new HashSet<>();
    used.add(primaryId);
    int attempts = 0;
    while (extras > 0 && attempts < allResources.size() * 2){
      Resource candidate = allResources.get(rnd.nextInt(allResources.size()));
      attempts++;
      if (!used.add(candidate.getId())){
        continue;
      }
      intervention.addResource(new ResourceRef(candidate.getId(), candidate.getName(), iconOf(candidate)));
      extras--;
    }
  }

  private List<Contact> randomContacts(Random rnd, String client){
    int count = 1 + rnd.nextInt(2);
    List<Contact> list = new ArrayList<>(count);
    for (int i = 0; i < count; i++){
      Contact contact = new Contact();
      contact.setId(UUID.randomUUID());
      String first = CONTACT_FIRST_NAMES[rnd.nextInt(CONTACT_FIRST_NAMES.length)];
      String last = CONTACT_LAST_NAMES[rnd.nextInt(CONTACT_LAST_NAMES.length)];
      contact.setFirstName(first);
      contact.setLastName(last);
      contact.setEmail((first + "." + last).toLowerCase(Locale.ROOT).replace(' ', '-') + "@" + domainFromClient(client));
      contact.setPhone(randomPhone(rnd));
      contact.setRole(CONTACT_ROLES[rnd.nextInt(CONTACT_ROLES.length)]);
      list.add(contact);
    }
    return list;
  }

  private String domainFromClient(String client){
    if (client == null || client.isBlank()){
      return "client.local";
    }
    String slug = client.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    slug = slug.replaceAll("^-+", "").replaceAll("-+$", "");
    if (slug.isBlank()){
      slug = "client";
    }
    return slug + ".fr";
  }

  private String randomPhone(Random rnd){
    StringBuilder sb = new StringBuilder("+33 6");
    for (int block = 0; block < 4; block++){
      int value = rnd.nextInt(100);
      sb.append(' ').append(value < 10 ? "0" : "").append(value);
    }
    return sb.toString();
  }

  private String randomDriver(Random rnd){
    String first = DRIVER_FIRST_NAMES[rnd.nextInt(DRIVER_FIRST_NAMES.length)];
    String last = CONTACT_LAST_NAMES[rnd.nextInt(CONTACT_LAST_NAMES.length)];
    return first + " " + last;
  }

  private void add(Intervention i){ interventions.put(i.getId(), i); }

  private Intervention attachPrimary(Intervention i, Resource r){
    if (i!=null && r!=null){
      i.setResources(List.of(new ResourceRef(r.getId(), r.getName(), iconOf(r))));
    }
    return i;
  }

  @Override public List<Resource> listResources(){
    List<Resource> list = new ArrayList<>(resources.values());
    list.sort(Comparator.comparing(resource -> resource.getName() == null ? "" : resource.getName(),
        String.CASE_INSENSITIVE_ORDER));
    return list;
  }
  @Override public Resource saveResource(Resource r){
    if(r.getId()==null) r.setId(UUID.randomUUID());
    resources.put(r.getId(), r);
    return r;
  }
  @Override public void deleteResource(UUID id){ resources.remove(id); }
  @Override public List<ResourceType> listResourceTypes(){ return new ArrayList<>(resourceTypes.values()); }
  @Override public ResourceType createResourceType(ResourceType type){
    ResourceType copy = copyType(type);
    resourceTypes.put(copy.getCode(), copy);
    return copy;
  }
  @Override public ResourceType updateResourceType(ResourceType type){
    ResourceType copy = copyType(type);
    resourceTypes.put(copy.getCode(), copy);
    return copy;
  }
  @Override public void deleteResourceType(String code){
    if (code==null || code.isBlank()) return;
    resourceTypes.remove(code);
    ResourceType fallback = resourceTypes.get("GENERIC");
    for (Resource r : resources.values()){
      ResourceType t = r.getType();
      if (t!=null && code.equals(t.getCode())){
        r.setType(fallback);
      }
    }
  }
  @Override public List<Unavailability> listResourceUnavailabilities(UUID resourceId){
    Resource r = resources.get(resourceId);
    return r==null? List.of() : new ArrayList<>(r.getUnavailabilities());
  }
  @Override public Unavailability addUnavailability(UUID resourceId, Unavailability u){
    Resource r = resources.get(resourceId);
    if (r==null) throw new NoSuchElementException("resource not found");
    Unavailability copy = new Unavailability(
        u.getId()!=null? u.getId():UUID.randomUUID(),
        u.getStart(),
        u.getEnd(),
        u.getReason()
    );
    r.getUnavailabilities().add(copy);
    return copy;
  }
  @Override public void deleteUnavailability(UUID resourceId, UUID unavailabilityId){
    Resource r = resources.get(resourceId);
    if (r==null) return;
    if (unavailabilityId==null) return;
    r.getUnavailabilities().removeIf(u -> unavailabilityId.equals(u.getId()));
  }

  @Override public List<Intervention> listInterventions(LocalDate from, LocalDate to){
    LocalDate start = from != null ? from : LocalDate.MIN;
    LocalDate end = to != null ? to : LocalDate.MAX;
    List<Intervention> list = new ArrayList<>();
    for (var intervention : interventions.values()){
      LocalDate debut = intervention.getDateDebut();
      LocalDate fin = intervention.getDateFin();
      boolean overlap = (fin == null || !fin.isBefore(start)) && (debut == null || !debut.isAfter(end));
      if (overlap){
        list.add(intervention);
      }
    }
    list.sort(Comparator
        .comparing(Intervention::getResourceId, Comparator.nullsLast(UUID::compareTo))
        .thenComparing(Intervention::getDateDebut, Comparator.nullsLast(LocalDate::compareTo))
        .thenComparing(Intervention::getStartDateTime));
    return list;
  }
  @Override public Intervention saveIntervention(Intervention i){
    if(i.getId()==null) i.setId(UUID.randomUUID());
    if (i.getResourceId()!=null && i.getResources().isEmpty()){
      Resource r = resources.get(i.getResourceId());
      if (r!=null){
        i.setResources(List.of(new ResourceRef(r.getId(), r.getName(), iconOf(r))));
      }
    }
    interventions.put(i.getId(), i);
    return i;
  }
  @Override public void deleteIntervention(UUID id){ interventions.remove(id); }

  @Override public List<Conflict> listConflicts(LocalDate from, LocalDate to){
    List<Conflict> out = new ArrayList<>();
    for (Resource resource : resources.values()){
      UUID resourceId = resource.getId();
      List<Intervention> related = listInterventions(from, to);
      related.removeIf(i -> !Objects.equals(i.getResourceId(), resourceId));
      related.sort(Comparator.comparing(Intervention::getStartDateTime));
      LocalDateTime lastEnd = null; Intervention last = null;
      for (var it : related){
        LocalDateTime startTime = it.getStartDateTime();
        LocalDateTime endTime = it.getEndDateTime();
        if (lastEnd != null && !startTime.isAfter(lastEnd)){
          out.add(new Conflict(last.getId(), it.getId(), resourceId));
        }
        if (lastEnd == null || endTime.isAfter(lastEnd)){
          lastEnd = endTime;
          last = it;
        }
      }
    }
    return out;
  }

  @Override public boolean resolveShift(UUID id, int minutes){
    Intervention i = interventions.get(id);
    if (i==null) return false;
    LocalDateTime start = i.getDateHeureDebut();
    LocalDateTime end = i.getDateHeureFin();
    if (start != null){
      i.setDateHeureDebut(start.plusMinutes(minutes));
    }
    if (end != null){
      i.setDateHeureFin(end.plusMinutes(minutes));
    }
    return start != null || end != null;
  }
  @Override public boolean resolveReassign(UUID id, UUID resourceId){
    Intervention i = interventions.get(id);
    if (i==null) return false;
    for (var it : interventions.values()){
      if (Objects.equals(it.getId(), id)) continue;
      if (!Objects.equals(it.getResourceId(), resourceId)) continue;
      LocalDateTime iStart = i.getDateHeureDebut();
      LocalDateTime iEnd = i.getDateHeureFin();
      LocalDateTime otherStart = it.getDateHeureDebut();
      LocalDateTime otherEnd = it.getDateHeureFin();
      if (iStart == null || iEnd == null || otherStart == null || otherEnd == null) continue;
      boolean overlap = !iEnd.isBefore(otherStart) && !iStart.isAfter(otherEnd);
      if (overlap) return false;
    }
    i.setResourceId(resourceId);
    Resource target = resources.get(resourceId);
    if (target!=null){
      i.setResources(List.of(new ResourceRef(target.getId(), target.getName(), iconOf(target))));
    } else {
      i.setResources(List.of());
    }
    return true;
  }
  @Override public boolean resolveSplit(UUID id, LocalDateTime splitAt){
    Intervention i = interventions.get(id);
    if (i==null) return false;
    LocalDateTime start = i.getDateHeureDebut();
    LocalDateTime end = i.getDateHeureFin();
    if (start == null || end == null) return false;
    if (!splitAt.isAfter(start) || !end.isAfter(splitAt)) return false;
    Intervention tail = new Intervention(UUID.randomUUID(), i.getResourceId(), i.getLabel()+" (suite)",
        splitAt.toLocalDate(), end.toLocalDate(), i.getColor());
    tail.setResources(i.getResources());
    tail.setDateHeureDebut(splitAt);
    tail.setDateHeureFin(end);
    interventions.put(tail.getId(), tail);
    i.setDateHeureFin(splitAt.minusMinutes(1));
    return true;
  }

  private ResourceType copyType(ResourceType type){
    if (type==null || type.getCode()==null || type.getCode().isBlank()){
      throw new IllegalArgumentException("code requis");
    }
    String code = type.getCode();
    ResourceType existing = resourceTypes.get(code);
    String label = type.getLabel();
    if (label==null || label.isBlank()){
      if (existing!=null && existing.getLabel()!=null && !existing.getLabel().isBlank()) label = existing.getLabel();
      else label = code;
    }
    String icon = type.getIcon();
    if (icon==null || icon.isBlank()){
      if (existing!=null && existing.getIcon()!=null && !existing.getIcon().isBlank()) icon = existing.getIcon();
    }
    return new ResourceType(code, label, icon);
  }

  private String iconOf(Resource r){
    if (r==null) return null;
    ResourceType t = r.getType();
    return t!=null? t.getIcon() : null;
  }

  @Override public PlanningValidation validate(Intervention i){
    PlanningValidation v = new PlanningValidation();
    LocalDateTime s = i.getDateHeureDebut();
    LocalDateTime e = i.getDateHeureFin();
    if (s==null || e==null){ v.ok = true; return v; }
    for (var other : interventions.values()){
      if (i.getId()!=null && i.getId().equals(other.getId())) continue;
      if (!Objects.equals(i.getResourceId(), other.getResourceId())) continue;
      LocalDateTime os = other.getDateHeureDebut();
      LocalDateTime oe = other.getDateHeureFin();
      if (os == null || oe == null) continue;
      boolean overlap = !e.isBefore(os) && !oe.isBefore(s);
      if (overlap){
        v.ok = false;
        PlanningValidation.Suggestion sug = new PlanningValidation.Suggestion();
        sug.resourceId = i.getResourceId();
        Duration dur = Duration.between(s,e);
        sug.startDateTime = oe;
        sug.endDateTime = oe.plus(dur);
        sug.label = "Décaler après le créneau suivant";
        v.suggestions.add(sug);
      }
    }
    if (v.suggestions.isEmpty()) v.ok = true;
    return v;
  }
}
