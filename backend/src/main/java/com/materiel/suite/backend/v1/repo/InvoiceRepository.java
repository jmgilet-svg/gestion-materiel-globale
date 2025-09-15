package com.materiel.suite.backend.v1.repo;

import com.materiel.suite.backend.v1.domain.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID> {
}

