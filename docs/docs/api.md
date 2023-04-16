# Ceris REST API

## Authentication

Supported authentication methods:

* Basic auth (`Authorization:Basic <base64>`)
* Bearer token (`Authorization:Bearer <token>`)

## Content Types

The REST API only supports `application/json` as both the request and response content type.

## Open Endpoints

Open endpoints require no authentication.

* Login : `POST /auth/login/`
* Health : `GET /health`

## Endpoints that require authentication

Closed endpoints require a valid token to be included in the header of the request. A token can be acquired from the
login endpoint above.

### Current User related

Information related to the current user whose token is provided with the request:

* Me : `GET /api/connect/me`

### Extension to kafka connect api

Additional endpoints to
the [connect API](https://docs.confluent.io/platform/current/connect/references/restapi.html#connectors) for viewing and
manipulating connectors, plugins, messages

* List defined secrets : `GET /api/connect/secrets`
* List connector topics : `GET /api/connect/topics`
* Get topic schema : `GET /api/connect/topics/:topic/schema`
* Get topic messages : `GET /api/connect/topics/:topic/messages`
* List installed plugins : `GET /api/connect/connector-plugins`
* Install new plugin : `POST /api/connect/connector-plugins`
* Uninstall plugin : `DELETE /api/connect/connector-plugins/:id`
* List installable plugins : `GET /api/connect/plugins-store`
* Proxy to
  kafka [connect API](https://docs.confluent.io/platform/current/connect/references/restapi.html#connectors) : `GET|POST|PUT|DELETE /api/connect/*`

### Monitoring endpoints

* Embedded server status : `GET /api/connect/status`
* Connector metrics : `GET /api/connect/metrics`
* Connector metrics prometheus : `GET /api/connect/prometheus`
