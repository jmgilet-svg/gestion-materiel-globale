package com.materiel.suite.backend.auth;

import com.materiel.suite.backend.auth.dto.AgencyV2Dto;
import com.materiel.suite.backend.auth.dto.LoginV2Request;
import com.materiel.suite.backend.auth.dto.UserV2Dto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AuthV2Controller {
  private final AuthCatalogService service;

  public AuthV2Controller(AuthCatalogService service){
    this.service = service;
  }

  @GetMapping("/api/v2/agencies")
  public ResponseEntity<List<AgencyV2Dto>> agencies(){
    return ResponseEntity.ok(service.listAgencies());
  }

  @PostMapping("/api/v2/auth/login")
  public ResponseEntity<UserV2Dto> login(@RequestBody LoginV2Request body){
    return service.login(body)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.status(401).build());
  }
}
