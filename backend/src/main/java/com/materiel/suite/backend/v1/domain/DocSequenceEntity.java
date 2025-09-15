package com.materiel.suite.backend.v1.domain;

import jakarta.persistence.*;

@Entity
@Table(name="doc_sequence")
public class DocSequenceEntity {
  @Id @GeneratedValue
  private Long id;
  @Column(nullable=false) private String type;
  @Column(nullable=false) private int year;
  @Column(nullable=false) private int counter;

  public Long getId(){ return id; }
  public void setId(Long id){ this.id=id; }
  public String getType(){ return type; }
  public void setType(String type){ this.type=type; }
  public int getYear(){ return year; }
  public void setYear(int year){ this.year=year; }
  public int getCounter(){ return counter; }
  public void setCounter(int counter){ this.counter=counter; }
}
