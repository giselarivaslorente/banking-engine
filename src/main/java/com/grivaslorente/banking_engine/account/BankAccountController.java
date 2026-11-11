package com.grivaslorente.banking_engine.account;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import java.util.List;

import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/accounts")
public class BankAccountController {
    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BankAccount createAccount(@RequestBody CreateAccountRequest request) {
        return bankAccountService.createAccount(
                request.ownerName(),
                request.currency()
        );
    }

    @GetMapping("/{id}")
    public BankAccount getAccount(@PathVariable UUID id) {
        return bankAccountService.getAccount(id);
    }

    @PostMapping("/{id}/deposit")
    public BankAccount deposit(
            @PathVariable UUID id,
            @RequestBody DepositRequest request
    ) {
        return bankAccountService.deposit(id, request.amount());
    }


    @PostMapping("/{id}/withdraw")
    public BankAccount withdraw(
            @PathVariable UUID id,
            @RequestBody WithdrawRequest request
    ) {
        return bankAccountService.withdraw(id, request.amount());
    }



    @PostMapping("/{id}/transfer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transfer(
            @PathVariable UUID id,
            @RequestBody TransferRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        bankAccountService.transfer(
                id,
                request.destinationAccountId(),
                request.amount(),
                idempotencyKey
        );
    }



    @GetMapping("/{id}/transactions")
    public List<BankTransaction> getTransactionHistory(
            @PathVariable UUID id
    ) {
        return bankAccountService.getTransactionHistory(id);
    }


}
