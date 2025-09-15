package com.materiel.suite.backend.v1.api;

import com.materiel.suite.backend.v1.domain.ClientEntity;
import com.materiel.suite.backend.v1.domain.ContactEntity;
import com.materiel.suite.backend.v1.repo.ClientRepository;
import com.materiel.suite.backend.v1.repo.ContactRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class ClientsController {
  private final ClientRepository clients;
  private final ContactRepository contacts;
  public ClientsController(ClientRepository c, ContactRepository ct){ this.clients=c; this.contacts=ct; }

  /* Clients */
  @GetMapping("/clients")
  public List<ClientEntity> listClients(){ return clients.findAll(); }
  @GetMapping("/clients/{id}")
  public ClientEntity getClient(@PathVariable UUID id){ return clients.findById(id).orElseThrow(); }
  @PostMapping("/clients")
  public ClientEntity createClient(@RequestBody ClientEntity c){ if (c.getId()==null) c.setId(UUID.randomUUID()); return clients.save(c); }
  @PutMapping("/clients/{id}")
  public ClientEntity updateClient(@PathVariable UUID id, @RequestBody ClientEntity c){ c.setId(id); return clients.save(c); }
  @DeleteMapping("/clients/{id}")
  public ResponseEntity<Void> deleteClient(@PathVariable UUID id){ clients.deleteById(id); return ResponseEntity.noContent().build(); }

  /* Contacts (sous-ressource) */
  @GetMapping("/clients/{id}/contacts")
  public List<ContactEntity> listContacts(@PathVariable UUID id){
    ClientEntity c = clients.findById(id).orElseThrow();
    return contacts.findByClient(c);
  }
  @PostMapping("/clients/{id}/contacts")
  public ContactEntity createContact(@PathVariable UUID id, @RequestBody ContactEntity ct){
    ClientEntity c = clients.findById(id).orElseThrow();
    if (ct.getId()==null) ct.setId(UUID.randomUUID());
    ct.setClient(c);
    return contacts.save(ct);
  }
  @PutMapping("/clients/{id}/contacts/{contactId}")
  public ContactEntity updateContact(@PathVariable UUID id, @PathVariable UUID contactId, @RequestBody ContactEntity ct){
    ClientEntity c = clients.findById(id).orElseThrow();
    ct.setId(contactId); ct.setClient(c);
    return contacts.save(ct);
  }
  @DeleteMapping("/clients/{id}/contacts/{contactId}")
  public ResponseEntity<Void> deleteContact(@PathVariable UUID id, @PathVariable UUID contactId){
    contacts.deleteById(contactId); return ResponseEntity.noContent().build();
  }
}

