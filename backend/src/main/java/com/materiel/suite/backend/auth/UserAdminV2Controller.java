package com.materiel.suite.backend.auth;

import com.materiel.suite.backend.auth.dto.PasswordUpdateRequest;
import com.materiel.suite.backend.auth.dto.UserCreateRequest;
import com.materiel.suite.backend.auth.dto.UserV2Dto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/users")
public class UserAdminV2Controller {
  private final AuthCatalogService service;

  public UserAdminV2Controller(AuthCatalogService service){
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<List<UserV2Dto>> list(){
    return ResponseEntity.ok(service.listUsers());
  }

  @PostMapping
  public ResponseEntity<UserV2Dto> create(@RequestBody UserCreateRequest request){
    return ResponseEntity.ok(service.createUser(request));
  }

  @PutMapping("/{id}")
  public ResponseEntity<UserV2Dto> update(@PathVariable String id, @RequestBody UserV2Dto body){
    return ResponseEntity.ok(service.updateUser(id, body));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id){
    service.deleteUser(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/password")
  public ResponseEntity<Void> updatePassword(@PathVariable String id, @RequestBody PasswordUpdateRequest request){
    service.updatePassword(id, request != null ? request.getNewPassword() : null);
    return ResponseEntity.noContent().build();
  }
}
