package com.materiel.suite.client.service;

import java.util.List;

/** Gestion des templates HTML pour devis/factures/emails. */
public interface DocumentTemplateService {
  class Template {
    private String id;
    private String agencyId;
    private String type;
    private String key;
    private String name;
    private String content;

    public String getId(){
      return id;
    }

    public void setId(String id){
      this.id = id;
    }

    public String getAgencyId(){
      return agencyId;
    }

    public void setAgencyId(String agencyId){
      this.agencyId = agencyId;
    }

    public String getType(){
      return type;
    }

    public void setType(String type){
      this.type = type;
    }

    public String getKey(){
      return key;
    }

    public void setKey(String key){
      this.key = key;
    }

    public String getName(){
      return name;
    }

    public void setName(String name){
      this.name = name;
    }

    public String getContent(){
      return content;
    }

    public void setContent(String content){
      this.content = content;
    }
  }

  class Asset {
    private String id;
    private String agencyId;
    private String key;
    private String name;
    private String contentType;
    private String base64;

    public String getId(){
      return id;
    }

    public void setId(String id){
      this.id = id;
    }

    public String getAgencyId(){
      return agencyId;
    }

    public void setAgencyId(String agencyId){
      this.agencyId = agencyId;
    }

    public String getKey(){
      return key;
    }

    public void setKey(String key){
      this.key = key;
    }

    public String getName(){
      return name;
    }

    public void setName(String name){
      this.name = name;
    }

    public String getContentType(){
      return contentType;
    }

    public void setContentType(String contentType){
      this.contentType = contentType;
    }

    public String getBase64(){
      return base64;
    }

    public void setBase64(String base64){
      this.base64 = base64;
    }
  }

  List<Template> list(String type);

  Template save(Template template);

  void delete(String id);

  List<Asset> listAssets();

  Asset saveAsset(Asset asset);

  void deleteAsset(String id);
}
