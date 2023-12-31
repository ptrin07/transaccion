package com.pcondori.nttdata.transactionmicroservice.model.dao;

import com.pcondori.nttdata.transactionmicroservice.utils.ClientType;
import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class ClientDao {
    @Id
    private String id;
    private String typeDocument;
    private String nroDocument;
    private String name;
    private ClientType typeClient;
}
