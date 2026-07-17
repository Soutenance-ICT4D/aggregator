# Diagramme de classes — SharePay Aggregator

Modèle du domaine tel qu'il existe aujourd'hui dans le code : les **13 entités JPA**
du backend et leurs associations, réparties en **5 modules**.

> Toutes les entités portent `id: UUID`, `createdAt` et `updatedAt` — omis ci-dessous
> pour la lisibilité. Les énumérations sont persistées en `EnumType.STRING`.
>
> Relevé des classes `@Entity` de `com.sharepay.aggregator.modules.*` au 17 juillet 2026.

## Modules

| Module | Entités |
|--------|---------|
| **account** | `User` · `OtpCode` · `RefreshToken` · `WithdrawalAccount` · `WithdrawalConfig` |
| **apps** | `Application` · `ApiKey` |
| **payment** | `PaymentProvider` · `TransactionIn` · `TransactionOut` · `UserBalance` |
| **collect** | `FundCollection` |
| **webhook** | `WebhookDelivery` |

Le module `admin` ne définit aucune entité propre : il opère sur `User` filtré par le rôle `MERCHANT`.

## Diagramme

```mermaid
classDiagram
  direction TB

  namespace account {
    class User {
      +UUID id
      +String fullName
      +String email
      +String phone
      +String country
      +AuthProvider provider
      +Role role
      +AccountStatus status
      +KycLevel kycLevel
      +boolean emailVerified
    }
    class OtpCode {
      +UUID id
      +String codeHash
      +OtpPurpose purpose
      +OffsetDateTime expiresAt
    }
    class RefreshToken {
      +UUID id
      +String tokenHash
      +UUID familyId
      +boolean isRevoked
      +OffsetDateTime expiresAt
    }
    class WithdrawalAccount {
      +UUID id
      +String providerCode
      +String accountNumber
      +String accountName
      +boolean isDefault
    }
    class WithdrawalConfig {
      +UUID id
      +WithdrawalMode mode
      +Long thresholdAmount
      +WithdrawalPeriod period
      +int consecutiveFailures
    }
  }

  namespace apps {
    class Application {
      +UUID id
      +String name
      +String currency
      +String webhookUrl
      +AppStatus status
    }
    class ApiKey {
      +UUID id
      +String name
      +String keyPrefix
      +ApiKeyEnvironment environment
      +boolean isActive
    }
  }

  namespace payment {
    class PaymentProvider {
      +UUID id
      +String code
      +String name
      +PaymentProviderType type
      +BigDecimal feePercentage
      +Long feeFixed
      +boolean isActive
    }
    class TransactionIn {
      +UUID id
      +String reference
      +TransactionInType type
      +Long amount
      +Long netAmount
      +TransactionStatus status
      +String payerAccount
    }
    class TransactionOut {
      +UUID id
      +String reference
      +Long amount
      +Long netAmount
      +TransactionStatus status
      +String beneficiaryName
    }
    class UserBalance {
      +UUID id
      +String currency
      +Long availableAmount
      +Long pendingAmount
    }
  }

  namespace collect {
    class FundCollection {
      +UUID id
      +String slug
      +String title
      +Long amount
      +FundCollectionStatus status
    }
  }

  namespace webhook {
    class WebhookDelivery {
      +UUID id
      +String eventName
      +WebhookDeliveryStatus status
      +int attemptCount
      +OffsetDateTime nextRetryAt
    }
  }

  User "1" --> "0..*" Application : possède
  User "1" --> "0..*" UserBalance : détient
  User "1" --> "0..*" TransactionOut : retire
  User "1" --> "0..*" OtpCode
  User "1" --> "0..*" RefreshToken
  User "1" --> "0..*" WithdrawalAccount
  User "1" --> "0..1" WithdrawalConfig
  WithdrawalConfig "0..*" --> "0..1" WithdrawalAccount : cible

  Application "1" --> "0..*" ApiKey : expose
  Application "1" --> "0..*" TransactionIn : encaisse
  Application "0..1" --> "0..*" TransactionOut
  Application "1" --> "0..*" FundCollection
  Application "1" --> "0..*" WebhookDelivery

  PaymentProvider "0..1" --> "0..*" TransactionIn : traite
  PaymentProvider "0..1" --> "0..*" TransactionOut : traite
  FundCollection "0..1" --> "0..*" TransactionIn : source
```

## Points de lecture

- **Deux hubs** structurent le modèle : `User` (le compte) et `Application` (le point d'intégration).
- Les **encaissements** (`TransactionIn`) sont rattachés à l'`Application`, **pas** directement au `User` — d'où le passage par `application.user` dans les requêtes admin.
- Les **retraits** (`TransactionOut`) sont, eux, rattachés directement au `User`.
- `PaymentProvider` est un **référentiel transverse** (MTN / Orange) relié en `0..1` aux deux types de transactions.

## Énumérations (14)

| Énumération | Module | Valeurs |
|-------------|--------|---------|
| `Role` | account | `ADMIN`, `MERCHANT`, `SUPPORT` |
| `AccountStatus` | account | `ACTIVE`, `PENDING_VERIFICATION`, `SUSPENDED`, `DELETED` |
| `KycLevel` | account | `NONE`, `BASIC`, `VERIFIED`, `ADVANCED` |
| `AuthProvider` | account | `LOCAL`, `GOOGLE` |
| `OtpPurpose` | account | `EMAIL_VERIFICATION`, `PASSWORD_RESET` |
| `WithdrawalMode` | account | `MANUAL`, `INSTANT`, `THRESHOLD`, `PERIODIC` |
| `WithdrawalPeriod` | account | `DAILY`, `WEEKLY`, `MONTHLY` |
| `AppStatus` | apps | `ACTIVE`, `SUSPENDED`, `DELETED` |
| `ApiKeyEnvironment` | apps | `LIVE`, `TEST` |
| `PaymentProviderType` | payment | `MOBILE_MONEY`, `BANK_CARD`, `BANK_TRANSFER` |
| `TransactionInType` | payment | `CHECKOUT`, `CHARGE`, `FUND_COLLECTION` |
| `TransactionStatus` | payment | `PENDING`, `SUCCESS`, `FAILED`, `CANCELLED`, `REFUNDED` |
| `FundCollectionStatus` | collect | `ACTIVE`, `CLOSED`, `EXPIRED`, `DELETED` |
| `WebhookDeliveryStatus` | webhook | `PENDING`, `DELIVERED`, `FAILED` |
