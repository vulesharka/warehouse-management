CREATE TABLE users (
    id         BIGSERIAL    PRIMARY KEY,
    username   VARCHAR(100) NOT NULL UNIQUE,
    email      VARCHAR(150) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(30)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE inventory_items (
    id             BIGSERIAL      PRIMARY KEY,
    name           VARCHAR(150)   NOT NULL,
    quantity       INTEGER        NOT NULL DEFAULT 0,
    unit_price     NUMERIC(10, 2) NOT NULL,
    package_volume NUMERIC(10, 4) NOT NULL
);

CREATE TABLE orders (
    id             BIGSERIAL    PRIMARY KEY,
    order_number   VARCHAR(50)  NOT NULL UNIQUE,
    status         VARCHAR(30)  NOT NULL DEFAULT 'CREATED',
    submitted_date TIMESTAMP,
    decline_reason VARCHAR(500),
    client_id      BIGINT       NOT NULL REFERENCES users (id),
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE order_items (
    id                 BIGSERIAL PRIMARY KEY,
    order_id           BIGINT    NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    inventory_item_id  BIGINT    NOT NULL REFERENCES inventory_items (id),
    requested_quantity INTEGER   NOT NULL,
    deadline_date      DATE      NOT NULL
);

CREATE INDEX idx_orders_client_id ON orders (client_id);
CREATE INDEX idx_orders_status    ON orders (status);
CREATE INDEX idx_order_items_order_id ON order_items (order_id);
