package com.materiel.suite.backend.v1.repo;

import com.materiel.suite.backend.v1.domain.DocSequenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface DocSequenceRepository extends JpaRepository<DocSequenceEntity, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from DocSequenceEntity s where s.type=:t and s.year=:y")
  Optional<DocSequenceEntity> findByTypeAndYearForUpdate(@Param("t") String type, @Param("y") int year);
}
