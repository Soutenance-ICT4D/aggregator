package com.sharepay.aggregator.modules.payment.model;

import com.sharepay.aggregator.modules.account.model.User;
import com.sharepay.aggregator.modules.apps.model.Application;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions_out")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionOut {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_provider_id")
    private PaymentProvider paymentProvider;

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

    @Column(name = "beneficiary_name", nullable = false, length = 255)
    private String beneficiaryName;

    @Column(name = "beneficiary_email", length = 255)
    private String beneficiaryEmail;

    @Column(name = "beneficiary_account", length = 100)
    private String beneficiaryAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "failure_code", length = 50)
    private String failureCode;

    @Version
    @Column(nullable = false)
    private Long version;
}
