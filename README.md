# GST Banking Platform

GST Banking Platform is a backend banking application built using a
microservices architecture.

The system separates authentication, customer management, bank accounts,
transactions, beneficiaries, notifications, and auditing into independent
services.

The main goal of the project is to demonstrate how a modern banking backend
can be designed using Java, Spring Boot, Spring Security, Spring Cloud Gateway,
JWT authentication, relational databases, and service-to-service communication.

---

# 1. Project Architecture

The application follows a microservices architecture.

```text
                         Client
                  Postman / Web / Mobile
                            |
                            v
                  +-------------------+
                  |  Gateway Service  |
                  |       :8080       |
                  +---------+---------+
                            |
        +-------------------+-------------------+
        |                   |                   |
        v                   v                   v
 +--------------+    +----------------+   +--------------+
 | Auth Service |    |Customer Service|   |Account Service|
 |    :8081     |    |     :8082      |   |    :8083     |
 +------+-------+    +-------+--------+   +------+-------+
        |                    |                   |
        v                    v                   v
     Auth DB            Customer DB          Account DB

                            |
                            v
                  +---------------------+
                  | Transaction Service |
                  |        :8084        |
                  +----------+----------+
                             |
                             v
                       Transaction DB

             +---------------+---------------+
             |                               |
             v                               v
 +---------------------+          +----------------------+
 | Beneficiary Service |          | Notification Service |
 |        :8085        |          |        :8086         |
 +---------------------+          +----------------------+

                             |
                             v
                    +----------------+
                    | Audit Service  |
                    |     :8087      |
                    +----------------+
```

The API Gateway is the main entry point for client applications.

Instead of calling individual microservices directly, clients normally send
requests to:

```text
http://localhost:8080
```

The Gateway determines which microservice should process the request.

---

# 2. Microservices

## 2.1 Gateway Service

Port:

```text
8080
```

The Gateway Service is the main entry point into the banking platform.

The client should normally communicate with the Gateway rather than directly
calling every backend microservice.

Responsibilities:

- Route incoming requests
- Provide a single API entry point
- Forward authentication requests
- Forward customer requests
- Forward account requests
- Forward transaction requests
- Handle CORS
- Support centralized authentication/security policies
- Hide internal microservice addresses from clients

Example routing:

```text
/api/auth/**          -> auth-service:8081

/api/customers/**     -> customer-service:8082

/api/accounts/**      -> account-service:8083

/api/transactions/**  -> transaction-service:8084

/api/beneficiaries/** -> beneficiary-service:8085
```

Example:

```text
POST
http://localhost:8080/api/auth/login

                |
                v

        Gateway Service
             :8080

                |
                v

         Auth Service
             :8081
```

The Gateway itself should normally not contain banking entities,
repositories, or direct database access.

---

## 2.2 Auth Service

Port:

```text
8081
```

The Auth Service is responsible for user authentication and security.

Responsibilities:

- User registration
- User login
- Password encryption
- JWT generation
- JWT validation support
- User identity management
- Role management
- Authentication failure handling

Technologies:

- Spring Security
- JWT
- BCrypt
- Spring Data JPA
- MySQL/PostgreSQL

Example endpoints:

```http
POST /api/auth/register

POST /api/auth/login
```

Example registration:

```json
{
  "username": "john",
  "email": "john@example.com",
  "password": "StrongPassword123!"
}
```

The password should never be stored directly.

The service stores something similar to:

```text
BCrypt(password)
```

rather than the original password.

During login:

```text
Email + Password
        |
        v
AuthenticationManager
        |
        v
AuthenticationProvider
        |
        v
UserDetailsService
        |
        v
Database
        |
        v
Password validated
        |
        v
JWT generated
```

---

## 2.3 Customer Service

Port:

```text
8082
```

The Customer Service manages customer information.

Authentication answers:

```text
"Who is this user?"
```

Customer management answers:

```text
"What banking customer profile belongs to this user?"
```

Responsibilities:

- Create customer
- Retrieve customer
- Update customer information
- Manage customer profile
- Customer status management
- Customer validation
- Search customers where appropriate

Possible customer information:

```text
customerId
userId
firstName
lastName
email
phone
dateOfBirth
address
status
createdAt
updatedAt
```

Example endpoints:

```http
POST /api/customers

GET /api/customers/{id}

GET /api/customers

PUT /api/customers/{id}

DELETE /api/customers/{id}
```

Example:

```text
GET
http://localhost:8080/api/customers/101
```

Flow:

```text
Client
   |
   v
Gateway
   |
   v
Customer Service
   |
   v
Customer Database
```

---

## 2.4 Account Service

Port:

```text
8083
```

The Account Service manages bank accounts belonging to customers.

Responsibilities:

- Create bank account
- Retrieve account
- Retrieve accounts belonging to customer
- Manage account type
- Manage account status
- Maintain account-related information
- Support balance-related business operations

Possible account information:

```text
accountId
accountNumber
customerId
accountType
balance
currency
status
createdAt
updatedAt
```

Possible account types:

```text
SAVINGS

CURRENT
```

Possible account statuses:

```text
ACTIVE

BLOCKED

CLOSED
```

Example endpoints:

```http
POST /api/accounts

GET /api/accounts/{accountId}

GET /api/accounts/customer/{customerId}

PUT /api/accounts/{accountId}/status
```

Example:

```text
GET
http://localhost:8080/api/accounts/customer/101
```

---

## 2.5 Transaction Service

Port:

```text
8084
```

The Transaction Service handles movement of money.

This is one of the most important services in the banking platform.

Responsibilities:

- Deposit money
- Withdraw money
- Transfer money
- Create transaction records
- Maintain transaction status
- Generate transaction references
- Retrieve transaction history
- Validate transaction requests

Possible transaction types:

```text
DEPOSIT

WITHDRAWAL

TRANSFER
```

Possible transaction statuses:

```text
PENDING

SUCCESS

FAILED
```

Example endpoints:

```http
POST /api/transactions/deposit

POST /api/transactions/withdraw

POST /api/transactions/transfer

GET /api/transactions/{transactionId}

GET /api/transactions/account/{accountId}
```

Example transfer request:

```json
{
  "fromAccountId": 1001,
  "toAccountId": 2001,
  "amount": 500.00
}
```

Typical transfer flow:

```text
Client
   |
   v
Gateway
   |
   v
Transaction Service
   |
   +------> Validate source account
   |
   +------> Validate destination account
   |
   +------> Validate amount
   |
   +------> Process transaction
   |
   +------> Store transaction
   |
   +------> Publish notification/audit event
```

Financial operations require additional production controls beyond a
demonstration project, particularly around concurrency, idempotency,
transaction consistency, authorization, and auditability.

---

## 2.6 Beneficiary Service

Port:

```text
8085
```

The Beneficiary Service manages saved payees or transfer recipients.

Responsibilities:

- Add beneficiary
- Retrieve beneficiary
- Retrieve customer's beneficiaries
- Update beneficiary
- Delete beneficiary
- Validate beneficiary information

Possible beneficiary information:

```text
beneficiaryId
customerId
beneficiaryName
accountNumber
bankName
status
createdAt
```

Example endpoints:

```http
POST /api/beneficiaries

GET /api/beneficiaries/{id}

GET /api/beneficiaries/customer/{customerId}

PUT /api/beneficiaries/{id}

DELETE /api/beneficiaries/{id}
```

---

## 2.7 Notification Service

Port:

```text
8086
```

The Notification Service is responsible for sending customer notifications.

Examples:

- Registration confirmation
- Login/security alert
- Account creation notification
- Deposit notification
- Withdrawal notification
- Transfer confirmation
- Failed transaction notification

Possible communication channels:

```text
Email

SMS

Push Notification
```

A future version can use asynchronous messaging.

Example:

```text
Transaction Service
        |
        | TransactionCompleted event
        v
Message Broker
        |
        v
Notification Service
        |
        v
Email / SMS
```

This prevents the transaction API from having to wait for an email or SMS
provider before returning a response.

---

## 2.8 Audit Service

Port:

```text
8087
```

The Audit Service maintains important business and security audit events.

Examples:

```text
USER_REGISTERED

LOGIN_SUCCESS

LOGIN_FAILED

CUSTOMER_UPDATED

ACCOUNT_CREATED

ACCOUNT_BLOCKED

TRANSFER_CREATED

TRANSFER_COMPLETED

TRANSFER_FAILED
```

Audit records can contain information such as:

```text
eventId
eventType
userId
serviceName
resourceId
timestamp
result
correlationId
```

Sensitive values should not be stored in logs.

Never log:

```text
Raw passwords

JWT tokens

Private encryption keys

Database credentials
```

---

## 2.9 Discovery Service

Port:

```text
8761
```

The Discovery Service is optional during the first stage of development.

Initially the Gateway can use:

```text
http://localhost:8081
http://localhost:8082
http://localhost:8083
```

As the platform grows, service discovery can remove some hardcoded service
locations.

Conceptually:

```text
Auth Service --------\
Customer Service -----\
Account Service ------- > Discovery Service
Transaction Service ---/
```

The Gateway can then discover registered services instead of relying only on
hardcoded addresses.

---

# 3. Service Ports

| Service | Port | Purpose |
|---------|------|---------|
| Gateway Service | 8080 | Main API entry point |
| Auth Service | 8081 | Authentication |
| Customer Service | 8082 | Customer management |
| Account Service | 8083 | Bank account management |
| Transaction Service | 8084 | Financial transactions |
| Beneficiary Service | 8085 | Beneficiary management |
| Notification Service | 8086 | Notifications |
| Audit Service | 8087 | Audit events |
| Discovery Service | 8761 | Service discovery |

---

# 4. Technology Stack

## Backend

```text
Java 21
Spring Boot
Spring Cloud Gateway
Spring Security
Spring Data JPA
Hibernate
Maven
```

## Security

```text
Spring Security
JWT Authentication
BCrypt Password Hashing
Role-Based Authorization
```

## Database

The project can use:

```text
MySQL

or

PostgreSQL
```

Each business service should own its data rather than directly reading and
writing another service's tables.

Example:

```text
Auth Service
     |
     v
Auth Database


Customer Service
     |
     v
Customer Database


Account Service
     |
     v
Account Database


Transaction Service
     |
     v
Transaction Database
```

---

# 5. Repository Structure

```text
gst-banking-platform/
│
├── README.md
│
├── .gitignore
│
├── docker-compose.yml
│
├── docs/
│
│   ├── architecture/
│
│   └── api/
│
├── gateway-service/
│   ├── pom.xml
│   └── src/
│
├── auth-service/
│   ├── pom.xml
│   └── src/
│
├── customer-service/
│   ├── pom.xml
│   └── src/
│
├── account-service/
│   ├── pom.xml
│   └── src/
│
├── transaction-service/
│   ├── pom.xml
│   └── src/
│
├── beneficiary-service/
│   ├── pom.xml
│   └── src/
│
├── notification-service/
│   ├── pom.xml
│   └── src/
│
├── audit-service/
│   ├── pom.xml
│   └── src/
│
└── discovery-service/
    ├── pom.xml
    └── src/
```

---

# 6. Internal Service Structure

A typical business microservice can follow:

```text
src/main/java/
└── api/
    └── customer_service/
        │
        ├── config/
        │
        ├── controller/
        │
        ├── dto/
        │
        ├── entity/
        │
        ├── exception/
        │
        ├── repository/
        │
        ├── service/
        │
        └── CustomerServiceApplication.java
```

Responsibilities:

```text
controller
    Receives HTTP requests.

dto
    Defines API request/response objects.

entity
    Represents database entities.

repository
    Handles database access.

service
    Contains business logic.

exception
    Contains custom and global exception handling.

config
    Contains service configuration.
```

---

# 7. Authentication Flow

Registration:

```text
Client
   |
   | POST /api/auth/register
   v
Gateway
   |
   v
Auth Service
   |
   +--> Validate request
   |
   +--> Check duplicate username/email
   |
   +--> BCrypt password
   |
   +--> Store user
   |
   v
Registration response
```

Login:

```text
Client
   |
   | POST /api/auth/login
   v
Gateway
   |
   v
Auth Service
   |
   +--> AuthenticationManager
   |
   +--> AuthenticationProvider
   |
   +--> UserDetailsService
   |
   +--> Validate password
   |
   +--> Generate JWT
   |
   v
JWT returned
```

---

# 8. Protected API Flow

After login, the client sends:

```http
Authorization: Bearer <JWT>
```

Example:

```text
GET /api/accounts/1001
```

Conceptual flow:

```text
Client
   |
   | Authorization: Bearer JWT
   v
Gateway
   |
   v
Authentication/Security
   |
   +---- Invalid ----> 401 Unauthorized
   |
   +---- Valid
           |
           v
      Account Service
           |
           v
       Account Data
```

Authentication and authorization responsibilities must be designed carefully:
downstream services should not blindly trust user-controlled identity headers.

---

# 9. Exception Handling

The APIs should return consistent error responses.

Example:

```json
{
  "timestamp": "2026-09-29T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Customer not found",
  "path": "/api/customers/100"
}
```

Recommended HTTP statuses:

```text
200 OK
    Successful request

201 Created
    Resource successfully created

400 Bad Request
    Invalid request or validation failure

401 Unauthorized
    Missing or invalid authentication

403 Forbidden
    Authenticated user does not have permission

404 Not Found
    Requested resource does not exist

409 Conflict
    Duplicate/conflicting resource

500 Internal Server Error
    Unexpected server-side failure
```

---

# 10. API Gateway Routes

Example routing:

```text
/api/auth/**
        |
        v
auth-service:8081


/api/customers/**
        |
        v
customer-service:8082


/api/accounts/**
        |
        v
account-service:8083


/api/transactions/**
        |
        v
transaction-service:8084


/api/beneficiaries/**
        |
        v
beneficiary-service:8085
```

Clients therefore use:

```text
http://localhost:8080/api/...
```

instead of needing to know every internal service port.

---

# 11. Running the Application

## Prerequisites

Install:

```text
Java 21
Maven
MySQL/PostgreSQL
Git
Postman
```

Check Java:

```bash
java -version
```

Check Maven:

```bash
mvn -version
```

---

# 12. Start Services

Start the required databases first.

Then start Auth Service:

```bash
cd auth-service
mvnw.cmd spring-boot:run
```

Start Customer Service:

```bash
cd customer-service
mvnw.cmd spring-boot:run
```

Start Account Service:

```bash
cd account-service
mvnw.cmd spring-boot:run
```

Start Transaction Service:

```bash
cd transaction-service
mvnw.cmd spring-boot:run
```

Finally start Gateway:

```bash
cd gateway-service
mvnw.cmd spring-boot:run
```

The primary client URL is:

```text
http://localhost:8080
```

---

# 13. Testing Authentication

## Register User

```http
POST http://localhost:8080/api/auth/register
```

Request:

```json
{
  "username": "john",
  "email": "john@example.com",
  "password": "StrongPassword123!"
}
```

## Login

```http
POST http://localhost:8080/api/auth/login
```

Request:

```json
{
  "email": "john@example.com",
  "password": "StrongPassword123!"
}
```

A successful authentication request returns a JWT.

For protected endpoints, send:

```http
Authorization: Bearer <JWT>
```

---

# 14. Recommended Development Order

Do not build every service simultaneously.

Recommended order:

```text
Phase 1
    Auth Service
        |
        v
    Gateway Service


Phase 2
    Customer Service


Phase 3
    Account Service


Phase 4
    Transaction Service


Phase 5
    Beneficiary Service


Phase 6
    Notification Service


Phase 7
    Audit Service


Phase 8
    Service Discovery


Phase 9
    Docker + Messaging + Monitoring
```

The first complete business flow should be:

```text
Register
   |
   v
Login
   |
   v
Create Customer
   |
   v
Create Account
   |
   v
Deposit
   |
   v
Transfer
   |
   v
Transaction History
```

---

# 15. Future Enhancements

Future improvements can include:

- Docker and Docker Compose
- Service discovery
- Centralized configuration
- Kafka or RabbitMQ
- Redis caching
- OpenAPI/Swagger documentation
- Distributed tracing
- Centralized logging
- Prometheus
- Grafana
- Rate limiting
- Circuit breakers
- Retry policies
- Idempotency keys
- Database migrations with Flyway/Liquibase
- Testcontainers
- CI/CD
- Kubernetes deployment
- Cloud deployment

---

# 16. Security Considerations

This project should follow basic security practices:

- Never store plaintext passwords
- Hash passwords using BCrypt
- Never commit JWT signing secrets
- Never commit database passwords
- Store secrets in environment variables or a secret manager
- Validate all incoming requests
- Use HTTPS outside local development
- Implement authorization in addition to authentication
- Avoid logging passwords and JWTs
- Protect administrative APIs
- Apply least-privilege database access
- Validate account ownership before financial operations

---

# 17. Important Disclaimer

This project is intended as a learning/reference implementation.

A real banking system requires substantially stronger controls around
authentication, authorization, encryption, transaction consistency,
fraud detection, regulatory compliance, auditability, high availability,
disaster recovery, secrets management, monitoring, and financial ledger
design.

---

# Author

GST Banking Platform

Backend Microservices Project built with Java and Spring.
