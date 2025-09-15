package com.materiel.suite.backend.v1.repo;

import com.materiel.suite.backend.v1.domain.DeliveryNoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeliveryNoteRepository extends JpaRepository<DeliveryNoteEntity, UUID> {
}

