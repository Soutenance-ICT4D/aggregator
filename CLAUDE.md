# SharePay Aggregator — Spring Boot Backend

## Stack
- **Java 21**, Spring Boot 4.0.5
- PostgreSQL + Spring Data JPA + Flyway (migrations)
- Spring Security: JWT (`JwtAuthenticationFilter`) + API Key (`ApiKeyAuthenticationFilter`) + OAuth2 Resource Server
- WebClient (WebFlux) pour les appels aux gateways de paiement
- Lombok (`@RequiredArgsConstructor`, `@Slf4j`, `@Data`, `@Builder`)
- springdoc-openapi (Swagger UI)
- `springboot4-dotenv` pour les variables d'environnement depuis `.env`

## Architecture

```
src/main/java/com/sharepay/aggregator/
├── modules/
│   ├── account/        # Auth, Merchant, Withdrawal
│   ├── admin/          # Admin, Staff, Support
│   ├── apps/           # Applications & API Keys
│   ├── collect/        # Fund Collections
│   ├── notification/   # Email, SMS, Push
│   ├── payment/        # PayIn, PayOut, Providers, Balances
│   └── webhook/        # Webhook delivery & retry
└── shared/
    ├── config/         # Security, Swagger, Async, WebClient
    ├── constant/       # Enums (TransactionStatus, Role, KycLevel…)
    ├── dto/            # ApiResponse<T>, PaginationResponse<T>
    ├── events/         # Spring ApplicationEvents (async)
    ├── exception/      # BusinessException, GlobalExceptionHandler
    ├── gateway/        # PaymentGateway interface + MTN/Orange impls
    ├── security/       # JWT, API Key, Rate Limiting filters
    └── util/           # HashUtil, OtpUtil
```

### Conventions par module
Chaque module suit strictement la structure :
```
{module}/
├── controller/     # @RestController, routes /api/v1/...
├── dto/
│   ├── request/    # *Request.java avec @Valid
│   └── response/   # *Response.java
├── model/          # @Entity JPA
├── repository/     # @Repository extends JpaRepository (+ Spec si filtres)
└── service/
    ├── *Service.java       # interface
    └── impl/*ServiceImpl.java  # @Service @Transactional
```

## Patterns à respecter

### Service
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class FooServiceImpl implements FooService {

    private final FooRepository fooRepository;

    @Override
    @Transactional
    public FooResponse create(CreateFooRequest request) {
        // ...
        throw new BusinessException("FOO_CONFLICT", "Message", HttpStatus.CONFLICT);
    }
}
```

### Controller
```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/foo")
public class FooController {

    private final FooService fooService;

    @PostMapping
    public ResponseEntity<ApiResponse<FooResponse>> create(
            @Valid @RequestBody CreateFooRequest request) {
        return ResponseEntity.ok(ApiResponse.success(fooService.create(request)));
    }
}
```

### Erreurs métier
Toujours utiliser `BusinessException(code, message, HttpStatus)` — jamais lancer de `RuntimeException` brute. Le `GlobalExceptionHandler` gère la sérialisation en `ApiResponse`.

### Gateway de paiement
Toujours passer par `PaymentGatewayRegistry.getGateway(PaymentProviderType)` — ne jamais injecter `MtnMomoGateway` ou `OrangeMoneyGateway` directement.

### Flyway
- Les migrations vont dans `src/main/resources/db/migration/`
- Nommage : `V{N}__{description_snake_case}.sql`
- Ne jamais modifier une migration déjà appliquée

### Événements asynchrones
Publier via `ApplicationEventPublisher`, écouter avec `@EventListener` dans `modules/notification/listener/NotificationListener.java`.

## Rôles et sécurité
- `ROLE_MERCHANT` : accès `/api/v1/merchants/**`
- `ROLE_ADMIN` : accès `/api/v1/admin/**`
- `ROLE_SUPPORT` : accès `/api/v1/admin/support/**`
- Auth API Key : header `X-API-Key` → `ApiKeyAuthenticationFilter`
- Auth JWT : header `Authorization: Bearer <token>` → `JwtAuthenticationFilter`

## Commandes

```bash
# Développement (hot reload)
./mvnw spring-boot:run

# Build
./mvnw clean package -DskipTests

# Tests
./mvnw test

# Docker
docker compose up --build
```

## À ne pas faire
- Ne pas injecter des beans par `@Autowired` sur les champs — toujours utiliser le constructeur (`@RequiredArgsConstructor`)
- Ne pas utiliser `Optional.get()` sans `isPresent()` — préférer `orElseThrow(() -> new BusinessException(...))`
- Ne pas créer de méthode `public` dans un `ServiceImpl` qui n'est pas dans l'interface
- Ne pas écrire de logique métier dans les controllers
- Ne pas modifier les enums dans `shared/constant/` sans vérifier les migrations Flyway correspondantes
