package com.grivaslorente.banking_engine.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

    @Modifying
    @Query(value = """
            INSERT INTO idempotency_keys (
                idempotency_key,
                source_account_id,
                destination_account_id,
                amount
            )
            VALUES (:key, :sourceId, :destinationId, :amount)
            ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("key") String key,
            @Param("sourceId") UUID sourceId,
            @Param("destinationId") UUID destinationId,
            @Param("amount") BigDecimal amount
    );
}
