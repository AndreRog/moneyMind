package com.adapters.in.web.http;

import com.adapters.in.web.http.hateoas.Link;
import com.app.domain.SearchResponse;
import com.app.domain.banks.ListBanks;
import com.app.domain.transactions.ImportTransactions;
import com.app.domain.transactions.SearchTransactions;
import com.app.domain.core.FinancialRecord;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.RestForm;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@Path("/transactions")
public class TransactionsResource {

    private final ImportTransactions importTransactions;
    private final SearchTransactions searchTransactions;

    public TransactionsResource(ImportTransactions importTransactions, SearchTransactions searchTransactions,
                                ListBanks listBanks) {
        this.importTransactions = importTransactions;
        this.searchTransactions = searchTransactions;
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
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response search(@Context UriInfo uriInfo,
                         @QueryParam("id") String transactionId,
                         @QueryParam("category") String category,
                         @QueryParam("bank") String bank,
                         @QueryParam("from") String from,
                         @QueryParam("to") String to,
                         @QueryParam("limit") int limit,
                         @QueryParam("cursor") String cursor
    ) throws IOException, URISyntaxException {
        final SearchResponse<FinancialRecord> searchResponse = this.searchTransactions.execute(
                transactionId, category,bank,from,to,limit,cursor
        );


        return Response.ok(toPageResponse(searchResponse, uriInfo, transactionId, category, bank, from, to, cursor)).build();
    }

    private Page<FinancialRecord> toPageResponse(SearchResponse<FinancialRecord> searchResponse, UriInfo uriInfo,
                                                 String transactionId, String category, String bank,
                                                 String from, String to, String cursor) throws URISyntaxException {
        Map<String, String> parameters = new HashMap<>();

        // previous only if not first page
        String previous = uriInfo.getRequestUri().toString();
        if(cursor == null || cursor.isBlank()) {
            previous = "";
        }

        if(transactionId != null && transactionId.isBlank()){
            parameters.put("transactionId", transactionId);
        }

        if(category != null && category.isBlank()){
            parameters.put("category", category);
        }
        if(bank != null && bank.isBlank()){
            parameters.put("bank", bank);
        }
        if(from != null && from.isBlank()){
            parameters.put("from", from);
        }

        if(to != null && to.isBlank()){
            parameters.put("to", to);
        }

        return new Page<>(Link.buildNextLink(searchResponse, uriInfo, parameters), new Link(previous), searchResponse.list());
    }


}
