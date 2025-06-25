# Spring Kafka Exactly-Once Processing Demo

This repository demonstrates the outbox pattern with a PostgreSQL-backed Spring Boot application, Kafka, Schema Registry, and Kafka Connect. It ensures **exactly-once Kafka delivery**. All services are orchestrated via Docker Compose.

---

## Components

- **PostgreSQL (`ordersdb`)**  
  Stores `orders` and `outbox_events` tables for change data capture.

- **Kafka**  
  Message broker for streaming events.

- **Schema Registry**  
  Manages Avro schemas for Kafka topics.

- **Kafka Connect (JDBC Source Connector)**  
  Polls `public.outbox_events` and publishes into a Kafka topic (`outbox_events`).

- **Spring Boot App**  
  Produces `OrderRequestEvent`s (writes to `orders` & `outbox_events`) and processes them (updates status).

---

## How to run

Build & Run

```
git clone https://github.com/zeinab-dashti/spring-kafka-outbox-pattern-demo.git

cd spring-kafka-outbox-pattern-demo
docker compose up --build -d
```

This will stand up:
* Zookeeper, Kafka, Schema Registry
* Postgres (_ordersdb_)
* Kafka Connect
* Spring Boot app (8080)


Wait for containers to become healthy
```
docker compose ps
```

---

## Verifying Connector

1. List available connector plugins
```
curl -s http://localhost:8083/connector-plugins | jq '
.[] | select(.class | test("Jdbc.*Connector"))'

 
{
   "class": "io.confluent.connect.jdbc.JdbcSinkConnector",
   "type": "sink",
   "version": "10.8.4"
}
{
   "class": "io.confluent.connect.jdbc.JdbcSourceConnector",
   "type": "source",
   "version": "10.8.4"
}
```

2. Check registered connector
```
curl http://localhost:8083/connectors

["outbox-source-connector"]%
```

If you see empty list then Post the connector:
```
curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d @connectors/outbox-source-connector.json
```

3. Inspect connector status
```
curl -s http://localhost:8083/connectors/outbox-source-connector/status | jq .

 
{
  "name": "outbox-source-connector",
  "connector": {
    "state": "RUNNING",
    "worker_id": "connect:8083"
  },
  "tasks": [
    {
      "id": 0,
      "state": "RUNNING",
      "worker_id": "connect:8083"
    }
  ],
  "type": "source"
}
```

If you instead get an error or see tasks in a FAILED state, grab the last fifty lines of the Connect logs and paste them here:
```
docker compose logs connect --tail=50
```

---

## Inspect Output

If everything is configured correctly, you should see:
* Spring Kafka listener
  * Consumes every message from the _order-requests_ and inserts it into a relational database’s _orders_ table.
  * Inserts records into _outbox_events_ table.
* JDBC Connector
  * Automatically reads the records from _outbox_events_ table and produces processed records to the _outbox_events_ topic.


### Query Records from Postgres table 

* Query business table:
```
docker compose exec postgres psql -U postgres -d ordersdb \
-c "SELECT * FROM orders;"

 event_id | product_code | quantity |  created_at
----------+--------------+----------+---------------
 evt-1    | P-001        |        2 | 1750823261132
 evt-2    | P-002        |        5 | 1750823261132
 evt-3    | P-003        |        1 | 1750823261132
(3 rows)
```

* Query outbox table:
```
docker compose exec postgres psql -U postgres -d ordersdb \
-c "SELECT * FROM outbox_events;"

       id       | aggregate_id |    event_type     |  occurred_at  |                                     payload
----------------+--------------+-------------------+---------------+----------------------------------------------------------------------------------
 19086f16-8e... | evt-1        | OrderRequestEvent | 1750823263221 | {"eventId":"evt-1","productCode":"P-001","quantity":2,"timestamp":1750823261132}
 8f8bff7c-9a... | evt-2        | OrderRequestEvent | 1750823263356 | {"eventId":"evt-2","productCode":"P-002","quantity":5,"timestamp":1750823261132}
 31e3612c-4e... | evt-3        | OrderRequestEvent | 1750823263375 | {"eventId":"evt-3","productCode":"P-003","quantity":1,"timestamp":1750823261132}
(3 rows)
```

* Consume Processed Orders:

```
docker compose exec schema-registry \
  kafka-avro-console-consumer \
  --bootstrap-server kafka:9092 \
  --topic outbox_events \
  --from-beginning \
  --max-messages 5 \
  --property schema.registry.url=http://schema-registry:8081
```


Generated log should have:
```
{"id":"7dbe7369-c2d4-4f01-8e6e-947873a7bc18","aggregate_id":"evt-1","event_type":"OrderRequestEvent","occurred_at":1750849489575,"payload":"{\"eventId\":\"evt-1\",\"status\":\"PROCESSED\",\"processedAt\":1750849490813}"}
{"id":"e7b5d630-a469-4262-bab9-5672b8147b6e","aggregate_id":"evt-2","event_type":"OrderRequestEvent","occurred_at":1750849489575,"payload":"{\"eventId\":\"evt-2\",\"status\":\"PROCESSED\",\"processedAt\":1750849490966}"}
{"id":"d64f0998-e04e-4b4d-9cb8-403696e8c7ce","aggregate_id":"evt-3","event_type":"OrderRequestEvent","occurred_at":1750849489575,"payload":"{\"eventId\":\"evt-3\",\"status\":\"PROCESSED\",\"processedAt\":1750849490981}"}
```
