CREATE TABLE trucks (
    id               BIGSERIAL      PRIMARY KEY,
    chassis_number   VARCHAR(100)   NOT NULL UNIQUE,
    license_plate    VARCHAR(50)    NOT NULL UNIQUE,
    container_volume NUMERIC(10, 4) NOT NULL
);

CREATE TABLE deliveries (
    id            BIGSERIAL PRIMARY KEY,
    order_id      BIGINT    NOT NULL UNIQUE REFERENCES orders (id),
    delivery_date DATE      NOT NULL
);

CREATE TABLE delivery_trucks (
    delivery_id BIGINT NOT NULL REFERENCES deliveries (id),
    truck_id    BIGINT NOT NULL REFERENCES trucks (id),
    PRIMARY KEY (delivery_id, truck_id)
);

CREATE INDEX idx_deliveries_delivery_date ON deliveries (delivery_date);
CREATE INDEX idx_deliveries_order_id      ON deliveries (order_id);
CREATE INDEX idx_delivery_trucks_truck_id ON delivery_trucks (truck_id);
