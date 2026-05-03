package com.sharepay.aggregator.modules.payment.repository;

import com.sharepay.aggregator.modules.payment.model.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentProviderRepository extends JpaRepository<PaymentProvider, UUID> {

    Optional<PaymentProvider> findByCode(String code);

    List<PaymentProvider> findByIsActiveTrueOrderByNameAsc();

    List<PaymentProvider> findByCountryAndIsActiveTrueOrderByNameAsc(String country);

    List<PaymentProvider> findByCurrencyAndIsActiveTrueOrderByNameAsc(String currency);
    long countByIsActiveTrue();
}
