# Microservice Security Starter

A lightweight internal Spring Boot starter for propagating authenticated user context from an API Gateway to downstream microservices.

The starter provides:

* Request-level user context
* Authentication checks
* Role-based authorization
* `SecurityPrincipal` access
* Automatic servlet filter registration
* Automatic Spring MVC interceptor registration
* No Spring Security dependency in downstream microservices
* No `ThreadLocal` usage

---

## Architecture

The API Gateway is responsible for:

1. Validating the JWT.
2. Extracting user information from the JWT.
3. Forwarding the authenticated user context to downstream services using HTTP headers.

The security starter is responsible for:

1. Reading those headers.
2. Creating a `SecurityPrincipal`.
3. Making the principal available through `SecurityContext`.
4. Enforcing `@RequiresAuthentication`.
5. Enforcing `@RequiresRole`.

```text
Client
  │
  │ Authorization: Bearer <JWT>
  ▼
┌─────────────────────┐
│     API Gateway     │
│                     │
│  Validate JWT       │
│  Extract claims     │
└──────────┬──────────┘
           │
           │ X-User-Id
           │ X-User-Email
           │ X-User-Role
           ▼
┌────────────────────────────┐
│      Microservice          │
│                            │
│ SecurityContextFilter      │
│           ↓                │
│ SecurityContext            │
│           ↓                │
│ AuthorizationInterceptor   │
│           ↓                │
│ Controller                 │
└────────────────────────────┘
```

---

# Installation

Add the starter as a dependency to the microservice.

```xml
<dependency>
    <groupId>com.experiment</groupId>
    <artifactId>microservice-security-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

The starter is automatically configured by Spring Boot.

No explicit `@Import` is required.

No filter registration is required.

No interceptor registration is required.

---

# Requirements

The consuming microservice must use:

* Java 21+
* Spring Boot 4.x
* Spring MVC

The starter expects the API Gateway to provide the following headers for authenticated requests:

| Header         | Description                |
| -------------- | -------------------------- |
| `X-User-Id`    | Authenticated user's ID    |
| `X-User-Email` | Authenticated user's email |
| `X-User-Role`  | Authenticated user's role  |

---

# Automatic Configuration

Once the dependency is added, the starter automatically registers:

```text
SecurityContextFilter
SecurityContext
SecurityAuthorizationInterceptor
```

The filter is registered for:

```text
/*
```

with an order of:

```text
1
```

The consuming microservice does not need to manually register any of these components.

---

# Public Endpoints

Endpoints without security annotations are treated as public.

```java
@GetMapping("/{id}")
public InventoryResponse getInventory(
        @PathVariable Long id
) {
    return inventoryService.getInventory(id);
}
```

No authentication is required.

If no security headers are present, the request continues normally.

---

# Authentication

Use `@RequiresAuthentication` when an endpoint requires an authenticated user.

```java
@RequiresAuthentication
@GetMapping("/my")
public List<InventoryResponse> getMyInventory() {
    // ...
}
```

If the request does not contain a valid user context, the starter throws:

```text
SecurityUnauthorizedException
```

The expected HTTP status is:

```text
401 Unauthorized
```

---

# Role-Based Authorization

Use `@RequiresRole` when an endpoint requires a specific role.

```java
@RequiresRole(SecurityRole.SELLER)
@PostMapping
public InventoryResponse createInventory(
        @RequestBody CreateInventoryRequest request
) {
    // ...
}
```

`@RequiresRole` automatically requires authentication.

Therefore, this is unnecessary:

```java
@RequiresAuthentication
@RequiresRole(SecurityRole.SELLER)
```

Use only:

```java
@RequiresRole(SecurityRole.SELLER)
```

If the user is authenticated but does not have the required role, the starter throws:

```text
SecurityForbiddenException
```

The expected HTTP status is:

```text
403 Forbidden
```

---

# Accessing the Current User

Inject `SecurityContext` into a controller or service.

```java
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final SecurityContext securityContext;

    public OrderController(SecurityContext securityContext) {
        this.securityContext = securityContext;
    }

    @RequiresAuthentication
    @GetMapping("/my")
    public List<OrderResponse> getMyOrders() {

        SecurityPrincipal user =
                securityContext.require();

        return orderService.getOrdersForUser(
                user.userId()
        );
    }
}
```

---

# SecurityPrincipal

`SecurityPrincipal` contains the authenticated user's identity.

```java
public record SecurityPrincipal(
        String userId,
        String email,
        SecurityRole role
) {
}
```

The available values are:

```java
user.userId();
user.email();
user.role();
```

Example:

```java
SecurityPrincipal user =
        securityContext.require();

String userId = user.userId();
String email = user.email();
SecurityRole role = user.role();
```

---

# SecurityContext API

`SecurityContext` provides two main methods.

## `get()`

Returns the current principal or `null` if the request is unauthenticated.

```java
SecurityPrincipal user =
        securityContext.get();
```

Use this when authentication is optional.

---

## `require()`

Returns the current principal.

If the request is unauthenticated, it throws:

```text
SecurityUnauthorizedException
```

Example:

```java
SecurityPrincipal user =
        securityContext.require();
```

Use this when the operation requires an authenticated user.

---

## `isAuthenticated()`

Checks whether a principal is available.

```java
if (securityContext.isAuthenticated()) {
    // authenticated
}
```

---

# Exception Handling

The starter intentionally does not define a global REST exception handler.

Each consuming microservice should define its own `@RestControllerAdvice`.

Example:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SecurityUnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(
            SecurityUnauthorizedException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(
                        401,
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(SecurityForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(
            SecurityForbiddenException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ApiError(
                        403,
                        ex.getMessage()
                ));
    }
}
```

This allows each microservice to maintain its own API error-response format.

---

# Security Headers

The starter recognizes these headers:

```text
X-User-Id
X-User-Email
X-User-Role
```

For an authenticated request, all three headers must be present.

Example:

```http
X-User-Id: 123
X-User-Email: user@example.com
X-User-Role: SELLER
```

If only some of the headers are present, the request is rejected with:

```text
401 Unauthorized
```

An invalid role results in:

```text
403 Forbidden
```

---

# Role Values

The current starter supports:

```java
public enum SecurityRole {
    CUSTOMER,
    SELLER
}
```

Roles are parsed case-insensitively.

For example:

```text
SELLER
seller
 Seller
```

all resolve to:

```java
SecurityRole.SELLER
```

---

# Complete Example

```java
@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final SecurityContext securityContext;

    public InventoryController(
            InventoryService inventoryService,
            SecurityContext securityContext
    ) {
        this.inventoryService = inventoryService;
        this.securityContext = securityContext;
    }

    // Public
    @GetMapping("/{id}")
    public InventoryResponse getInventory(
            @PathVariable Long id
    ) {
        return inventoryService.getInventory(id);
    }

    // Authenticated users
    @RequiresAuthentication
    @GetMapping("/my")
    public List<InventoryResponse> getMyInventory() {

        SecurityPrincipal user =
                securityContext.require();

        return inventoryService.getByUserId(
                user.userId()
        );
    }

    // Sellers only
    @RequiresRole(SecurityRole.SELLER)
    @PostMapping
    public InventoryResponse createInventory(
            @RequestBody CreateInventoryRequest request
    ) {

        SecurityPrincipal user =
                securityContext.require();

        return inventoryService.createInventory(
                request,
                user.userId()
        );
    }
}
```

---

# Request Flow

## Public request

```text
Client
  ↓
Gateway
  ↓
Microservice
  ↓
SecurityContextFilter
  ↓
No security headers
  ↓
Controller
```

---

## Authenticated request

```text
Client
  ↓
Gateway
  ↓
JWT validation
  ↓
X-User-* headers
  ↓
SecurityContextFilter
  ↓
SecurityPrincipal
  ↓
SecurityAuthorizationInterceptor
  ↓
@RequiresAuthentication
  ↓
Controller
```

---

## Role-protected request

```text
Client
  ↓
Gateway
  ↓
JWT validation
  ↓
X-User-Role: SELLER
  ↓
SecurityContextFilter
  ↓
SecurityPrincipal
  ↓
SecurityAuthorizationInterceptor
  ↓
@RequiresRole(SELLER)
  ↓
Controller
```

---

# What the Starter Does NOT Handle

The starter does not:

* Validate JWTs
* Generate JWTs
* Authenticate credentials
* Manage users
* Manage sessions
* Store users in a database
* Define API error-response formats
* Use `ThreadLocal`
* Require Spring Security in downstream microservices

JWT validation and authentication remain responsibilities of the API Gateway/authentication layer.

---

# Microservice Responsibilities

A consuming microservice is responsible for:

```text
Controllers
Services
Repositories
Business logic
Business exceptions
Global exception handling
Database access
```

The starter is responsible for:

```text
User context propagation
Authentication checks
Role authorization
SecurityPrincipal
SecurityContext
Security annotations
```

---

# Package Structure

```text
com.experiment.microservicesecuritystarter
│
├── annotation
│   ├── RequiresAuthentication
│   └── RequiresRole
│
├── config
│   └── SecurityStarterAutoConfiguration
│
├── security
│   ├── SecurityPrincipal
│   ├── SecurityRole
│   ├── SecurityContext
│   ├── SecurityContextFilter
│   └── SecurityAuthorizationInterceptor
│
└── exception
    ├── SecurityUnauthorizedException
    └── SecurityForbiddenException
```