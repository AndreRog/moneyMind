package com.moneymind.finance.infrastructure.web.http;

import com.moneymind.finance.domain.categories.ListCategories;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/categories")
public class CategoriesResource {

    @Inject
    ListCategories listCategories;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        return Response.ok(listCategories.execute(null)).build();
    }
}