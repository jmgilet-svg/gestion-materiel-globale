package com.materiel.suite.backend.v1.repo;

import com.materiel.suite.backend.v1.domain.InterventionEntity;
import com.materiel.suite.backend.v1.domain.ResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface InterventionRepository extends JpaRepository<InterventionEntity, UUID> {
  @Query("select i from InterventionEntity i where (i.startDateTime<=:to and i.endDateTime>=:from)")
  List<InterventionEntity> overlap(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
  @Query("select i from InterventionEntity i where i.resource=:res and (i.startDateTime<=:to and i.endDateTime>=:from)")
  List<InterventionEntity> overlapByResource(@Param("res") ResourceEntity res, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
