# Docker and Kubernetes deployment

[Back to README](../README.md)

The same multi-stage image contains the ETL and API executable JARs. The default
entry point starts the API; Docker or the Kubernetes Job overrides the command
to run the finite ETL process.

## Container contract

| Property | Value |
| --- | --- |
| Runtime | Java 21 JRE |
| User | Non-root UID/GID `10001` |
| HTTP port | `8000` |
| Database | `/data/orders.db` |
| Model cache | `/data/embedding-model-cache` |
| Health check | `GET /healthz` |
| Persistent mount | `/data` |

## Docker

Build the image:

```shell
docker build -t orderiq:local .
docker volume create orderiq-data
```

Load the supplied CSV into the persistent volume:

```shell
docker run --rm \
  --entrypoint java \
  -v orderiq-data:/data \
  orderiq:local \
  -jar /app/order-data.jar load /app/data/orders.csv
```

Start the API with the same volume:

```shell
docker run --rm \
  -p 8000:8000 \
  --env OPENAI_API_KEY \
  -v orderiq-data:/data \
  orderiq:local
```

The first start downloads `all-MiniLM-L6-v2` into the persistent volume. Wait
for readiness before sending semantic requests:

```shell
curl --fail http://localhost:8000/healthz
curl --fail http://localhost:8000/readyz
```

## Kubernetes resources

| Manifest | Responsibility |
| --- | --- |
| `configmap.yaml` | Database path, embedding cache, model ID, and server port |
| `persistent-volume-claim.yaml` | Shared SQLite and model-cache storage |
| `etl-job.yaml` | Finite CSV load using `order-data.jar` |
| `deployment.yaml` | One `order-ai` API replica with probes and resource limits |
| `service.yaml` | Internal `ClusterIP` service on port 8000 |
| `secret.example.yaml` | Documents the required Secret shape without a real key |

### Why one replica

The exercise uses SQLite on one persistent volume. The Deployment therefore
uses one replica and a `Recreate` strategy instead of claiming unsafe horizontal
scaling over a shared file. The enterprise design replaces the persistence
technology before increasing API replicas.

### Deploy

Make `orderiq:local` available to the cluster by loading it into a local cluster
or pushing it to an accessible registry. Create the real Secret outside source
control:

```shell
kubectl create secret generic orderiq-secrets \
  --from-literal=openai-api-key="$OPENAI_API_KEY"
```

Apply resources in dependency order:

```shell
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/persistent-volume-claim.yaml
kubectl apply -f k8s/etl-job.yaml
kubectl wait --for=condition=complete job/orderiq-etl --timeout=120s
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

Verify the workload:

```shell
kubectl get job orderiq-etl
kubectl get pods
kubectl get service orderiq
```

The Deployment uses:

- `/healthz` for liveness.
- `/readyz` for readiness, preventing semantic traffic during index startup.
- ConfigMap values for non-secret configuration.
- A Secret reference for the OpenAI API key.
- A non-root security context with all Linux capabilities dropped.
- A PVC for the SQLite database and transformer cache.

## Implemented versus production deployment

| Exercise implementation | Enterprise evolution |
| --- | --- |
| One API replica and SQLite PVC | Multiple replicas backed by a managed relational database |
| In-memory vector snapshot | Tenant-scoped vector collections |
| One configured OpenAI model | Tenant-aware gateway routing to cloud or on-premise models |
| Kubernetes Secret | External secret manager with rotation and audit |
| ClusterIP only | Authenticated ingress/API gateway with TLS and rate limits |

See [ARCHITECTURE.md](../ARCHITECTURE.md) for the implemented boundaries and
[Part 4d](enterprise-architecture.md) for the residency-cell design.
