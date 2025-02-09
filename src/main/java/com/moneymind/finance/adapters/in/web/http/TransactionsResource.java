package com.moneymind.finance.adapters.in.web.http;

import com.moneymind.finance.adapters.in.web.http.dto.UpdateCategoryRequest;
import com.moneymind.finance.adapters.in.web.http.hateoas.Link;
import com.moneymind.finance.domain.SearchResponse;
import com.moneymind.finance.domain.transactions.ClassifyTransactions;
import com.moneymind.finance.domain.transactions.ImportTransactions;
import com.moneymind.finance.domain.transactions.SearchTransactions;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.transactions.UpdateTransactions;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.RestForm;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Path("/transactions")
public class TransactionsResource {

    private final ImportTransactions importTransactions;
    private final SearchTransactions searchTransactions;
    private final UpdateTransactions updateTransactions;
    private final ClassifyTransactions classifyTransactions;

    public TransactionsResource(ImportTransactions importTransactions, SearchTransactions searchTransactions,
                                UpdateTransactions updateTransactions, ClassifyTransactions classifyTransactions) {
        this.importTransactions = importTransactions;
        this.searchTransactions = searchTransactions;
        this.updateTransactions = updateTransactions;
        this.classifyTransactions = classifyTransactions;

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
    public Response search(
            @Context UriInfo uriInfo,
            @QueryParam("id") String transactionId,
            @QueryParam("category") String category,
            @QueryParam("dimension") String dimension,
            @QueryParam("bank") String bank,
            @QueryParam("from") String from,
            @QueryParam("to") String to,
            @QueryParam("limit") int limit,
            @QueryParam("cursor") String cursor,
            @QueryParam("sort") String sort
    ) {
        final SearchResponse<FinancialRecord> searchResponse = this.searchTransactions.execute(
                transactionId, category, dimension, bank, from, to, 100, //TODO: support limit
                cursor, sort
        );

        return Response.ok(toPageResponse(searchResponse, uriInfo, transactionId, category, bank, from, to, cursor)).build();
    }

    @GET
    @Path("/categories")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchCategories(
            @Context UriInfo uriInfo
    ){
        // Sure BUT THESE CATEGORIES SHOULD BE FOR TRAINING
        // TRAVEL -> FLIGHTS, ACCOMMODATION
        return Response.ok(List.of("HOUSING", "RESTAURANTS", "GROCERIES", "TRAVEL", "FLIGHTS", "ACCOMMODATION",
                                    "INCOME", "OTHERS", "NICO", "HEALTH & WELLNESS", "GYM", "PSYCHOLOGY", "SUBSCRIPTIONS",
                                    "NETFLIX", "PRIME", "APPLE", "NESPRESSO", "UTILITIES", "WATER", "ELECTRICITY",
                                    "APPLIANCES", "SALARY", "FUN MONEY", "INTERNET", "MOBILE", "TRANSFER BETWEEN ACCOUNTS",
                                    "FINANCIAL EXPENSES", "CAR", "GASOLINE", "CAR TOOL"
        )).build();
    }

    @PUT
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateTransaction(@PathParam("id") String id,
                                      UpdateCategoryRequest updateCategoryRequest) {
        final FinancialRecord record = this.updateTransactions.execute(id, updateCategoryRequest.category());

        if(record == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(record).build();
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response classifyTransaction() {
        CompletableFuture<Void> classifyTask = this.classifyTransactions.execute();
        return Response.accepted().build();
    }


    private Page<FinancialRecord> toPageResponse(SearchResponse<FinancialRecord> searchResponse, UriInfo uriInfo,
                                                 String transactionId, String category, String bank,
                                                 String from, String to, String cursor) {
        Map<String, String> parameters = new HashMap<>();

        // previous only if not first page
        if (transactionId != null && transactionId.isBlank()) {
            parameters.put("transactionId", transactionId);
        }
        if (category != null && category.isBlank()) {
            parameters.put("category", category);
        }
        if (bank != null && bank.isBlank()) {
            parameters.put("bank", bank);
        }
        if (from != null && from.isBlank()) {
            parameters.put("from", from);
        }
        if (to != null && to.isBlank()) {
            parameters.put("to", to);
        }

        return new Page<>(Link.buildNextLink(searchResponse, uriInfo, parameters), searchResponse.list());
    }
}
