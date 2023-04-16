package io.ceris.apicall.auth;

public record UserPrincipal(String username, User.Role role) {
    UserPrincipal(User user) {
        this(user.username(), user.role());
    }
}
