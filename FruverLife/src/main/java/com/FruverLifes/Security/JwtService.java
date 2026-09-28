package com.FruverLifes.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    // Clave secreta para firmar los JWT
    @Value("${jwt.secret}")
    private String secretKey;

    // Generar JWT
    public String generarToken(String usuario, String cargo) {

        return Jwts.builder()
                .subject(usuario)
                .claim("cargo", cargo)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(getSignKey())
                .compact();
    }

    // Obtener usuario desde el token
    public String obtenerUsuario(String token) {

        return obtenerClaim(token, Claims::getSubject);
    }

    public String obtenerCargo(String token) {
        return obtenerClaim(token, claims -> claims.get("cargo", String.class));
    }

    // Obtener cualquier información del token
    public <T> T obtenerClaim(
            String token,
            Function<Claims, T> resolver
    ) {

        Claims claims = Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return resolver.apply(claims);
    }

    // Validar token
    public boolean esTokenValido(String token, String usuario) {

        try {

            String usuarioToken = obtenerUsuario(token);

            return usuarioToken.equals(usuario)
                    && !estaExpirado(token);

        } catch (Exception e) {

            return false;
        }
    }

    // Verificar expiración
    private boolean estaExpirado(String token) {

        Date expiracion = obtenerClaim(
                token,
                Claims::getExpiration
        );

        return expiracion.before(new Date());
    }

    // Crear clave para firmar
    private SecretKey getSignKey() {

        byte[] keyBytes = Decoders.BASE64.decode(secretKey);

        return Keys.hmacShaKeyFor(keyBytes);
    }
}