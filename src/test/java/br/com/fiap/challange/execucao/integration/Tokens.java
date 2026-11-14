package br.com.fiap.challange.execucao.integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.util.Base64;
import java.util.Date;
import java.util.Map;

final class Tokens {

    static final String SEGREDO = "dGVzdFNlY3JldEtleUZvclRlc3RpbmdQdXJwb3Nlc09maWNpbmExMjM0NQ==";

    private Tokens() {
    }

    static String mecanico() {
        return gerar("joao.mecanico", "FUNCIONARIO");
    }

    static String cliente() {
        return gerar("52998224725", "CLIENTE");
    }

    static String admin() {
        return gerar("admin", "ADMIN");
    }

    private static String gerar(String subject, String role) {
        return "Bearer " + Jwts.builder()
                .claims(Map.of("role", role))
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SEGREDO)))
                .compact();
    }
}
