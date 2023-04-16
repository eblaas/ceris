package io.ceris.apicall;

import org.json.JSONObject;

public class ApiCallError extends RuntimeException {

    private final int status;
    private final String message;

    public ApiCallError(int status, String msg) {
        this.status = status;
        this.message = msg;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String toJson() {
        return new JSONObject().put("error_code", status).put("message", message).toString();
    }
}
