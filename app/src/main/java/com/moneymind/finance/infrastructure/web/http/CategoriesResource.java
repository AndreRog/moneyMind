package com.moneymind.finance.infrastructure.web.http;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/categories")
public class CategoriesResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        return Response.ok(List.of(
                "HOUSING", "RESTAURANTS", "GROCERIES", "TRAVEL", "FLIGHTS", "ACCOMMODATION",
                "INCOME", "OTHERS", "NICO", "HEALTH & WELLNESS", "GYM", "PSYCHOLOGY", "SUBSCRIPTIONS",
                "NETFLIX", "PRIME", "APPLE", "NESPRESSO", "UTILITIES", "WATER", "ELECTRICITY",
                "APPLIANCES", "SALARY", "FUN MONEY", "INTERNET", "MOBILE", "TRANSFER BETWEEN ACCOUNTS",
                "FINANCIAL EXPENSES", "CAR", "GASOLINE", "CAR TOOL"
        )).build();
    }
}
