package com.pcondori.nttdata.transactionmicroservice.service;

import com.pcondori.nttdata.transactionmicroservice.client.WebProductMicroservice;
import com.pcondori.nttdata.transactionmicroservice.exception.CustomException;
import com.pcondori.nttdata.transactionmicroservice.model.RequestTransaction;
import com.pcondori.nttdata.transactionmicroservice.model.RequestTransactionAccount;
import com.pcondori.nttdata.transactionmicroservice.model.dao.AccountDao;
import com.pcondori.nttdata.transactionmicroservice.model.dao.TransactionDao;
import com.pcondori.nttdata.transactionmicroservice.repository.TransaccionRepository;
import com.pcondori.nttdata.transactionmicroservice.utils.MapperTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired
    WebProductMicroservice productService;

    @Autowired
    MapperTransaction mapper;
    @Autowired
    TransaccionRepository transaccionRepository;

    public Mono<TransactionDao> retiroCuenta(RequestTransactionAccount request) {
        return productService.getAllAccountClient(request.getClientId())
                .filter(account -> account.getId().equals(request.getAccountId()))
                .switchIfEmpty(Mono.error(new CustomException(HttpStatus.NOT_FOUND, "No existe cuenta para ese cliente")))
                .filter(accountDao -> accountDao.getBalance() >= request.getAmount() && request.getAmount()>0)
                .switchIfEmpty(Mono.error(new CustomException(HttpStatus.NOT_FOUND, "Ingreso un monto invalido")))
                .single()
                .flatMap(account -> {
                            Double balanceNuevo = account.getBalance() - request.getAmount();
                            return productService.updateAccount(mapper.toRequestUpdateAccount(balanceNuevo,request.getClientId(), request.getAccountId()))
                                    .flatMap( ac ->transaccionRepository.save(mapper.retiroRequestToDao(request,request.getAmount())));
                        }
                );
    }

    public Mono<TransactionDao> depositoCuenta(RequestTransactionAccount request){
        return productService.getAllAccountClient(request.getClientId())
                .filter(account -> account.getId().equals(request.getAccountId()))
                .switchIfEmpty(Mono.error(new CustomException(HttpStatus.NOT_FOUND, "No existe cuenta para ese cliente")))
                .filter(accountDao -> 0 < request.getAmount() && accountDao.getBalance()>0)
                .switchIfEmpty(Mono.error(new CustomException(HttpStatus.NOT_FOUND, "Ingreso un monto invalido")))
                .single()
                .flatMap(account -> {
                            Double balanceNuevo = account.getBalance() + request.getAmount();
                            account.setBalance(balanceNuevo);
                            return productService.updateAccount(mapper.toRequestUpdateAccount(balanceNuevo,request.getClientId(), request.getAccountId()))
                                    .flatMap( ac ->transaccionRepository.save(mapper.depositoRequestToDao(request,request.getAmount())));
                        }
                );
    }


    public Mono<TransactionDao> postTransferencia(RequestTransaction request){
        return productService.getAllAccountClient(request.getClientId())
                .switchIfEmpty(Mono.error(new CustomException(HttpStatus.BAD_REQUEST,"No hay cuentas ligadas a este cliente")))
                .filter(account -> account.getId().equals(request.getTo()) || account.getId().equals(request.getFrom()))
                .collectList()
                .map( listAccount -> listAccount.stream().collect(Collectors.toMap(AccountDao::getId, cuenta -> cuenta)))
                .map(mapAccount -> {
                    AccountDao accountFrom = mapAccount.get(request.getFrom());
                    AccountDao accountTo = mapAccount.get(request.getTo());
                    //Actualizamos los balance
                    accountFrom.setBalance(accountFrom.getBalance() - request.getAmount());
                    accountTo.setBalance(accountTo.getBalance() + request.getAmount());
                    //Seteamos las cuentas actualizadas en el Map
                    mapAccount.put(request.getFrom(), accountFrom);
                    mapAccount.put(request.getTo(), accountTo);
                    return mapAccount;
                })
                .map(mapAccount -> new ArrayList<>(mapAccount.values()))
                .flatMap(listAccount -> Flux.fromIterable(listAccount)
                        .flatMap(account -> productService.updateAccount(mapper.toRequestUpdateAccount(account.getBalance(),request.getClientId(),account.getId())))
                        .then(transaccionRepository.save(mapper.transRequestToTransDao(request))));

    }

    public Flux<TransactionDao> getAllTransactionByClient(String clientId){
        return transaccionRepository.findAll()
                .filter(trans -> trans.getClientId().equals(clientId));

    }
}
