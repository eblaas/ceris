package io.ceris.apicall.auth;

import io.ceris.Configuration;
import io.ceris.apicall.ApiCallError;
import io.ceris.apicall.Router;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Filter;
import spark.Request;
import spark.Response;

import java.util.Optional;

import static spark.Spark.halt;

public class AuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    public static final String USER_PRINCIPAL = "USER_PRINCIPAL";
    private static final UserPrincipal AUTH_DISABLED_PRINCIPAL = new UserPrincipal("admin", User.Role.ADMIN);

    private final UserService userService;
    private final boolean authEnabled;

    public AuthFilter(Configuration configuration, UserService userService) {
        this.authEnabled = configuration.isAuthEnabled();
        this.userService = userService;
        log.info("Authentication enabled={}", authEnabled);
    }

    public void handle(Request request, Response response) {

        if (authEnabled && isProtectedResource(request)) {
            Optional<UserPrincipal> userPrincipal = userService.authenticate(request);

            if (userPrincipal.isPresent()) {
                request.attribute(USER_PRINCIPAL, userPrincipal.get());
                if (!request.requestMethod().equalsIgnoreCase("GET") && userPrincipal.get().role() == User.Role.USER) {
                    throw new ApiCallError(HttpStatus.SC_FORBIDDEN, "Requires ADMIN role");
                }
            } else {
                halt(HttpStatus.SC_UNAUTHORIZED);
            }
        } else {
            request.attribute(USER_PRINCIPAL, AUTH_DISABLED_PRINCIPAL);
        }
    }

    private boolean isProtectedResource(Request request) {
        return request.uri().toLowerCase().contains(Router.ROUTE_CONNECT_API);
    }
}