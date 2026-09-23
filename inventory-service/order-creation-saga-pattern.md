# Order Creation Saga Pattern

```text
                              CLIENT
                                │
                                │ POST /orders
                                ▼
                    ┌─────────────────────────┐
                    │      ORDER SERVICE      │
                    │                         │
                    │ Generate UUIDv4 sagaId  │
                    │                         │
                    │ Retry / Backoff         │
                    │ Feign Client            │
                    └────────────┬────────────┘
                                 │
                                 │ POST /inventory/reserve
                                 │
                                 │ sagaId + items
                                 ▼
                    ┌─────────────────────────┐
                    │    INVENTORY SERVICE    │
                    │                         │
                    │      BEGIN TX           │
                    │          │              │
                    │    Check sagaId         │
                    │          │              │
                    │    RESERVED?            │
                    │      │       │          │
                    │     YES      NO         │
                    │      │       │          │
                    │   return     ▼          │
                    │   success  Lock rows    │
                    │              │           │
                    │         Check stock      │
                    │              │           │
                    │         Decrement        │
                    │              │           │
                    │       Saga=RESERVED      │
                    │              │           │
                    │           COMMIT         │
                    └─────────────┬───────────┘
                                  │
                             200 SUCCESS
                                  │
                                  ▼
                    ┌─────────────────────────┐
                    │      ORDER SERVICE      │
                    │                         │
                    │    @Transactional       │
                    │                         │
                    │       INSERT Order      │
                    └────────────┬────────────┘
                                 │
                         ┌───────┴────────┐
                         │                │
                      SUCCESS           FAILURE
                         │                │
                         ▼                ▼
                        DONE       INSERT OUTBOX EVENT
                                          │
                                          ▼
                               ┌────────────────────┐
                               │      ORDER DB      │
                               │                    │
                               │ orders             │
                               │ outbox_events      │
                               └─────────┬──────────┘
                                         │
                                  Outbox Publisher
                                         │
                                         ▼
                               ┌────────────────────┐
                               │      RabbitMQ      │
                               │                    │
                               │ RELEASE_INVENTORY  │
                               └─────────┬──────────┘
                                         │
                                         ▼
                               ┌────────────────────┐
                               │ INVENTORY SERVICE  │
                               │                    │
                               │ Check eventId      │
                               │ Check saga state   │
                               │       │            │
                               │   RESERVED?        │
                               │    /       \       │
                               │   YES       NO     │
                               │    │         │     │
                               │ Release    Ignore  │
                               │    │               │
                               │ RESERVED           │
                               │     ↓              │
                               │ RELEASED            │
                               │     │              │
                               │   COMMIT            │
                               └────────────────────┘
```

---

# 1. Order Service DB

## `orders`

```text
orders
────────────────────────
id
user_id
total_amount
status
created_at
updated_at
...
```

**No `saga_id`.**

The order is created only after successful inventory reservation.

---

## `outbox_events`

```text
outbox_events
────────────────────────
event_id       PK
event_type
payload
status
attempts
created_at
published_at
```

No `saga_id` column.

The Saga ID is inside `payload`.

Example:

```json
{
  "eventId": "event-123",
  "sagaId": "saga-abc",
  "items": [
    {
      "inventoryId": 17,
      "quantity": 3
    },
    {
      "inventoryId": 42,
      "quantity": 2
    }
  ]
}
```

---

# 2. Inventory Service DB

## `inventory`

```text
inventory
────────────────────────
id
product_id
units_available
...
```

## `saga`

```text
saga
────────────────────────
saga_id       PK
status
created_at
updated_at
```

Only:

```text
RESERVED
RELEASED
```

No `PENDING`.

## `processed_events`

```text
processed_events
────────────────────────
event_id       PK
processed_at
```

Used for idempotent RabbitMQ consumption.

---

# 3. Forward Saga

Order receives:

```json
{
  "items": [
    {"inventoryId": 17, "quantity": 3},
    {"inventoryId": 42, "quantity": 2}
  ]
}
```

Generate:

```java
UUID sagaId = UUID.randomUUID();
```

Then:

```text
Order
  │
  │ Feign
  ▼
Inventory
```

with:

```json
{
  "sagaId": "ABC",
  "items": [
    {"inventoryId": 17, "quantity": 3},
    {"inventoryId": 42, "quantity": 2}
  ]
}
```

---

# 4. Inventory reservation transaction

Everything below is **one local Inventory DB transaction**:

```text
BEGIN
   │
   ├── Check sagaId
   │
   ├── If RESERVED → return success
   │
   ├── Sort inventory IDs
   │
   ├── SELECT ... FOR UPDATE
   │
   ├── Check availability
   │
   ├── Decrement inventory
   │
   ├── INSERT saga
   │       sagaId = ABC
   │       status = RESERVED
   │
   └── COMMIT
```

If anything fails:

```text
ROLLBACK
```

So inventory decrement and `Saga = RESERVED` are atomic.

---

# 5. Retry behavior

### 4xx

```text
409 INSUFFICIENT_INVENTORY
        ↓
Don't retry
        ↓
Return error
```

### 5xx / timeout

```text
503 / timeout
        ↓
Retry
        ↓
SAME sagaId
```

Example:

```text
ABC → attempt 1
ABC → attempt 2
ABC → attempt 3
```

Never:

```text
ABC → attempt 1
XYZ → attempt 2    ❌
```

If the first request actually committed but its response was lost:

```text
ABC → RESERVED
```

the retry finds:

```text
ABC → RESERVED
```

and returns success without decrementing inventory again.

---

# 6. Successful reservation → Order creation

Once Inventory returns:

```text
200 OK
```

Order Service creates:

```text
@Transactional
INSERT INTO orders ...
```

Now:

```text
Inventory → RESERVED
Order → CREATED
```

Done.

---

# 7. Order creation failure → Outbox

Suppose:

```text
Inventory → RESERVED
        ↓
Order INSERT
        ↓
FAIL
```

Order Service creates:

```text
outbox_events
────────────────────────
event_id    = XYZ
event_type  = RELEASE_INVENTORY
payload     = {...}
status      = PENDING
```

The payload contains:

```json
{
  "eventId": "XYZ",
  "sagaId": "ABC",
  "items": [
    {"inventoryId": 17, "quantity": 3},
    {"inventoryId": 42, "quantity": 2}
  ]
}
```

The event is now durable in Order DB.

---

# 8. Outbox Publisher

Background worker:

```text
Order DB
   │
   │ PENDING events
   ▼
Outbox Publisher
   │
   ▼
RabbitMQ
```

If RabbitMQ is down:

```text
PENDING
   ↓
retry later
```

Nothing is lost.

Once published:

```text
PENDING → PUBLISHED
```

---

# 9. Compensation

RabbitMQ sends:

```text
RELEASE_INVENTORY
```

Inventory consumer:

```text
BEGIN
   │
   ├── Check eventId
   │
   ├── Find Saga by sagaId
   │
   ├── Is status RESERVED?
   │       │
   │       ├── NO → ignore
   │       │
   │       └── YES
   │
   ├── Sort inventory IDs
   │
   ├── SELECT ... FOR UPDATE
   │
   ├── Restore quantities
   │
   ├── Saga: RESERVED → RELEASED
   │
   ├── Mark eventId processed
   │
   └── COMMIT
```

---

# 10. Duplicate RabbitMQ delivery

RabbitMQ can deliver the same event twice.

First:

```text
event XYZ
    ↓
Saga = RESERVED
    ↓
restore inventory
    ↓
Saga = RELEASED
    ↓
mark XYZ processed
```

Second:

```text
event XYZ
    ↓
XYZ already processed
    ↓
ignore
```

So:

```text
7 → 10
```

not:

```text
7 → 13
```

---

# 11. Final state machine

```text
                    reserve
                       │
                       ▼
                ┌─────────────┐
                │  RESERVED   │
                └──────┬──────┘
                       │
                 Order fails
                       │
                       ▼
                ┌─────────────┐
                │   RELEASED  │
                └─────────────┘
```

That's all you need.

---

# Final component/data model

```text
                         ORDER SERVICE
                    ┌────────────────────┐
                    │                    │
                    │ Feign Client       │
                    │ Retry              │
                    │ Order Creation     │
                    │ Outbox Publisher   │
                    │                    │
                    └───────┬────────────┘
                            │
                    ┌───────┴────────┐
                    │                │
                  Feign              │
                    │                │
                    ▼                ▼
            INVENTORY SERVICE     ORDER DB
                    │           ┌──────────────┐
                    │           │ orders       │
                    │           │ outbox_events│
                    │           └──────────────┘
                    │
                    ▼
              INVENTORY DB
           ┌──────────────────┐
           │ inventory        │
           │ saga             │
           │ processed_events │
           └────────┬─────────┘
                    ▲
                    │
                 RabbitMQ
                    ▲
                    │
              Outbox Publisher
```

### The four critical invariants

**1. Reservation atomicity**

```text
inventory decrement
       +
Saga = RESERVED
       ↓
same DB transaction
```

**2. Reservation idempotency**

```text
same sagaId
     ↓
same reservation
     ↓
never decrement twice
```

**3. Durable compensation**

```text
Order failure
     ↓
Outbox PENDING
     ↓
RabbitMQ eventually
```

**4. Idempotent compensation**

```text
RESERVED → RELEASED

RELEASED → do nothing
```