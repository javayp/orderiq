# OrderIQ

OrderIQ is a Java 21/Spring Boot implementation of the customer-order ETL and
AI-augmented query exercise. It is one deployable microservice with two internal
Gradle modules so that data concerns and AI concerns can evolve independently
without introducing a network boundary inside this exercise.

## Current status

| Exercise area | Status |
| --- | --- |
| Part 1 — CSV ETL and SQLite load | Implemented and tested |
| Part 2 — REST query API | Next increment |
| Part 3 — Docker and Kubernetes | Planned, not implemented |
| Part 4 — NL query, semantic search, scaling write-up | Planned, not implemented |

## Project structure

```text
orderiq/
├── order-data/   # Order domain, CSV normalization, ETL orchestration, SQLite
├── order-ai/     # Spring Boot executable boundary; API and AI components go here
├── data/         # Supplied CSV input; generated SQLite files are ignored
└── docs/         # Architecture by responsibility
```

`order-ai` depends on `order-data` through Java interfaces. There is no internal
HTTP call and no second runtime service. The output remains one executable JAR
and, later, one container image.

## Run the implemented ETL

Prerequisite: Java 21. The Gradle wrapper is included.

```shell
./gradlew test
./gradlew :order-ai:bootRun --args='load data/orders.csv'
```

To choose another SQLite location:

```shell
ORDERIQ_DB_PATH=/absolute/path/orders.db \
  ./gradlew :order-ai:bootRun --args='load data/orders.csv'
```

The loader accepts multiple files and replaces the previous dataset atomically:

```shell
./gradlew :order-ai:bootRun --args='load data/orders-a.csv data/orders-b.csv'
```

## ETL behavior

- Accepts ISO dates plus `MM/DD/YYYY`, `DD-MM-YYYY`, and `YYYY/MM/DD` variants.
- Persists dates as ISO `YYYY-MM-DD` text so SQLite range queries are reliable.
- Converts EUR using the fixed rate `1 EUR = 1.10 USD`; USD remains unchanged.
- Rounds persisted USD amounts to two decimal places with `HALF_UP`.
- Defaults missing or non-numeric amounts to zero.
- Defaults missing currencies to USD.
- Drops rows missing `order_id` or `customer_id`.
- Drops impossible dates and unsupported currencies because no valid normalized
  order can be produced.
- Reports every default, rejection, and duplicate without aborting valid rows.
- Replaces all rows in one database transaction, avoiding partial datasets.

The supplied dataset was validated with these results:

```text
rows read:           5009
rows loaded:         4948
rows dropped:          61
amounts defaulted:     55
currencies defaulted:  30
SQLite integrity:      ok
date range:            2024-01-03 to 2027-12-30
total USD revenue:     2337365.30
```

## Architecture documentation

- [Data module](docs/data-module.md)
- [AI module](docs/ai-module.md)
- [Infrastructure and enterprise scaling](docs/infrastructure.md)

These documents distinguish the exercise implementation from the future
50-customer architecture. SQLite is appropriate for the submission scope; it
is not presented as the multi-region production database.
