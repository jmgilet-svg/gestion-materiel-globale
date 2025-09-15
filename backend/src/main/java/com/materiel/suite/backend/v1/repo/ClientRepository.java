package com.materiel.suite.backend.v1.repo;

import com.materiel.suite.backend.v1.domain.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClientRepository extends JpaRepository<ClientEntity, UUID> { }

