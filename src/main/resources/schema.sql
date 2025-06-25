CREATE TABLE IF NOT EXISTS orders (
    event_id TEXT PRIMARY KEY,
    product_code TEXT NOT NULL,
    quantity INT NOT NULL,
    created_at BIGINT NOT NULL
);


CREATE TABLE IF NOT EXISTS outbox_events (
  id            UUID      PRIMARY KEY,
  aggregate_id  TEXT      NOT NULL,
  event_type    TEXT      NOT NULL,
  occurred_at   TIMESTAMP    NOT NULL,
  payload       TEXT     NOT NULL
);
