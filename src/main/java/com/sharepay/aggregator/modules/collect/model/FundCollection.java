package com.sharepay.aggregator.modules.collect.model;

import com.sharepay.aggregator.modules.apps.model.Application;
import com.sharepay.aggregator.shared.constant.FundCollectionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fund_collections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundCollection {

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
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    private String coverImageUrl;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "XAF";

    @Column(name = "is_amount_fixed", nullable = false)
    @Builder.Default
    private boolean isAmountFixed = false;

    @Column
    private Long amount;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FundCollectionStatus status = FundCollectionStatus.ACTIVE;

    @Column(name = "collect_customer_info", nullable = false)
    @Builder.Default
    private boolean collectCustomerInfo = true;

    @Column(name = "thank_you_message", columnDefinition = "TEXT")
    private String thankYouMessage;
}
