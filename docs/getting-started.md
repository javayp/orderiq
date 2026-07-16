# Getting started

[Back to README](../README.md)

This guide covers a complete local run from a fresh clone. Run commands from
the repository root so the ETL and API resolve the same database and model-cache
paths.

## Prerequisites

| Requirement | Version or purpose |
| --- | --- |
| Git | Clone the repository |
| JDK | Java 21 |
| OpenAI API key | Required when starting `order-ai` and calling `/orders/ask` |
| Internet access | Required for the first Gradle build and embedding-model download |

Gradle does not need to be installed. The repository includes Gradle Wrapper
9.5.1. Docker and Kubernetes are optional and documented in
[deployment](deployment.md).

## Terminal setup

### 1. Clone and verify Java

```shell
git clone https://github.com/javayp/orderiq.git
cd orderiq
java -version
./gradlew --version
```

Both version commands must use Java 21. Set `JAVA_HOME` to a Java 21 JDK if
another version appears.

### 2. Build and test

```shell
./gradlew clean test
```

This compiles both modules and runs the ETL, repository, API, guardrail, SQL,
retry, and semantic-search tests.

### 3. Load the supplied CSV

```shell
./gradlew :order-data:bootRun --args='load data/orders.csv'
```

The ETL creates `data/orders.db`, prints row-level issues and the final report,
then exits. The supplied file completes with:

```text
ETL complete: read=5009, loaded=4948, dropped=61, amount_defaults=55, currency_defaults=30
```

Running the command again atomically replaces the dataset; it does not append
duplicate rows.

To load multiple files:

```shell
./gradlew :order-data:bootRun \
  --args='load data/orders-a.csv data/orders-b.csv'
```

### 4. Configure the API

```shell
export OPENAI_API_KEY=your-api-key
```

Do not put the key in `application.yaml` or commit it to Git.

| Environment variable | Default | Purpose |
| --- | --- | --- |
| `OPENAI_MODEL` | `gpt-5.4-nano` | OpenAI model used for SQL planning |
| `ORDERIQ_DB_PATH` | `./data/orders.db` | SQLite database created by the ETL |
| `ORDERIQ_EMBEDDING_CACHE` | `./data/embedding-model-cache` | Transformer-model cache |
| `SERVER_PORT` | `8080` | Local API port |

If `ORDERIQ_DB_PATH` is overridden, use the same value for the ETL and API,
including in any separate terminal or IDE configuration:

```shell
export ORDERIQ_DB_PATH=/absolute/path/orders.db
./gradlew :order-data:bootRun --args='load data/orders.csv'
```

### 5. Start the API

```shell
./gradlew :order-ai:bootRun
```

Keep this terminal open. The first start downloads `all-MiniLM-L6-v2` and builds
the semantic index; later starts reuse the cached model.

### 6. Verify readiness

From another terminal:

```shell
curl --fail http://localhost:8080/healthz
curl --fail http://localhost:8080/readyz
curl http://localhost:8080/orders/stats
curl "http://localhost:8080/orders/semantic_search?q=high%20value%20recent%20orders&top_k=5"
curl -X POST http://localhost:8080/orders/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"What is the total revenue?"}'
```

`/healthz` confirms process liveness. `/readyz` returns `503` while the first
semantic index is building and `200` when every endpoint is ready. Stop the API
with `Ctrl+C`.

## Windows PowerShell

```powershell
.\gradlew.bat clean test
.\gradlew.bat :order-data:bootRun --args="load data/orders.csv"
$env:OPENAI_API_KEY="your-api-key"
.\gradlew.bat :order-ai:bootRun
```

## IntelliJ IDEA

1. Select **File → Open** and choose the repository root containing
   `settings.gradle`. Do not import the modules separately.
2. Import the project as Gradle and select **Gradle Wrapper** as the distribution.
3. Set both **Project SDK** and **Gradle JVM** to Java 21, then allow Gradle sync
   to finish.
4. If IntelliJ reports missing Lombok-generated constructors, enable the Lombok
   plugin and annotation processing.
5. Create an Application configuration named `OrderIQ ETL`:
   - Main class: `com.orderiq.data.OrderDataApplication`
   - Module classpath: `order-data.main`
   - Program arguments: `load data/orders.csv`
   - Working directory: repository root
6. Run `OrderIQ ETL`; the process exits after printing its report.
7. Create an Application configuration named `OrderIQ API`:
   - Main class: `com.orderiq.OrderiqApplication`
   - Module classpath: `order-ai.main`
   - Working directory: repository root
   - Environment variable: `OPENAI_API_KEY=your-api-key`
8. Run `OrderIQ API` and wait for `/readyz` to return `200`.

## Common startup problems

| Symptom | Check |
| --- | --- |
| Application cannot resolve `OPENAI_API_KEY` | Set the variable in the same shell or IntelliJ run configuration that starts `order-ai` |
| `/readyz` returns `503` | Wait for the first MiniLM download and index build; inspect application logs |
| API reads an empty or different database | Run the ETL first and confirm both processes use the same working directory and `ORDERIQ_DB_PATH` |
| Port 8080 is busy | Set `SERVER_PORT` to another port and use it in the verification URLs |
| IntelliJ cannot resolve generated constructors | Enable Lombok and annotation processing, then refresh Gradle |

See the [API reference](api-reference.md) for every endpoint and
[deployment](deployment.md) for Docker and Kubernetes.
