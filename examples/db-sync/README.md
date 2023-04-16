## Database replication with CDC

This example demonstrates how to replicate a source database (Postgres) to a destination database (Postgres) via CDC

Start containers:

```sh
docker-compose up
```

1. Ceris running at http://localhost:4567
2. PgAdmin running at http://localhost:6060
3. Make changes in the source-db, e.g. table `inventory.customers`
4. Changes are replicated to destination-db

Clean up:

```sh
docker-compose down
```