package com.materiel.suite.client.service;

import com.materiel.suite.client.model.ResourceType;

import java.util.List;

public interface ResourceTypeService {
  List<ResourceType> listAll();
  ResourceType save(ResourceType type);
  void delete(String id);
}
