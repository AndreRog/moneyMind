package com.moneymind.finance.infrastrucuture.web.http;

import com.moneymind.finance.adapters.in.web.http.dto.UpdateCategoryRequest;
import com.moneymind.finance.adapters.in.web.http.hateoas.Link;
import com.moneymind.finance.domain.PagedResult;
import com.moneymind.finance.domain.core.ClassifiedFinancialRecord;
import com.moneymind.finance.domain.transactions.ClassifyTransactions;
import com.moneymind.finance.domain.transactions.ImportTransactions;
import com.moneymind.finance.domain.transactions.SearchTransactions;
import com.moneymind.finance.domain.core.FinancialRecord;
import com.moneymind.finance.domain.transactions.UpdateTransactions;
import com.moneymind.finance.infrastrucuture.web.http.dto.ClassificationRequest;
import com.opencsv.CSVWriter;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.RestForm;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/transactions")
public class TransactionsResource {

    private final ImportTransactions importTransactions;
    private final SearchTransactions searchTransactions;
    private final UpdateTransactions updateTransactions;
    private final ClassifyTransactions classifyTransactions;
    private final UriInfo uriInfo;

    public TransactionsResource(ImportTransactions importTransactions, SearchTransactions searchTransactions,
                                UpdateTransactions updateTransactions, final ClassifyTransactions classifyTransactions, UriInfo uriInfo) {
        this.importTransactions = importTransactions;
        this.searchTransactions = searchTransactions;
        this.updateTransactions = updateTransactions;
        this.classifyTransactions = classifyTransactions;
        this.uriInfo = uriInfo;
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
        final PagedResult<FinancialRecord> pagedResult = this.searchTransactions.execute(
                transactionId, category, dimension, bank, from, to, 100, //TODO: support limit
                cursor, sort
        );

        return Response.ok(toPageResponse(pagedResult, uriInfo, transactionId, category, bank, from, to, cursor)).build();
    }

    @GET
    @Path("/categories")
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchCategories(
            @Context UriInfo uriInfo
    ) {
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

        if (record == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(record).build();
    }

    @POST
    @Path("/classify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response classifyTransactions(
            @Context UriInfo uriInfo,
            ClassificationRequest classificationRequest) {

        PagedResult<ClassifiedFinancialRecord> classifiedTx = this.classifyTransactions.execute(classificationRequest);
        return Response.ok(toWebResponse(classifiedTx, uriInfo)).build();
    }

    @GET
    @Path("/export")
    public Response exportTransactions(@QueryParam("format") String format) {
        // Retrieve all transactions using the search method with null parameters
        final PagedResult<FinancialRecord> pagedResult = this.searchTransactions.execute(
                null, null, null, null, null, null, 10000, // Large limit to get all transactions
                null, null
        );

        List<FinancialRecord> transactions = pagedResult.list();

        // Default to YAML if format is not specified
        String requestedFormat = format != null ? format.toLowerCase() : "yaml";

        if ("csv".equals(requestedFormat)) {
            // Generate CSV
            StringWriter stringWriter = new StringWriter();
            CSVWriter csvStringWriter = new CSVWriter(stringWriter);

            // Create file for writing
            File csvFile = new File("transactions.csv");
            FileWriter fileWriter = null;
            CSVWriter csvFileWriter = null;

            try {
                fileWriter = new FileWriter(csvFile);
                csvFileWriter = new CSVWriter(fileWriter);

                // Write header
                String[] header = {"ID", "Bank", "Date", "Description", "Amount", "Balance", "Category"};
                csvStringWriter.writeNext(header);
                csvFileWriter.writeNext(header);

                // Write data
                for (FinancialRecord record : transactions) {
                    String[] row = {
                            record.getId(),
                            record.getBankName(),
                            record.getDate() != null ? record.getDate().toString() : "",
                            record.getDescription(),
                            record.getAmount() != null ? record.getAmount().toString() : "",
                            record.getFinalBalance() != null ? record.getFinalBalance().toString() : "",
                            record.getCategory()
                    };
                    csvStringWriter.writeNext(row);
                    csvFileWriter.writeNext(row);
                }

                // Close writers
                csvStringWriter.close();
                csvFileWriter.close();
                fileWriter.close();

                System.out.println("CSV file written to: " + csvFile.getAbsolutePath());

                return Response.ok(stringWriter.toString())
                        .header("Content-Disposition", "attachment; filename=\"transactions.csv\"")
                        .type("text/csv")
                        .build();
            } catch (IOException e) {
                // Clean up resources
                try {
                    if (csvStringWriter != null) csvStringWriter.close();
                    if (csvFileWriter != null) csvFileWriter.close();
                    if (fileWriter != null) fileWriter.close();
                } catch (IOException ex) {
                    // Ignore cleanup errors
                }
                return Response.serverError().entity("Error generating CSV: " + e.getMessage()).build();
            }
        } else {
            // Generate YAML
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);

            Yaml yaml = new Yaml(options);

            List<Map<String, Object>> yamlData = new ArrayList<>();
            for (FinancialRecord record : transactions) {
                Map<String, Object> recordMap = new HashMap<>();
                recordMap.put("id", record.getId());
                recordMap.put("bank", record.getBankName());
                recordMap.put("date", record.getDate() != null ? record.getDate().toString() : null);
                recordMap.put("description", record.getDescription());
                recordMap.put("amount", record.getAmount());
                recordMap.put("balance", record.getFinalBalance());
                recordMap.put("category", record.getCategory());

                yamlData.add(recordMap);
            }

            String yamlString = yaml.dump(yamlData);

            // Write to file
            File yamlFile = new File("transactions.yaml");
            FileWriter fileWriter = null;

            try {
                fileWriter = new FileWriter(yamlFile);
                fileWriter.write(yamlString);
                fileWriter.close();

                System.out.println("YAML file written to: " + yamlFile.getAbsolutePath());

                return Response.ok(yamlString)
                        .header("Content-Disposition", "attachment; filename=\"transactions.yaml\"")
                        .type("application/x-yaml")
                        .build();
            } catch (IOException e) {
                // Clean up resources
                try {
                    if (fileWriter != null) fileWriter.close();
                } catch (IOException ex) {
                    // Ignore cleanup errors
                }
                return Response.serverError().entity("Error generating YAML: " + e.getMessage()).build();
            }
        }
    }


    private Page<FinancialRecord> toPageResponse(PagedResult<FinancialRecord> pagedResult, UriInfo uriInfo,
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

        return new Page<>(Link.buildNextLink(pagedResult, uriInfo, parameters), pagedResult.list());
    }

    private Page<ClassifiedFinancialRecord> toWebResponse(PagedResult<ClassifiedFinancialRecord> pagedResult, UriInfo uriInfo) {
        return new Page<>(Link.buildNextLink( pagedResult, uriInfo), pagedResult.list());
    }
}
