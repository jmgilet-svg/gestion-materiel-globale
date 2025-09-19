package com.materiel.suite.backend.intervention.dto;

import java.time.Instant;

/** DTO simple pour représenter un événement d'historique d'intervention (API v2 mockée). */
public class TimelineEventV2Dto {
  private String id;
  private String interventionId;
  private Instant timestamp;
  private String type;
  private String message;
  private String author;

  public String getId(){
    return id;
  }

  public void setId(String id){
    this.id = id;
  }

  public String getInterventionId(){
    return interventionId;
  }

  public void setInterventionId(String interventionId){
    this.interventionId = interventionId;
  }

  public Instant getTimestamp(){
    return timestamp;
  }

  public void setTimestamp(Instant timestamp){
    this.timestamp = timestamp;
  }

  public String getType(){
    return type;
  }

  public void setType(String type){
    this.type = type;
  }

  public String getMessage(){
    return message;
  }

  public void setMessage(String message){
    this.message = message;
  }

  public String getAuthor(){
    return author;
  }

  public void setAuthor(String author){
    this.author = author;
  }
}
