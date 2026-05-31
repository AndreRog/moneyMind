package com.moneymind.finance.infrastructure.web.http;

import com.moneymind.finance.domain.PagedResult;
import com.moneymind.finance.domain.core.ClassificationQuery;
import com.moneymind.finance.domain.core.ClassifiedFinancialRecord;
import com.moneymind.finance.domain.transactions.ClassifyTransactions;
import com.moneymind.finance.infrastructure.web.http.dto.ClassificationRequest;
import com.moneymind.finance.infrastructure.web.http.hateoas.Link;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/classifications")
public class ClassificationsResource {

    private final ClassifyTransactions classifyTransactions;

    public ClassificationsResource(final ClassifyTransactions classifyTransactions) {
        this.classifyTransactions = classifyTransactions;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response classify(@Context UriInfo uriInfo, ClassificationRequest request) {
        PagedResult<ClassifiedFinancialRecord> result = this.classifyTransactions.execute(
                new ClassificationQuery(request != null ? request.cursor() : null)
        );
        return Response.ok(new Page<>(Link.buildNextLink(result, uriInfo), result.list())).build();
    }
}
