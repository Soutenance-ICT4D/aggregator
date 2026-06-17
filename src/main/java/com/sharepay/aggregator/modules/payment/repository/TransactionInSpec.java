package com.sharepay.aggregator.modules.payment.repository;

import com.sharepay.aggregator.modules.apps.model.Application;
import com.sharepay.aggregator.modules.payment.model.TransactionIn;
import com.sharepay.aggregator.shared.constant.AppStatus;
import com.sharepay.aggregator.shared.constant.TransactionInType;
import com.sharepay.aggregator.shared.constant.TransactionStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TransactionInSpec {

    private TransactionInSpec() {}

    public static Specification<TransactionIn> forMerchant(
            UUID userId, TransactionStatus status, TransactionInType type,
            UUID appId, OffsetDateTime from, OffsetDateTime to) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Fetch associations only in the data query — not the count query
            boolean isCount = Long.class.equals(query.getResultType());
            Join<TransactionIn, Application> appJoin;
            if (!isCount) {
                root.fetch("paymentProvider", JoinType.LEFT);
                @SuppressWarnings("unchecked")
                Join<TransactionIn, Application> f =
                        (Join<TransactionIn, Application>) (Object) root.fetch("application", JoinType.INNER);
                appJoin = f;
                query.distinct(true);
            } else {
                appJoin = root.join("application", JoinType.INNER);
            }

            predicates.add(cb.equal(appJoin.get("user").get("id"), userId));
            predicates.add(cb.notEqual(appJoin.get("status"), AppStatus.DELETED));

            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (type   != null) predicates.add(cb.equal(root.get("type"),   type));
            if (appId  != null) predicates.add(cb.equal(appJoin.get("id"),  appId));
            if (from   != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to     != null) predicates.add(cb.lessThan(root.get("createdAt"), to));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
