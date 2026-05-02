package com.sharepay.aggregator.modules.payment.model;

import com.sharepay.aggregator.modules.apps.model.Application;
import com.sharepay.aggregator.modules.collect.model.FundCollection;
import com.sharepay.aggregator.shared.constant.TransactionInType;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions_in")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionIn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(nullable = false, unique = true, length = 100)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionInType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fund_collection_id")
    private FundCollection fundCollection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_provider_id")
    private PaymentProvider paymentProvider;

    @Column(name = "session_token", unique = true, length = 100)
    private String sessionToken;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "fee_amount", nullable = false)
    @Builder.Default
    private Long feeAmount = 0L;

    @Column(name = "net_amount", nullable = false)
    private Long netAmount;

    @Column(name = "provider_transaction_id", length = 255)
    private String providerTransactionId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "merchant_reference", length = 255)
    private String merchantReference;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "payer_account", length = 100)
    private String payerAccount;

    @Column(name = "payer_name", length = 255)
    private String payerName;

    @Column(name = "payer_email", length = 255)
    private String payerEmail;

    @Column(name = "success_url", length = 500)
    private String successUrl;

    @Column(name = "cancel_url", length = 500)
    private String cancelUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "failure_code", length = 50)
    private String failureCode;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Version
    @Column(nullable = false)
    private Long version;
}
