## Database replication to S3

This example demonstrates how to replicate a source database (Postgres) to a S3 cloud bucket

Start containers:

```sh
docker-compose up
```

1. Ceris running at http://localhost:4567
2. PgAdmin running at http://localhost:6060
3. S3 UI running at http://localhost:9001 (minio:minio123)
4. Make changes in the source-db, e.g. table `inventory.customers`
5. New files in S3 should appear

Clean up:

```sh
docker-compose down
```