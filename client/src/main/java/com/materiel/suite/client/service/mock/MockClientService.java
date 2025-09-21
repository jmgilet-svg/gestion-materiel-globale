package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.Client;
import com.materiel.suite.client.model.Contact;
import com.materiel.suite.client.service.ClientService;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MockClientService implements ClientService {
  private static final String[][] COMPANY_NAMES = {
      {"Chantiers", "Dupont"}, {"Transports", "Durand"}, {"Levage", "Martin"},
      {"BTP", "Leroy"}, {"Logistique", "Girard"}, {"Manutention", "Moreau"},
      {"Construction", "Petit"}, {"Génie", "Rousseau"}, {"Solutions", "Robert"},
      {"Industries", "Richard"}, {"Services", "Bernard"}, {"Travaux", "Garnier"},
      {"Constructions", "Lambert"}, {"Chantiers", "Bonnet"}, {"Ateliers", "Francois"},
      {"Group", "Legrand"}, {"Tech", "Gauthier"}, {"TP", "Lopez"},
      {"Infrastructures", "Fernandez"}, {"Matériaux", "Julien"}
  };
  private static final String[] CITIES = {
      "Lyon", "Villeurbanne", "Vénissieux", "Bron", "Meyzieu", "Givors",
      "Saint-Priest", "Caluire", "Tassin", "Oullins",
      "Paris", "Nanterre", "Saint-Denis", "Aubervilliers", "Montreuil", "Vitry",
      "Ivry", "Boulogne", "Versailles", "Argenteuil"
  };
  private static final String[] STREETS = {
      "rue des Forges", "avenue de la République", "boulevard des Frères Lumière",
      "rue Pasteur", "rue Victor-Hugo", "chemin du Canal", "rue des Frênes",
      "rue des Acacias", "rue de la Soie", "rue du Port"
  };
  private static final String[] COMPANY_NOTES = {
      "Client historique de la région lyonnaise.",
      "Sensibilité prix sur les prestations de levage.",
      "Prévoir relances pour factures en attente.",
      "Demande de prioriser les interventions urgentes.",
      "Intéressé par de nouvelles solutions logistiques."
  };
  private static final String[] CONTACT_ROLES = {
      "Directeur de chantier", "Responsable achats", "Chargé d'affaires",
      "Assistante commerciale", "Responsable sécurité", "Chef de projet"
  };
  private static final String[] FIRST_NAMES_FR = {
      "Jean", "Sophie", "Paul", "Emma", "Lucas", "Chloé", "Hugo", "Léa", "Louis", "Manon",
      "Nolan", "Camille", "Arthur", "Sarah", "Antoine", "Zoé", "Julien", "Inès", "Maxime", "Eva"
  };
  private static final String[] LAST_NAMES_FR = {
      "Martin", "Bernard", "Thomas", "Petit", "Robert", "Richard", "Durand", "Dubois", "Moreau", "Laurent",
      "Simon", "Michel", "Lefebvre", "Leroy", "Roux", "David", "Bertrand", "Morel", "Fournier", "Girard"
  };
  private static final String[] AGENCY_IDS = { "A1", "A2" };

  private final Map<UUID, Client> clients = new ConcurrentHashMap<>();
  private final Map<UUID, Map<UUID, Contact>> contacts = new ConcurrentHashMap<>();

  public MockClientService(){
    if (clients.isEmpty()){
      seedMany();
    }
  }

  private Map<UUID,Contact> ctMap(UUID clientId){
    return contacts.computeIfAbsent(clientId, k -> new ConcurrentHashMap<>());
  }

  @Override public List<Client> list(){
    ArrayList<Client> out = new ArrayList<>(clients.values());
    out.sort(Comparator.comparing(Client::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    return out;
  }

  @Override public Client get(UUID id){
    return clients.get(id);
  }

  @Override public Client save(Client c){
    if (c.getId()==null) c.setId(UUID.randomUUID());
    clients.put(c.getId(), c);
    contacts.computeIfAbsent(c.getId(), k -> new ConcurrentHashMap<>());
    return c;
  }

  @Override public void delete(UUID id){
    clients.remove(id);
    contacts.remove(id);
  }

  @Override public List<Contact> listContacts(UUID clientId){
    ArrayList<Contact> list = new ArrayList<>(ctMap(clientId).values());
    Comparator<String> stringComparator = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
    list.sort(Comparator
        .comparing(Contact::getLastName, stringComparator)
        .thenComparing(Contact::getFirstName, stringComparator));
    return list;
  }

  @Override public Contact saveContact(UUID clientId, Contact ct){
    if (ct.getId()==null) ct.setId(UUID.randomUUID());
    ct.setClientId(clientId);
    ctMap(clientId).put(ct.getId(), ct);
    return ct;
  }

  @Override public void deleteContact(UUID clientId, UUID contactId){
    ctMap(clientId).remove(contactId);
  }

  private void seedMany(){
    Random rnd = new Random(20240918L);
    for (int i=0;i<COMPANY_NAMES.length;i++){
      String[] parts = COMPANY_NAMES[i];
      String companyName = parts[0] + " " + parts[1];
      String code = "CLI" + twoDigits(i+1);
      String city = CITIES[i % CITIES.length];
      String addressLine = (1 + rnd.nextInt(120)) + " " + STREETS[rnd.nextInt(STREETS.length)];
      String postal = postalForCity(city, rnd);
      String address = addressLine + "\n" + postal + " " + city;
      String domain = domainFromName(companyName);

      Client client = new Client();
      client.setId(new UUID(rnd.nextLong(), rnd.nextLong()));
      client.setName(companyName);
      client.setCode(code);
      client.setEmail("contact@" + domain);
      client.setPhone(landline(rnd));
      client.setVatNumber("FR" + (100000000 + rnd.nextInt(900000000)));
      client.setBillingAddress(address);
      client.setShippingAddress(address);
      client.setNotes(pick(rnd, COMPANY_NOTES));
      if (AGENCY_IDS.length > 0){
        client.setAgencyId(AGENCY_IDS[i % AGENCY_IDS.length]);
      }
      clients.put(client.getId(), client);

      Map<UUID, Contact> map = ctMap(client.getId());
      int contactCount = 2 + rnd.nextInt(3);
      int primaryIndex = rnd.nextInt(contactCount);
      for (int k=0;k<contactCount;k++){
        Contact ct = new Contact();
        ct.setId(new UUID(rnd.nextLong(), rnd.nextLong()));
        ct.setClientId(client.getId());
        String firstName = pick(rnd, FIRST_NAMES_FR);
        String lastName = pick(rnd, LAST_NAMES_FR);
        ct.setFirstName(firstName);
        ct.setLastName(lastName);
        ct.setEmail((firstName + "." + lastName + "@" + domain).toLowerCase(Locale.ROOT));
        ct.setPhone(mobile(rnd));
        ct.setRole(pick(rnd, CONTACT_ROLES));
        boolean archived = k != primaryIndex && rnd.nextInt(10)==0;
        ct.setArchived(archived);
        setPrimary(ct, k==primaryIndex);
        map.put(ct.getId(), ct);
      }
    }
  }

  private static void setPrimary(Contact ct, boolean primary){
    try {
      Contact.class.getMethod("setPrimary", boolean.class).invoke(ct, primary);
    } catch (Exception ignore) {
      // ignored: le modèle peut ne pas exposer la notion de contact principal
    }
  }

  private static String twoDigits(int value){
    if (value < 10){
      return "0" + value;
    }
    if (value < 100){
      return Integer.toString(value);
    }
    return Integer.toString(value % 100);
  }

  private static String twoDigits(Random rnd){
    int v = rnd.nextInt(100);
    return v < 10 ? "0" + v : Integer.toString(v);
  }

  private static String postalForCity(String city, Random rnd){
    if (city == null || city.isBlank()){
      return "75000";
    }
    return switch (city) {
      case "Paris" -> "750" + twoDigits(rnd);
      case "Nanterre", "Boulogne", "Versailles" -> "920" + twoDigits(rnd);
      case "Saint-Denis", "Aubervilliers" -> "930" + twoDigits(rnd);
      case "Montreuil" -> "93100";
      case "Vitry" -> "94400";
      case "Ivry" -> "94200";
      case "Argenteuil" -> "95100";
      case "Lyon" -> "6900" + (1 + rnd.nextInt(9));
      case "Villeurbanne" -> "69100";
      case "Vénissieux" -> "69200";
      case "Bron" -> "69500";
      case "Meyzieu" -> "69330";
      case "Givors" -> "69700";
      case "Saint-Priest" -> "69800";
      case "Caluire" -> "69300";
      case "Tassin" -> "69160";
      case "Oullins" -> "69600";
      default -> "69000";
    };
  }

  private static String landline(Random rnd){
    int zone = 1 + rnd.nextInt(5);
    return "+33 " + zone + " " + phoneSegment(rnd) + " " + phoneSegment(rnd) + " " + phoneSegment(rnd) + " " + phoneSegment(rnd);
  }

  private static String mobile(Random rnd){
    return "+33 6 " + phoneSegment(rnd) + " " + phoneSegment(rnd) + " " + phoneSegment(rnd) + " " + phoneSegment(rnd);
  }

  private static String phoneSegment(Random rnd){
    int v = rnd.nextInt(100);
    return v < 10 ? "0" + v : Integer.toString(v);
  }

  private static String domainFromName(String name){
    String slug = slug(name);
    return (slug.isEmpty() ? "client" : slug) + ".fr";
  }

  private static String slug(String value){
    if (value == null){
      return "";
    }
    String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
    StringBuilder sb = new StringBuilder();
    for (int i=0;i<normalized.length();i++){
      char ch = normalized.charAt(i);
      if (Character.getType(ch) == Character.NON_SPACING_MARK){
        continue;
      }
      if (ch == 'œ' || ch == 'Œ'){
        sb.append("oe");
        continue;
      }
      if (ch == 'æ' || ch == 'Æ'){
        sb.append("ae");
        continue;
      }
      if (Character.isLetterOrDigit(ch)){
        sb.append(Character.toLowerCase(ch));
      }
    }
    return sb.toString();
  }

  private static <T> T pick(Random rnd, T[] values){
    return values[rnd.nextInt(values.length)];
  }
}
