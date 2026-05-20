package com.sharepay.aggregator.modules.payment.repository;

import com.sharepay.aggregator.modules.payment.model.UserBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserBalanceRepository extends JpaRepository<UserBalance, UUID> {

    Optional<UserBalance> findByUser_IdAndCurrency(UUID userId, String currency);

    List<UserBalance> findByUser_IdOrderByCurrencyAsc(UUID userId);

    /** Incrémente le montant disponible de manière atomique (utilisé lors d'un paiement entrant réussi). */
    @Modifying
    @Query("""
            UPDATE UserBalance b
            SET b.availableAmount = b.availableAmount + :amount,
                b.pendingAmount   = b.pendingAmount   - :amount
            WHERE b.user.id = :userId AND b.currency = :currency
            """)
    int confirmPendingToAvailable(@Param("userId") UUID userId,
                                  @Param("currency") String currency,
                                  @Param("amount") long amount);

    /** Déplace un montant de available vers pending (initiation d'un payout). */
    @Modifying
    @Query("""
            UPDATE UserBalance b
            SET b.availableAmount = b.availableAmount - :amount,
                b.pendingAmount   = b.pendingAmount   + :amount
            WHERE b.user.id = :userId AND b.currency = :currency
              AND b.availableAmount >= :amount
            """)
    int moveAvailableToPending(@Param("userId") UUID userId,
                               @Param("currency") String currency,
                               @Param("amount") long amount);

    /** Annule un payout en échec : remet le montant de pending vers available. */
    @Modifying
    @Query("""
            UPDATE UserBalance b
            SET b.availableAmount = b.availableAmount + :amount,
                b.pendingAmount   = b.pendingAmount   - :amount
            WHERE b.user.id = :userId AND b.currency = :currency
            """)
    int rollbackPendingToAvailable(@Param("userId") UUID userId,
                                   @Param("currency") String currency,
                                   @Param("amount") long amount);

    /** Crédite le solde disponible lors d'un pay-in confirmé (atomique). */
    @Modifying
    @Query("""
            UPDATE UserBalance b
            SET b.availableAmount = b.availableAmount + :amount
            WHERE b.user.id = :userId AND b.currency = :currency
            """)
    int creditAvailableAmount(@Param("userId") UUID userId,
                              @Param("currency") String currency,
                              @Param("amount") long amount);

    /**
     * Crédite le solde disponible, en créant la ligne si elle n'existe pas.
     * Utilise un UPSERT natif PostgreSQL pour garantir l'atomicité.
     */
    @Modifying
    @Query(value = """
            INSERT INTO user_balances (id, user_id, currency, available_amount, pending_amount, created_at, updated_at)
            VALUES (gen_random_uuid(), :userId, :currency, :amount, 0, NOW(), NOW())
            ON CONFLICT (user_id, currency) DO UPDATE
            SET available_amount = user_balances.available_amount + EXCLUDED.available_amount,
                updated_at = NOW()
            """, nativeQuery = true)
    void upsertCreditAvailableAmount(@Param("userId") UUID userId,
                                     @Param("currency") String currency,
                                     @Param("amount") long amount);

    /** Décrémente le montant en attente lors d'un pay-out confirmé (atomique). */
    @Modifying
    @Query("""
            UPDATE UserBalance b
            SET b.pendingAmount = b.pendingAmount - :amount
            WHERE b.user.id = :userId AND b.currency = :currency
              AND b.pendingAmount >= :amount
            """)
    int decrementPendingAmount(@Param("userId") UUID userId,
                               @Param("currency") String currency,
                               @Param("amount") long amount);
}
