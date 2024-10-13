package com.adapters.in.web.http;


import com.app.domain.banks.ListBanks;
import com.app.domain.core.FinancialRecord;
import com.app.domain.transactions.ImportTransactions;
import com.app.domain.transactions.ListTransactions;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.IOException;
import java.util.List;

@Path("/banks")
public class BanksResource {
    private final ListBanks listBanks;

    public BanksResource(ListBanks listBanks) {
        this.listBanks = listBanks;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() throws IOException {
        return Response.ok(this.listBanks.execute()).build();
    }

}
