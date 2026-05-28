# Payments Implementation

A Spring Boot application that integrates with **Razorpay** to handle payment link creation and webhook processing.

Built as a learning project to understand real-world payment integrations — covering signature verification, idempotency, and order lifecycle management.

---

## Tech Stack

- Java 21
- Spring Boot 3.5
- Spring Data JPA
- H2 (in-memory DB, swap with PostgreSQL for production)
- Razorpay Java SDK
- Thymeleaf (payment status page)
- Lombok

---

## How It Works

### Payment Flow

1. Client calls `POST /api/orders` with amount and customer details
2. App creates a Razorpay Payment Link and stores the order in DB with status `PENDING`
3. Customer pays via the Razorpay-hosted payment page
4. Razorpay sends a webhook to `POST /webhook/razorpay`
5. App verifies the signature, checks for duplicate events (idempotency), and updates the order status
6. Customer is redirected to `/payment-status` to see the result

### Order Status

```
PENDING → SUCCESS
PENDING → FAILED
```

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders` | Create an order and get a payment link |
| POST | `/webhook/razorpay` | Razorpay webhook receiver |
| GET | `/payment-status?razorpay_payment_link_id=<id>` | Payment result page |

### Create Order — Request Body

```json
{
  "amount": 50000,
  "customerName": "Aravind",
  "customerEmail": "aravind@example.com"
}
```

> Amount is in **paise** (50000 = ₹500)

### Create Order — Response

```json
{
  "orderId": "uuid",
  "paymentLink": "https://rzp.io/...",
  "amount": 500,
  "status": "PENDING"
}
```

---

## Key Implementation Details

### Signature Verification

Every incoming webhook is verified using HMAC-SHA256 before processing:

```
HMAC(payload, webhookSecret) == X-Razorpay-Signature header
```

Invalid signatures are rejected with `400 Bad Request`.

### Idempotency

Before processing any webhook, the app checks if a `PaymentEvent` already exists for that `paymentLinkId`. Duplicate webhooks (Razorpay retries on failure) are silently skipped.

---

## Local Setup

### 1. Clone and configure

Copy `application.properties` and fill in your Razorpay credentials:

```properties
razorpay.key.id=your_key_id
razorpay.key.secret=your_key_secret
razorpay.webhook.secret=your_webhook_secret
app.base-url=https://your-ngrok-url
```

> Use `application-local.properties` (gitignored) to keep real keys off git.

### 2. Run the app

```bash
./mvnw spring-boot-run
```

### 3. Expose locally with ngrok (for webhook testing)

```bash
ngrok http 8080
```

Update `app.base-url` and your Razorpay dashboard webhook URL with the ngrok URL.

### 4. H2 Console (for debugging DB)

```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:paymentsdb
```

---

## Project Structure

```
src/main/java/org/example/paymentsimplementation/
├── controller/
│   ├── OrderController.java          # POST /api/orders
│   ├── WebhookController.java        # POST /webhook/razorpay
│   └── PaymentStatusController.java  # GET /payment-status
├── service/
│   ├── OrderService.java             # Creates Razorpay payment link
│   └── WebhookService.java           # Signature verification + event handling
├── entity/
│   ├── Order.java
│   ├── OrderStatus.java
│   └── PaymentEvent.java
├── repository/
│   ├── OrderRepository.java
│   └── PaymentEventRepository.java
└── dto/
    ├── OrderRequest.java
    └── OrderResponse.java
```
