package com.pcondori.nttdata.transactionmicroservice.repository;

import com.pcondori.nttdata.transactionmicroservice.model.dao.TransactionDao;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransaccionRepository extends ReactiveMongoRepository<TransactionDao,String> {
}
