package com.materiel.suite.backend.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TemplateControllerTest {

  private final TemplateController controller = new TemplateController();

  @BeforeEach
  void clearBefore(){
    clearStore();
  }

  @AfterEach
  void clearAfter(){
    clearStore();
  }

  @Test
  void listIsEmptyByDefault(){
    List<TemplateController.TemplateDto> all = controller.list(null, null);
    assertTrue(all.isEmpty(), "store should start empty");
  }

  @Test
  void upsertStoresTemplateForAgencyAndType(){
    TemplateController.TemplateDto input = new TemplateController.TemplateDto(
        null,
        "agency-A",
        TemplateController.TemplateType.QUOTE,
        "default",
        "Devis par défaut",
        "<h1>Quote</h1>"
    );

    TemplateController.TemplateDto saved = controller.upsert("agency-A", input);

    assertNotNull(saved.id(), "id generated");
    assertEquals("agency-A", saved.agencyId(), "agency resolved");

    List<TemplateController.TemplateDto> sameAgency = controller.list("agency-A", TemplateController.TemplateType.QUOTE);
    assertEquals(1, sameAgency.size(), "template visible for matching agency/type");

    List<TemplateController.TemplateDto> otherAgency = controller.list("agency-B", TemplateController.TemplateType.QUOTE);
    assertTrue(otherAgency.isEmpty(), "other agencies should not see template");

    controller.delete(saved.id());

    assertTrue(controller.list("agency-A", TemplateController.TemplateType.QUOTE).isEmpty(), "template removed after delete");
  }

  @Test
  void upsertUsesHeaderAgencyWhenBodyMissing(){
    TemplateController.TemplateDto input = new TemplateController.TemplateDto(
        null,
        null,
        TemplateController.TemplateType.EMAIL,
        "newsletter",
        "Newsletter",
        "<h1>Bonjour</h1>"
    );

    TemplateController.TemplateDto saved = controller.upsert("agency-Z", input);
    assertEquals("agency-Z", saved.agencyId(), "agency comes from header when body missing");
  }

  private void clearStore(){
    controller.list(null, null).forEach(t -> {
      if (t != null && t.id() != null){
        controller.delete(t.id());
      }
    });
  }
}
