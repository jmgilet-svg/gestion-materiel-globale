package com.materiel.suite.client.service.mock;

import com.materiel.suite.client.model.Client;
import com.materiel.suite.client.model.Contact;
import com.materiel.suite.client.service.ClientService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MockClientService implements ClientService {
  private final Map<UUID, Client> clients = new ConcurrentHashMap<>();
  private final Map<UUID, Map<UUID, Contact>> contacts = new ConcurrentHashMap<>();

  public MockClientService(){
    if (clients.isEmpty()){
      Client c1 = new Client(); c1.setId(UUID.randomUUID()); c1.setName("ACME Bâtiment"); c1.setEmail("contact@acme.tld"); c1.setPhone("+33 1 23 45 67 89");
      Client c2 = new Client(); c2.setId(UUID.randomUUID()); c2.setName("Société MARTIN"); c2.setEmail("info@martin.fr");
      clients.put(c1.getId(), c1); clients.put(c2.getId(), c2);
      Contact k = new Contact(); k.setId(UUID.randomUUID()); k.setClientId(c1.getId()); k.setFirstName("Karim"); k.setLastName("Azoulay"); k.setEmail("karim@acme.tld"); k.setPhone("+33 6 12 34 56 78"); put(ctMap(c1.getId()), k);
    }
  }
  private Map<UUID,Contact> ctMap(UUID clientId){ return contacts.computeIfAbsent(clientId, k -> new ConcurrentHashMap<>()); }
  private void put(Map<UUID,Contact> m, Contact c){ m.put(c.getId(), c); }

  @Override public List<Client> list(){ return new ArrayList<>(clients.values()); }
  @Override public Client get(UUID id){ return clients.get(id); }
  @Override public Client save(Client c){
    if (c.getId()==null) c.setId(UUID.randomUUID());
    clients.put(c.getId(), c); return c;
  }
  @Override public void delete(UUID id){ clients.remove(id); contacts.remove(id); }

  @Override public List<Contact> listContacts(UUID clientId){
    return new ArrayList<>(ctMap(clientId).values());
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
}

