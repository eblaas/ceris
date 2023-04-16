package io.ceris.apicall.auth;

import io.ceris.Configuration;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

import java.time.Duration;
import java.util.*;

public final class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private static final String ROLE_CLAIM = "role";

    private final String jwtSecretKey;
    private final Duration jwtExpiration;
    private final List<User> users;

    public UserService(Configuration configuration) {
        this.jwtSecretKey = configuration.get("CERIS_AUTH_JWT_SECRET_KEY");
        this.jwtExpiration = Duration.parse(configuration.get("CERIS_AUTH_JWT_EXPIRATION"));
        this.users = User.parse(configuration.get("CERIS_AUTH_USERS"));
    }

    public Optional<UserPrincipal> findUser(String username, String password) {
        return users.stream()
                .filter(user -> Objects.equals(user.username(), username))
                .filter(user -> Objects.equals(user.password(), DigestUtils.sha256Hex(password)))
                .findAny()
                .map(UserPrincipal::new);
    }

    public String newToken(UserPrincipal user) {
        DefaultClaims claims = new DefaultClaims();
        claims.put(ROLE_CLAIM, user.role().name());
        claims.setSubject(user.username());
        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration.toMillis()))
                .signWith(SignatureAlgorithm.HS512, jwtSecretKey)
                .compact();
    }

    public Optional<UserPrincipal> authenticate(Request request) {

        if (!request.headers().contains(HttpHeaders.AUTHORIZATION)) {
            return Optional.empty();
        }

        try {
            String authorizationHeader = request.headers(HttpHeaders.AUTHORIZATION);
            String[] typeAndValue = authorizationHeader.split(" ");

            if (typeAndValue.length == 2) {

                String type = typeAndValue[0];
                String value = typeAndValue[1];

                if ("Basic".equalsIgnoreCase(type)) {
                    return getUserPrincipalFromBasic(value);
                } else if ("Bearer".equalsIgnoreCase(type)) {
                    return getUserPrincipalFromToken(value);
                }
            }
        } catch (Exception e) {
            log.error("Auth failed", e);
        }
        return Optional.empty();
    }

    private Optional<UserPrincipal> getUserPrincipalFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecretKey)
                    .parseClaimsJws(token)
                    .getBody();
            String role = claims.get(ROLE_CLAIM, String.class);
            return Optional.of(new UserPrincipal(claims.getSubject(), User.Role.valueOf(role)));
        } catch (Exception e) {
            log.warn("Token validation failed. {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<UserPrincipal> getUserPrincipalFromBasic(String encodedHeader) {

        String decodedHeader = new String(Base64.getDecoder().decode(encodedHeader));
        String[] usernamePassword = decodedHeader.split(":");

        return usernamePassword.length == 2
                ? findUser(usernamePassword[0], usernamePassword[1])
                : Optional.empty();
    }

}