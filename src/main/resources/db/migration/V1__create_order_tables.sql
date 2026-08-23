CREATE TABLE orders
(
    id           UUID PRIMARY KEY,
    customer_id  VARCHAR(100)    NOT NULL,
    status       VARCHAR(30)     NOT NULL,
    total_amount NUMERIC(19, 2)  NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE order_items
(
    id         BIGSERIAL PRIMARY KEY,
    order_id   UUID           NOT NULL,
    product_id VARCHAR(100)   NOT NULL,
    quantity   INTEGER        NOT NULL,
    unit_price NUMERIC(19, 2) NOT NULL,

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES orders (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_order_items_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_order_items_unit_price
        CHECK (unit_price > 0)
);

CREATE INDEX idx_orders_customer_id
    ON orders (customer_id);

CREATE INDEX idx_order_items_order_id
    ON order_items (order_id);