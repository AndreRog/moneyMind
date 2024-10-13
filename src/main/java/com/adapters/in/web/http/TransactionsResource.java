package com.adapters.in.web.http;

import com.app.domain.banks.ListBanks;
import com.app.domain.transactions.ImportTransactions;
import com.app.domain.transactions.ListTransactions;
import com.app.domain.core.FinancialRecord;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Path("/transactions")
public class TransactionsResource {

    private final ImportTransactions importTransactions;
    private final ListTransactions listTransactions;

    public TransactionsResource(ImportTransactions importTransactions, ListTransactions listTransactions,
                                ListBanks listBanks) {
        this.importTransactions = importTransactions;
        this.listTransactions = listTransactions;
    }


    @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importTransactions(@RestForm("type") String type, @RestForm("file") InputStream file) throws IOException {
        this.importTransactions.execute(type, file);
        return Response.noContent().build();
    }

    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() throws IOException {
        List<FinancialRecord> records = this.listTransactions.execute();
        return Response.ok(records).build();
    }
}
