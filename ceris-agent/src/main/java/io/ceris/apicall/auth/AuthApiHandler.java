package io.ceris.apicall.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ceris.apicall.ApiCallError;
import spark.Request;
import spark.Response;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.Map;

import static io.ceris.apicall.auth.AuthFilter.USER_PRINCIPAL;

public class AuthApiHandler {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    public AuthApiHandler(UserService userService, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.userService = userService;
    }

    public String login(Request request, Response response) throws IOException {
        try {
            JsonNode json = objectMapper.readTree(request.body());
            String username = json.get("username").asText();
            String password = json.get("password").asText();
            UserPrincipal user = userService.findUser(username, password).orElseThrow(NotFoundException::new);
            return objectMapper.writeValueAsString(Map.of("token", userService.newToken(user)));
        } catch (Exception e) {
            throw new ApiCallError(400, "Invalid credentials");
        }
    }

    public String me(Request request, Response response) throws IOException {
        return objectMapper.writeValueAsString(request.attribute(USER_PRINCIPAL));
    }
}
