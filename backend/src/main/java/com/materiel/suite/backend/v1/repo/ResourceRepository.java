package com.materiel.suite.backend.v1.repo;

import com.materiel.suite.backend.v1.domain.ResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ResourceRepository extends JpaRepository<ResourceEntity, UUID> { }
