# Five-Minute Demo

This project is designed to be evaluated locally: PostgreSQL and Kafka run in Docker, while the engine dashboard and worker run as two Spring Boot processes.

## Start the system

Open three terminals from the repository root.

```bash
cd infra && docker compose up -d
```

```bash
cd engine && ./mvnw spring-boot:run
```

```bash
cd worker && ../engine/mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080). Wait until the dashboard shows **Engine UP**.

## What to show

1. Click **Register demo workflow**. It registers an order-fulfillment definition with five steps.
2. Select **Happy path** and click **Start workflow**. The pipeline should complete in roughly five seconds.
3. Select **Non-critical skip**. `reserve_inventory` is deliberately failed by the demo worker, appears as `SKIPPED`, and the workflow still completes.
4. Select **Retry then succeed**. `charge_payment` fails on its first attempt, enters `RETRYING`, then succeeds after the configured 30-second retry delay.

The dashboard shows the current step, status transitions, retry count, failure reason, and full context for every instance.

## One-command API demo

After the services are running, execute:

```bash
./scripts/demo.sh
```

It registers the same definition and runs the happy-path, skip, and retry scenarios through the REST API.

## Reset local data

Use this only when you want a clean local database:

```bash
cd infra && docker compose down -v
```

The named Docker volume is removed; source files are unaffected.
