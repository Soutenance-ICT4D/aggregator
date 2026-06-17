package com.sharepay.aggregator.modules.payment.repository;

import com.sharepay.aggregator.modules.payment.model.TransactionOut;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TransactionOutSpec {

    private TransactionOutSpec() {}

    public static Specification<TransactionOut> forMerchant(
            UUID userId, TransactionStatus status, UUID appId,
            OffsetDateTime from, OffsetDateTime to) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            boolean isCount = Long.class.equals(query.getResultType());
            if (!isCount) {
                root.fetch("paymentProvider", JoinType.LEFT);
                root.fetch("application",     JoinType.LEFT);
                query.distinct(true);
            }

            predicates.add(cb.equal(root.get("user").get("id"), userId));
            if (status != null) predicates.add(cb.equal(root.get("status"),                  status));
            if (appId  != null) predicates.add(cb.equal(root.get("application").get("id"),   appId));
            if (from   != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to     != null) predicates.add(cb.lessThan(root.get("createdAt"),             to));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
