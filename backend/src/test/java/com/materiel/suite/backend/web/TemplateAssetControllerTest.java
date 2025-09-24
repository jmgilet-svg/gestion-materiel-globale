package com.materiel.suite.backend.web;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TemplateAssetControllerTest {

  private final TemplateAssetController controller = new TemplateAssetController();

  @Test
  void canCreateListAndDeleteAssets(){
    TemplateAssetController.AssetDto saved = controller.upsert("agency-1",
        new TemplateAssetController.AssetDto(null, null, "logo", "Logo", "image/png", "abc"));

    assertNotNull(saved.id());
    assertEquals("agency-1", saved.agencyId());

    List<TemplateAssetController.AssetDto> list = controller.list("agency-1");
    assertEquals(1, list.size());
    assertEquals("logo", list.get(0).key());

    controller.delete(saved.id());

    assertTrue(controller.list("agency-1").isEmpty());
  }
}
