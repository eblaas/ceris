package io.ceris.apicall.auth;

import io.ceris.apicall.Router;
import spark.Filter;
import spark.Request;
import spark.Response;

import static spark.Spark.halt;

public class CorsFilter implements Filter {


    public void handle(Request request, Response response) throws InterruptedException {
        response.header("Access-Control-Allow-Origin", "*");
        response.header("Access-Control-Allow-Headers", "*");
        response.header("Access-Control-Allow-Methods", "*");
        if ("OPTIONS".equalsIgnoreCase(request.requestMethod())) {
            halt(200);
        }
    }

    private boolean isProtectedResource(Request request) {
        return request.uri().toLowerCase().contains(Router.ROUTE_CONNECT_API);
    }
}