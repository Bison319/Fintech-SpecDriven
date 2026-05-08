# SpecKit Specification — Fintech Transaction & Wallet Platform

> OpenAPI specs, schemas, error contracts, idempotency rules, and validation rules.

---

## 1. User Service API (`/api/v1/users`)

### 1.1 OpenAPI Specification

```yaml
openapi: 3.0.3
info:
  title: User Service API
  version: 1.0.0
  description: Manages user registration and profile operations.

paths:
  /api/v1/users:
    post:
      operationId: createUser
      summary: Register a new user
      tags: [Users]
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateUserRequest'
      responses:
        '201':
          description: User created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiResponse_UserResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '409':
          $ref: '#/components/responses/ConflictError'

    get:
      operationId: listUsers
      summary: List users with pagination
      tags: [Users]
      parameters:
        - $ref: '#/components/parameters/PageParam'
        - $ref: '#/components/parameters/SizeParam'
      responses:
        '200':
          description: Paginated user list
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiResponse_PagedUsers'

  /api/v1/users/{userId}:
    get:
      operationId: getUserById
      summary: Get user by ID
      tags: [Users]
      parameters:
        - name: userId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: User details
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiResponse_UserResponse'
        '404':
          $ref: '#/components/responses/NotFoundError'

    put:
      operationId: updateUser
      summary: Update user profile
      tags: [Users]
      parameters:
        - name: userId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdateUserRequest'
      responses:
        '200':
          description: User updated
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiResponse_UserResponse'

    delete:
      operationId: deactivateUser
      summary: Deactivate user (soft delete)
      tags: [Users]
      parameters:
        - name: userId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: User deactivated

components:
  schemas:
    CreateUserRequest:
      type: object
      required: [email, fullName]
      properties:
        email:
          type: string
          format: email
          maxLength: 255
        fullName:
          type: string
          minLength: 2
          maxLength: 100
        phoneNumber:
          type: string
          pattern: '^\+[1-9]\d{1,14}$'

    UpdateUserRequest:
      type: object
      properties:
        fullName:
          type: string
          minLength: 2
          maxLength: 100
        phoneNumber:
          type: string
          pattern: '^\+[1-9]\d{1,14}$'

    UserResponse:
      type: object
      properties:
        id:
          type: string
          format: uuid
        email:
          type: string
        fullName:
          type: string
        phoneNumber:
          type: string
        status:
          type: string
          enum: [ACTIVE, INACTIVE, SUSPENDED]
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time
```

---

## 2. Wallet Service API (`/api/v1/wallets`)

### 2.1 OpenAPI Specification

```yaml
openapi: 3.0.3
info:
  title: Wallet Service API
  version: 1.0.0
  description: Manages wallet lifecycle and balance operations.

paths:
  /api/v1/wallets:
    post:
      operationId: createWallet
      summary: Create a new wallet for a user
      tags: [Wallets]
      headers:
        - $ref: '#/components/parameters/IdempotencyKey'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateWalletRequest'
      responses:
        '201':
          description: Wallet created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiResponse_WalletResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '409':
          $ref: '#/components/responses/ConflictError'

  /api/v1/wallets/{walletId}:
    get:
      operationId: getWallet
      summary: Get wallet details and balance
      tags: [Wallets]
      parameters:
        - name: walletId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Wallet details
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiResponse_WalletResponse'
        '404':
          $ref: '#/components/responses/NotFoundError'

  /api/v1/wallets/{walletId}/credit:
    post:
      operationId: creditWallet
      summary: Credit (add funds to) a wallet
      tags: [Wallets]
      parameters:
        - name: walletId
          in: path
          required: true
          schema:
            type: string
            format: uuid
        - $ref: '#/components/parameters/IdempotencyKey'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/AdjustBalanceRequest'
      responses:
        '200':
          description: Balance updated
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiResponse_WalletResponse'
        '404':
          $ref: '#/components/responses/NotFoundError'
        '409':
          description: Optimistic lock conflict — retry
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

  /api/v1/wallets/{walletId}/debit:
    post:
      operationId: debitWallet
      summary: Debit (withdraw funds from) a wallet
      tags: [Wallets]
      parameters:
        - name: walletId
          in: path
          required: true
          schema:
            type: string
            format: uuid
        - $ref: '#/components/parameters/IdempotencyKey'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/AdjustBalanceRequest'
      responses:
        '200':
          description: Balance updated
        '404':
          $ref: '#/components/responses/NotFoundError'
        '422':
          description: Insufficient balance
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

  /api/v1/wallets/user/{userId}:
    get:
      operationId: getWalletsByUser
      summary: List all wallets for a user
      tags: [Wallets]
      parameters:
        - name: userId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: User wallets

components:
  schemas:
    CreateWalletRequest:
      type: object
      required: [userId, currency]
      properties:
        userId:
          type: string
          format: uuid
        currency:
          type: string
          enum: [USD, EUR, GBP, INR]
          description: ISO 4217 currency code

    AdjustBalanceRequest:
      type: object
      required: [amount, description]
      properties:
        amount:
          type: number
          format: decimal
          minimum: 0.01
          description: Must be positive. Use credit/debit endpoints to indicate direction.
        description:
          type: string
          maxLength: 500
        referenceId:
          type: string
          format: uuid
          description: External reference (e.g., transaction ID)

    WalletResponse:
      type: object
      properties:
        id:
          type: string
          format: uuid
        userId:
          type: string
          format: uuid
        balance:
          type: number
          format: decimal
        currency:
          type: string
        status:
          type: string
          enum: [ACTIVE, FROZEN, CLOSED]
        version:
          type: integer
          format: int64
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time

  parameters:
    IdempotencyKey:
      name: Idempotency-Key
      in: header
      required: true
      schema:
        type: string
        format: uuid
      description: Client-generated UUID to ensure idempotent processing.
```

---

## 3. Transaction Service API (`/api/v1/transactions`)

### 3.1 OpenAPI Specification

```yaml
openapi: 3.0.3
info:
  title: Transaction Service API
  version: 1.0.0
  description: Orchestrates payment transactions with idempotency and fraud checks.

paths:
  /api/v1/transactions:
    post:
      operationId: processTransaction
      summary: Process a new transaction
      tags: [Transactions]
      parameters:
        - $ref: '#/components/parameters/IdempotencyKey'
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateTransactionRequest'
      responses:
        '201':
          description: Transaction processed
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiResponse_TransactionResponse'
        '400':
          $ref: '#/components/responses/ValidationError'
        '409':
          description: Duplicate idempotency key — returning original response
        '422':
          description: Business rule violation (insufficient funds, fraud rejected)

    get:
      operationId: listTransactions
      summary: List transactions with filters
      tags: [Transactions]
      parameters:
        - name: walletId
          in: query
          schema:
            type: string
            format: uuid
        - name: type
          in: query
          schema:
            type: string
            enum: [CREDIT, DEBIT, TRANSFER]
        - name: status
          in: query
          schema:
            type: string
            enum: [PENDING, COMPLETED, FAILED, REVERSED]
        - name: fromDate
          in: query
          schema:
            type: string
            format: date
        - name: toDate
          in: query
          schema:
            type: string
            format: date
        - $ref: '#/components/parameters/PageParam'
        - $ref: '#/components/parameters/SizeParam'
      responses:
        '200':
          description: Transaction list

  /api/v1/transactions/{transactionId}:
    get:
      operationId: getTransaction
      summary: Get transaction details
      tags: [Transactions]
      parameters:
        - name: transactionId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Transaction details
        '404':
          $ref: '#/components/responses/NotFoundError'

components:
  schemas:
    CreateTransactionRequest:
      type: object
      required: [sourceWalletId, type, amount, currency]
      properties:
        sourceWalletId:
          type: string
          format: uuid
        targetWalletId:
          type: string
          format: uuid
          description: Required for TRANSFER type
        type:
          type: string
          enum: [CREDIT, DEBIT, TRANSFER]
        amount:
          type: number
          format: decimal
          minimum: 0.01
          maximum: 1000000.00
        currency:
          type: string
          enum: [USD, EUR, GBP, INR]
        description:
          type: string
          maxLength: 500

    TransactionResponse:
      type: object
      properties:
        id:
          type: string
          format: uuid
        sourceWalletId:
          type: string
          format: uuid
        targetWalletId:
          type: string
          format: uuid
        type:
          type: string
          enum: [CREDIT, DEBIT, TRANSFER]
        amount:
          type: number
          format: decimal
        currency:
          type: string
        status:
          type: string
          enum: [PENDING, COMPLETED, FAILED, REVERSED]
        idempotencyKey:
          type: string
          format: uuid
        fraudCheckResult:
          type: string
          enum: [APPROVED, REVIEW, REJECTED]
        description:
          type: string
        createdAt:
          type: string
          format: date-time
        completedAt:
          type: string
          format: date-time
```

---

## 4. Fraud Service API (`/api/v1/fraud`)

### 4.1 OpenAPI Specification

```yaml
openapi: 3.0.3
info:
  title: Fraud Service API
  version: 1.0.0
  description: Rule-based fraud detection and risk scoring.

paths:
  /api/v1/fraud/check:
    post:
      operationId: evaluateTransaction
      summary: Evaluate transaction for fraud risk
      tags: [Fraud]
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/FraudCheckRequest'
      responses:
        '200':
          description: Fraud check result
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ApiResponse_FraudCheckResponse'

  /api/v1/fraud/checks/{userId}:
    get:
      operationId: getFraudHistory
      summary: Get fraud check history for a user
      tags: [Fraud]
      parameters:
        - name: userId
          in: path
          required: true
          schema:
            type: string
            format: uuid
        - $ref: '#/components/parameters/PageParam'
        - $ref: '#/components/parameters/SizeParam'
      responses:
        '200':
          description: Fraud check history

components:
  schemas:
    FraudCheckRequest:
      type: object
      required: [userId, walletId, amount, currency, transactionType]
      properties:
        userId:
          type: string
          format: uuid
        walletId:
          type: string
          format: uuid
        amount:
          type: number
          format: decimal
        currency:
          type: string
        transactionType:
          type: string
          enum: [CREDIT, DEBIT, TRANSFER]

    FraudCheckResponse:
      type: object
      properties:
        checkId:
          type: string
          format: uuid
        decision:
          type: string
          enum: [APPROVED, REVIEW, REJECTED]
        riskScore:
          type: integer
          minimum: 0
          maximum: 100
        reasons:
          type: array
          items:
            type: string
        checkedAt:
          type: string
          format: date-time
```

---

## 5. Error Contract (Shared)

All services use this uniform error response:

```yaml
ErrorResponse:
  type: object
  properties:
    success:
      type: boolean
      example: false
    data:
      type: object
      nullable: true
    error:
      type: object
      properties:
        code:
          type: string
          description: Machine-readable error code
          example: WALLET_INSUFFICIENT_BALANCE
        message:
          type: string
          description: Human-readable error message
        details:
          type: array
          items:
            type: object
            properties:
              field:
                type: string
              message:
                type: string
    metadata:
      type: object
      properties:
        timestamp:
          type: string
          format: date-time
        correlationId:
          type: string
          format: uuid
        service:
          type: string
```

### Error Code Catalog

| Code                            | HTTP | Service      | Description                             |
| ------------------------------- | ---- | ------------ | --------------------------------------- |
| `USER_NOT_FOUND`                | 404  | User         | No user found with given ID             |
| `USER_EMAIL_DUPLICATE`          | 409  | User         | Email already registered                |
| `WALLET_NOT_FOUND`              | 404  | Wallet       | No wallet found with given ID           |
| `WALLET_INSUFFICIENT_BALANCE`   | 422  | Wallet       | Debit amount exceeds available balance  |
| `WALLET_FROZEN`                 | 422  | Wallet       | Wallet is frozen, operations blocked    |
| `WALLET_VERSION_CONFLICT`       | 409  | Wallet       | Optimistic lock failure, retry needed   |
| `TXN_DUPLICATE_IDEMPOTENCY_KEY`| 409  | Transaction  | Idempotency key already processed       |
| `TXN_FRAUD_REJECTED`           | 422  | Transaction  | Transaction rejected by fraud check     |
| `TXN_PROCESSING_FAILED`        | 500  | Transaction  | Internal error during processing        |
| `FRAUD_CHECK_FAILED`           | 500  | Fraud        | Fraud engine error                      |
| `VALIDATION_ERROR`             | 400  | All          | Request validation failure              |
| `SERVICE_UNAVAILABLE`          | 503  | All          | Downstream dependency unreachable       |

---

## 6. Idempotency Rules

### 6.1 Protocol

1. Client generates a UUID v4 as `Idempotency-Key` header.
2. Server checks `idempotency_keys` table for existing key.
3. If found: return cached response with `200` status and `X-Idempotent-Replayed: true` header.
4. If not found: process request, store `(key, response_body, response_status, created_at)`, return result.
5. Keys expire after **24 hours** (cleaned by scheduled job).

### 6.2 Idempotency Key Table Schema

```sql
CREATE TABLE idempotency_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key UUID NOT NULL,
    operation       VARCHAR(50) NOT NULL,
    request_hash    VARCHAR(64) NOT NULL,
    response_status INTEGER NOT NULL,
    response_body   JSONB NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP NOT NULL DEFAULT NOW() + INTERVAL '24 hours',
    UNIQUE (idempotency_key, operation)
);

CREATE INDEX idx_idempotency_key ON idempotency_keys (idempotency_key, operation);
CREATE INDEX idx_idempotency_expires ON idempotency_keys (expires_at);
```

### 6.3 Request Hash Verification

The server computes a SHA-256 hash of the request body. If a duplicate key arrives with a **different** request body hash, the server returns `422 Unprocessable Entity` with error code `IDEMPOTENCY_KEY_MISMATCH`.

---

## 7. Validation Rules

### 7.1 User Service

| Field        | Rule                            | Error Message                      |
| ------------ | ------------------------------- | ---------------------------------- |
| `email`      | Required, valid email, max 255  | "Valid email is required"          |
| `fullName`   | Required, 2-100 chars           | "Full name must be 2-100 characters" |
| `phoneNumber`| Optional, E.164 format          | "Phone must be in E.164 format"    |

### 7.2 Wallet Service

| Field      | Rule                          | Error Message                        |
| ---------- | ----------------------------- | ------------------------------------ |
| `userId`   | Required, valid UUID          | "Valid user ID is required"          |
| `currency` | Required, one of [USD,EUR,GBP,INR] | "Unsupported currency"          |
| `amount`   | Required, > 0.00, max 1M     | "Amount must be between 0.01 and 1,000,000" |

### 7.3 Transaction Service

| Field             | Rule                              | Error Message                           |
| ----------------- | --------------------------------- | --------------------------------------- |
| `sourceWalletId`  | Required, valid UUID              | "Source wallet ID is required"          |
| `targetWalletId`  | Required for TRANSFER type        | "Target wallet required for transfers"  |
| `type`            | Required, one of [CREDIT,DEBIT,TRANSFER] | "Invalid transaction type"       |
| `amount`          | Required, 0.01 - 1,000,000       | "Amount out of allowed range"           |
| `currency`        | Required, must match wallet currency | "Currency mismatch"                  |

---

*All API implementations must conform exactly to this specification. Deviations require a spec amendment approved in a PR.*
