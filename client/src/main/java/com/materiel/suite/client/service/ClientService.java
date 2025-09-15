package com.materiel.suite.client.service;

import com.materiel.suite.client.model.Client;
import com.materiel.suite.client.model.Contact;

import java.util.List;
import java.util.UUID;

public interface ClientService {
  List<Client> list();
  Client get(UUID id);
  Client save(Client c);
  void delete(UUID id);

  List<Contact> listContacts(UUID clientId);
  Contact saveContact(UUID clientId, Contact ct);
  void deleteContact(UUID clientId, UUID contactId);
}

