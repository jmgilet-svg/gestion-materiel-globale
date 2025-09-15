package com.materiel.suite.backend.v1.repo;

import com.materiel.suite.backend.v1.domain.ContactEntity;
import com.materiel.suite.backend.v1.domain.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContactRepository extends JpaRepository<ContactEntity, UUID> {
  List<ContactEntity> findByClient(ClientEntity client);
}

