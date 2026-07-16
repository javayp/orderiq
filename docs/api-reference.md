# API reference

[Back to README](../README.md)

The local base URL is `http://localhost:8080`; the container exposes port
`8000`. JSON fields use snake case. The OpenAI key is configured on the server
and is never sent by an API caller.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/healthz` | Process liveness |
| `GET` | `/readyz` | Semantic-index readiness |
| `GET` | `/orders/customer/{customer_id}` | Exact orders for one customer |
| `GET` | `/orders/stats` | Revenue, average value, and daily order counts |
| `GET` | `/orders/recent?days=N` | Orders in a recent UTC date window |
| `POST` | `/orders/ask` | Natural-language question translated to read-only SQL |
| `GET` | `/orders/semantic_search?q=...&top_k=N` | Orders ranked by semantic similarity |

## `GET /healthz`

Accepts no parameters and confirms that the API process is alive.

```shell
curl -i http://localhost:8080/healthz
```

```http
HTTP/1.1 200 OK
Content-Type: text/plain

ok
```

## `GET /readyz`

Accepts no parameters and reports whether the first semantic index is ready.

```shell
curl -i http://localhost:8080/readyz
```

```http
HTTP/1.1 200 OK
Content-Type: text/plain

ready
```

During startup it returns `503 Service Unavailable` with `not ready`. Exact
order and statistics APIs may already be usable, but semantic traffic should
wait for `200`.

## `GET /orders/customer/{customer_id}`

Returns every normalized order for one exact customer ID, ordered by date and
then order ID.

| Input | Location | Rules |
| --- | --- | --- |
| `customer_id` | Path | Required, non-blank, maximum 100 characters |

```shell
curl http://localhost:8080/orders/customer/C001
```

Example `200 OK` response; values depend on the loaded CSV:

```json
[
  {
    "order_id": "1001",
    "customer_id": "C001",
    "order_date": "2024-03-15",
    "amount_usd": 320.00
  }
]
```

An unknown customer returns `200 OK` with `[]`.

## `GET /orders/stats`

Accepts no parameters and calculates statistics across normalized orders.

```shell
curl http://localhost:8080/orders/stats
```

```json
{
  "total_revenue": 12345.67,
  "avg_order_value": 87.50,
  "orders_per_day": {
    "2024-03-15": 15,
    "2024-03-16": 20
  }
}
```

`orders_per_day` has one key per order date and is ordered by date. An empty
dataset returns zero revenue and average with an empty daily map.

## `GET /orders/recent?days=N`

Returns orders from an inclusive UTC window ending today. `days=11` means today
and the previous ten days; future-dated orders are excluded.

| Input | Location | Rules |
| --- | --- | --- |
| `days` | Query | Required integer greater than zero |

```shell
curl "http://localhost:8080/orders/recent?days=11"
```

```json
[
  {
    "order_id": "1055",
    "customer_id": "C001",
    "order_date": "2026-07-15",
    "amount_usd": 125.75
  }
]
```

Results are ordered by date descending and then order ID. No matches return
`[]`. Missing, non-integer, or non-positive `days` values return `400`.

## `POST /orders/ask`

Accepts one natural-language order question. The service applies deterministic
guardrails, asks the configured model for a read-only SQLite query, validates
and executes it, and retries once if validation or execution fails.

| Input | Location | Rules |
| --- | --- | --- |
| `Content-Type` | Header | Must be `application/json` |
| `question` | JSON body | Required, non-blank, maximum 1,000 characters |

```shell
curl -X POST http://localhost:8080/orders/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"What is the total revenue from customer C001 in the last 30 days?"}'
```

Example `200 OK` response:

```json
{
  "answer": "Total revenue: 4230.0, Order count: 3",
  "sql_used": "SELECT ROUND(SUM(amount_usd), 2) AS total_revenue, COUNT(*) AS order_count FROM orders WHERE customer_id = 'C001' AND order_date BETWEEN date('now', '-29 days') AND date('now')",
  "rows": [
    {
      "total_revenue": 4230.0,
      "order_count": 3
    }
  ]
}
```

- `answer` is formatted deterministically from the returned rows, without a
  second LLM call.
- `sql_used` is the validated SQL that succeeded, including corrected SQL when
  the retry was used.
- `rows` contains the database result. Its columns depend on generated aliases,
  so aggregate and list questions have different row shapes.

Unsupported-schema, unsafe, multiple-intent, and out-of-domain questions return
`400`. If both SQL attempts fail, the endpoint returns `502 Bad Gateway`.

## `GET /orders/semantic_search`

Ranks orders by cosine similarity between the query embedding and in-memory
order embeddings. Use it for fuzzy descriptions such as “high value recent
orders.” Use `/orders/ask` for exact IDs, numeric comparisons, and date filters.

| Input | Location | Rules |
| --- | --- | --- |
| `q` | Query | Required, non-blank, maximum 500 characters |
| `top_k` | Query | Optional integer from 1 to 50; default is 5 |

```shell
curl "http://localhost:8080/orders/semantic_search?q=high%20value%20recent%20orders&top_k=5"
```

Example `200 OK` response; rankings depend on the current index:

```json
[
  {
    "order_id": "1055",
    "customer_id": "C001",
    "amount_usd": 1250.00,
    "order_date": "2026-07-15",
    "score": 0.812345
  }
]
```

The response may contain fewer than `top_k` records because matches below the
configured similarity threshold are removed. Invalid or out-of-domain input
returns `400`; an unavailable index returns `503`.

## Error response format

Handled JSON errors use Spring `ProblemDetail` and contain a status, title, and
explanation:

```json
{
  "title": "Invalid order query",
  "status": 400,
  "detail": "days must be greater than zero"
}
```

Guardrail rejections also include a decision:

```json
{
  "title": "Order question rejected",
  "status": 400,
  "detail": "No supported order-domain concept was recognized.",
  "decision": "OUT_OF_DOMAIN"
}
```

| Status | Meaning |
| --- | --- |
| `400` | Invalid parameters, unsupported schema, or guardrail rejection |
| `502` | Initial and corrected SQL both failed validation or execution |
| `503` | Semantic index or embedding operation is unavailable |
