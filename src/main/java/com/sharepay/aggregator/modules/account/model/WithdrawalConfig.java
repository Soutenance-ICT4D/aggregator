package com.sharepay.aggregator.modules.account.model;

import com.sharepay.aggregator.shared.constant.WithdrawalMode;
import com.sharepay.aggregator.shared.constant.WithdrawalPeriod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "withdrawal_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawalConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WithdrawalMode mode = WithdrawalMode.MANUAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private WithdrawalAccount account;

    @Column(name = "threshold_amount")
    private Long thresholdAmount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private WithdrawalPeriod period;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "XAF";

    @Column(name = "last_triggered_at")
    private OffsetDateTime lastTriggeredAt;

    @Column(name = "consecutive_failures", nullable = false)
    @Builder.Default
    private int consecutiveFailures = 0;

    @Column(name = "last_error_at")
    private OffsetDateTime lastErrorAt;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;
}
