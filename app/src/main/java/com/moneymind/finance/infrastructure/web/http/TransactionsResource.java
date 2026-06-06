package com.moneymind.finance.infrastructure.web.http;

import com.moneymind.finance.domain.core.ClassifiedFinancialRecord;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.core.SearchResult;
import com.moneymind.finance.domain.core.TransactionSearchQuery;
import com.moneymind.finance.domain.transactions.ExportTransactions;
import com.moneymind.finance.domain.transactions.GetTransaction;
import com.moneymind.finance.domain.transactions.ImportTransactions;
import com.moneymind.finance.domain.transactions.SearchTransactions;
import com.moneymind.finance.domain.transactions.UpdateTransactions;
import com.moneymind.finance.infrastructure.web.http.dto.UpdateCategoryRequest;
import com.moneymind.finance.infrastructure.web.http.hateoas.Link;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.RestForm;

import java.io.InputStream;
import java.util.List;

@Path("/transactions")
public class TransactionsResource {

    private final ImportTransactions importTransactions;
    private final SearchTransactions searchTransactions;
    private final ExportTransactions exportTransactions;
    private final UpdateTransactions updateTransactions;
    private final GetTransaction getTransaction;

    public TransactionsResource(ImportTransactions importTransactions, SearchTransactions searchTransactions,
                                ExportTransactions exportTransactions, UpdateTransactions updateTransactions,
                                GetTransaction getTransaction) {
        this.importTransactions = importTransactions;
        this.searchTransactions = searchTransactions;
        this.exportTransactions = exportTransactions;
        this.updateTransactions = updateTransactions;
        this.getTransaction = getTransaction;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importTransactions(@RestForm("type") String type, @RestForm("file") InputStream file) {
        try {
            List<ClassifiedFinancialRecord> records = this.importTransactions.execute(type, file);
            return Response.status(Response.Status.CREATED).entity(records).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response search(
            @Context UriInfo uriInfo,
            @QueryParam("id") String transactionId,
            @QueryParam("category") String category,
            @QueryParam("excludeCategory") List<String> excludeCategory,
            @QueryParam("aggregateByPeriod") String aggregateByPeriod,
            @QueryParam("aggregateByColumn") String aggregateByColumn,
            @QueryParam("bank") String bank,
            @QueryParam("from") String from,
            @QueryParam("to") String to,
            @QueryParam("limit") int limit,
            @QueryParam("cursor") String cursor,
            @QueryParam("sort") String sort
    ) {
        int effectiveLimit = limit <= 0 ? 50 : Math.min(limit, 100);

        TransactionSearchQuery query = TransactionSearchQuery.builder()
                .id(transactionId)
                .category(category)
                .excludeCategories(excludeCategory)
                .aggregateByPeriod(aggregateByPeriod)
                .aggregateByColumn(aggregateByColumn)
                .bank(bank)
                .from(from)
                .to(to)
                .limit(effectiveLimit)
                .cursor(cursor)
                .sort(sort)
                .build();

        SearchResult result = this.searchTransactions.execute(query);

        return switch (result) {
            case SearchResult.Records r -> Response.ok(new Page<>(Link.buildNextLink(r.result(), uriInfo), r.result().list())).build();
            case SearchResult.Aggregated a -> Response.ok(new Page<>(Link.buildNextLink(a.result(), uriInfo), a.result().list())).build();
        };
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") String id) {
        FinancialRecord record = this.getTransaction.execute(id);
        if (record == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(record).build();
    }

    @PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateTransaction(@PathParam("id") String id, UpdateCategoryRequest updateCategoryRequest) {
        final FinancialRecord record = this.updateTransactions.execute(id, updateCategoryRequest.category());
        if (record == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(record).build();
    }

    @GET
    @Path("/export")
    public Response exportTransactions() {
        String csv = this.exportTransactions.execute();
        return Response.ok(csv)
                .header("Content-Disposition", "attachment; filename=\"transactions.csv\"")
                .type("text/csv")
                .build();
    }
}
