-- Indexes to support paginated customer payment and order history queries
CREATE INDEX IF NOT EXISTS idx_capacity_reservations_user_id
    ON capacity_reservations (user_id, id);

CREATE INDEX IF NOT EXISTS idx_payments_cap_res_created_at
    ON payments (capacity_reservation_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_user_confirmed_at
    ON orders (user_id, confirmed_at DESC);

CREATE INDEX IF NOT EXISTS idx_orders_payment_id
    ON orders (payment_id);
