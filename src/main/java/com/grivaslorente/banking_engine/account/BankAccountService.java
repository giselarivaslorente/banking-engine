package com.grivaslorente.banking_engine.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.Locale;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import java.util.List;

import com.grivaslorente.banking_engine.idempotency.IdempotencyKeyRepository;
import com.grivaslorente.banking_engine.idempotency.IdempotencyKey;


@Service
public class BankAccountService {
    private final BankAccountRepository bankAccountRepository;
    private final BankTransactionRepository transactionRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;



    public BankAccountService(
            BankAccountRepository bankAccountRepository,
            BankTransactionRepository transactionRepository,
            IdempotencyKeyRepository idempotencyKeyRepository
    ) {
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
    }


    @Transactional
    public BankAccount createAccount(String ownerName, String currency) {

        if (ownerName == null || ownerName.isBlank()) {
            throw new IllegalArgumentException("Owner name cannot be empty");
        }

        ownerName = ownerName.trim();

        if (ownerName.length() > 100) {
            throw new IllegalArgumentException("Owner name is too long");
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency cannot be empty");
        }

        Currency validatedCurrency;

        try {
            validatedCurrency = Currency.getInstance(
                    currency.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid currency code");
        }

        BankAccount account = new BankAccount(
                ownerName,
                validatedCurrency.getCurrencyCode()
        );

        return bankAccountRepository.save(account);
    }


    @Transactional(readOnly = true)
    public BankAccount getAccount(UUID id) {
        return bankAccountRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Account not found"
                        )
                );
    }

    @Transactional
    public BankAccount deposit(UUID id, BigDecimal amount) {

        BankAccount account = bankAccountRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Account not found"
                        )
                );

        if (amount == null || amount.signum() <= 0
                || amount.scale() > 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid deposit amount"
            );
        }

        account.deposit(amount);

        BankAccount savedAccount = bankAccountRepository.save(account);

        transactionRepository.save(
                new BankTransaction(id, "DEPOSIT", amount)
        );

        return savedAccount;
    }


    @Transactional
    public BankAccount withdraw(UUID id, BigDecimal amount) {

        BankAccount account = bankAccountRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Account not found"
                        )
                );

        if (amount == null || amount.signum() <= 0
                || amount.scale() > 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid withdrawal amount"
            );
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Insufficient funds"
            );
        }

        account.withdraw(amount);

        BankAccount savedAccount = bankAccountRepository.save(account);

        transactionRepository.save(
                new BankTransaction(id, "WITHDRAWAL", amount)
        );

        return savedAccount;
    }


    @Transactional
    public void transfer(
            UUID sourceId,
            UUID destinationId,
            BigDecimal amount,
            String idempotencyKey
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Idempotency-Key"
            );
        }

        if (sourceId == null || destinationId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Account IDs cannot be null"
            );
        }

        if (sourceId.equals(destinationId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot transfer to the same account"
            );
        }

        if (amount == null || amount.signum() <= 0
                || amount.scale() > 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid transfer amount"
            );
        }


        int inserted = idempotencyKeyRepository.insertIfAbsent(
                idempotencyKey,
                sourceId,
                destinationId,
                amount
        );

        if (inserted == 0) {
            IdempotencyKey existing = idempotencyKeyRepository
                    .findById(idempotencyKey)
                    .orElseThrow();

            boolean sameTransfer =
                    existing.getSourceAccountId().equals(sourceId)
                            && existing.getDestinationAccountId().equals(destinationId)
                            && existing.getAmount().compareTo(amount) == 0;

            if (!sameTransfer) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Idempotency-Key already used for a different transfer"
                );
            }

            // This exact transfer was already completed
            return;
        }

        UUID firstId;
        UUID secondId;

        if (sourceId.compareTo(destinationId) < 0) {
            firstId = sourceId;
            secondId = destinationId;
        } else {
            firstId = destinationId;
            secondId = sourceId;
        }

        BankAccount firstAccount = bankAccountRepository
                .findByIdForUpdate(firstId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found"
                ));

        BankAccount secondAccount = bankAccountRepository
                .findByIdForUpdate(secondId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found"
                ));

        BankAccount source = sourceId.equals(firstId)
                ? firstAccount : secondAccount;

        BankAccount destination = destinationId.equals(firstId)
                ? firstAccount : secondAccount;


        if (!source.getCurrency().equals(destination.getCurrency())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Accounts must have the same currency"
            );
        }

        if (source.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Insufficient funds"
            );
        }

        source.withdraw(amount);
        destination.deposit(amount);

        bankAccountRepository.save(source);
        bankAccountRepository.save(destination);

        transactionRepository.save(
                new BankTransaction(sourceId, "TRANSFER_OUT", amount)
        );

        transactionRepository.save(
                new BankTransaction(destinationId, "TRANSFER_IN", amount)
        );
    }

    @Transactional(readOnly = true)
    public List<BankTransaction> getTransactionHistory(UUID accountId) {

        if (!bankAccountRepository.existsById(accountId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Account not found"
            );
        }

        return transactionRepository
                .findByAccountIdOrderByCreatedAtDesc(accountId);
    }



}
