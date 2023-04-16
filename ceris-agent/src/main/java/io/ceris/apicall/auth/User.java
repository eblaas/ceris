package io.ceris.apicall.auth;


import org.apache.commons.codec.digest.DigestUtils;

import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

record User(String username, String password, User.Role role) {

    public enum Role {ADMIN, USER}

    public static List<User> parse(String usersString) {
        return Arrays.stream(usersString.split(","))
                .map(user -> user.split(":"))
                .peek(parts -> checkArgument(parts.length == 3, "Illegal format. <user>:<pass>:<role>"))
                .map(parts -> new User(parts[0], DigestUtils.sha256Hex(parts[1]), Role.valueOf(parts[2].toUpperCase())))
                .toList();
    }
}
