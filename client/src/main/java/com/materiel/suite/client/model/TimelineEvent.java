package com.materiel.suite.client.model;

import java.time.Instant;

/** Représente un événement affiché dans l'historique d'une intervention. */
public class TimelineEvent {
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
