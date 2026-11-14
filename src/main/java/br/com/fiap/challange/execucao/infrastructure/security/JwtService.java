package br.com.fiap.challange.execucao.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;

/**
 * O Execution Service não emite token: só valida o JWT emitido pelo OS Service
 * (funcionário) ou pela function de CPF (cliente). Todos assinam com o
 * mesmo segredo, guardado no Secret Manager.
 */
@Service
public class JwtService {

    private final SecretKey chave;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.chave = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    }

    /** Lança exceção se o token for inválido ou estiver expirado. */
    public Claims validar(String token) {
        return Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
    }
}
