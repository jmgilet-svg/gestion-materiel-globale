package com.materiel.suite.backend.v1.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre ultra-léger : si Authorization=Bearer <token> présent, on interprète
 * - "admin" => rôle ADMIN
 * - tout autre => rôle USER
 * Si aucun token : mode permissif (USER). Peut être désactivé via env GM_AUTH_REQUIRED=true pour imposer un token.
 */
@Component
@Order(10)
public class AuthFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
    String auth = req.getHeader("Authorization");
    boolean required = Boolean.parseBoolean(System.getenv().getOrDefault("GM_AUTH_REQUIRED","false"));
    String role = "USER";
    if (auth!=null && auth.startsWith("Bearer ")){
      String tok = auth.substring(7);
      role = "admin".equals(tok) ? "ADMIN" : "USER";
    } else if (required){
      res.sendError(401, "Unauthorized");
      return;
    }
    req.setAttribute("gm.role", role);
    chain.doFilter(req, res);
  }
  public static boolean hasRole(HttpServletRequest req, String r){
    Object o = req.getAttribute("gm.role"); return r.equals(o);
  }
}
