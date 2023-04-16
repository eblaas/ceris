# Configuration

Ceris can be configured with:

* java system properties e.g. `-Dcersi.demo=true`
* env variables e.g. `CERIS_DEME=true`

## Authentication

Ceris users and roles can be defined via the configuration `CERIS_AUTH_USERS`.

The following format is supported: `<username>:<password>:<ADMIN|USER>,...`

`ADMIN` has read/write access, `USER` read only access

| Variable  | Default | Description                   |
|-----------|-------|-------------------------------|
| `CERIS_AUTH_ENABLED` | true  | Enable authentication / login |
| `CERIS_AUTH_USERS` | admin:admin:ADMIN,<br/>user:user:USER | Default users and there roles |
| `CERIS_AUTH_JWT_SECRET_KEY` | -     | Key for sighning JWT tokens   |
| `CERIS_AUTH_JWT_EXPIRATION` | PT24H | JWT token ttl                 |

## Embedded Kafka Configuration

| Variable                      | Default                    | Description                                                                                                                                                                 |
|-------------------------------|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `CERIS_EMBEDDED_ENABLED`      | true                       | Run ceris with embedded kafka connect                                                                                                                                       |
| `CERIS_EMBEDDED_DATA_PATH`      | data<br/>(relative to workDir) | Path location for persisting data. (kafka log, connect plugins)                                                                                                             |
| `CERIS_EMBEDDED_PLUGINS_INSTALL` |                            | comma sep. list of plugins to install at start-up                                                                                                                           |
| `CONNECT_*`                     |                            | Kafka connect worker properties e.g. CONNECT_GROUP_ID [doc](https://docs.confluent.io/platform/current/connect/references/allconfigs.html#distributed-worker-configuration) |
| `SCHEMA_REGISTRY_*`             |                            | Schema registry propterties                                                                                                                                                 |
| `KAFKA_*`              |                            | Kafka client propterties used for managing topics                                                                                                                           |
| `CERIS_API_PORT`           | 4567                       | Ceris API port                                                                                                                                                              |
| `CERIS_INIT_RESOURCES`           |                            | JSON configuration of connectors created at startup                                                                                                                         |

## Secrets

You can use it to prevent secrets from appearing in cleartext in connector configurations. Secrets are never persisted
in connector configs, logs, or in REST API requests and responses.

Secrets are defined with the prefix `CERIS_SECRET_` and can be referenced in configurations
with `${env:CERIS_SECRET_X_Y}`

| Pre-defined secrets                    | Placeholder in configuration                | Description            |
|----------------------------------------|---------------------------------------------|------------------------|
| `CERIS_SECRET_KAFKA_BOOTSTRAP_SERVERS` | `${env:CERIS_SECRET_KAFKA_BOOTSTRAP_SERVERS}` | Kafka bootstrap server |
| `CERIS_SECRET_SCHEMA_REGISTRY_URL`     | `${env:CERIS_SECRET_SCHEMA_REGISTRY_URL}`                | Schema registry url    |