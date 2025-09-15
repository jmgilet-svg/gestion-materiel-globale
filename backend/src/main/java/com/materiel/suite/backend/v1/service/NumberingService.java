package com.materiel.suite.backend.v1.service;

import com.materiel.suite.backend.v1.repo.DocSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Service
public class NumberingService {
  private final DocSequenceRepository repo;
  public NumberingService(DocSequenceRepository repo){ this.repo=repo; }

  @Transactional
  public String next(String type){
    int year = Year.now().getValue();
    var seq = repo.findByTypeAndYearForUpdate(type, year).orElseGet(() -> {
      var n = new com.materiel.suite.backend.v1.domain.DocSequenceEntity();
      n.setType(type); n.setYear(year); n.setCounter(0);
      return repo.saveAndFlush(n);
    });
    int next = seq.getCounter()+1;
    seq.setCounter(next);
    repo.save(seq);
    return switch (type){
      case "QUOTE" -> String.format("Q%d-%04d", year, next);
      case "ORDER" -> String.format("BC%d-%04d", year, next);
      case "DELIVERY" -> String.format("BL%d-%04d", year, next);
      case "INVOICE" -> String.format("FA%d-%04d", year, next);
      default -> String.format("%s-%d-%04d", type, year, next);
    };
  }
}
