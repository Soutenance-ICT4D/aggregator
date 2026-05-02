package com.sharepay.aggregator.modules.payment.model;

import com.sharepay.aggregator.modules.account.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "user_balances",
        uniqueConstraints = @UniqueConstraint(name = "unique_user_currency", columnNames = {"user_id", "currency"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "XAF";

    @Column(name = "available_amount", nullable = false)
    @Builder.Default
    private Long availableAmount = 0L;

    @Column(name = "pending_amount", nullable = false)
    @Builder.Default
    private Long pendingAmount = 0L;
}
