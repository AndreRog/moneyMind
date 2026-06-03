package com.moneymind.finance.infrastructure.web.http;

import com.moneymind.finance.domain.guest.GuestImport;
import com.moneymind.finance.domain.guest.GuestReview;
import com.moneymind.finance.domain.guest.UnsupportedBankException;
import com.moneymind.finance.infrastructure.web.http.dto.GuestImportError;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestForm;

import java.io.InputStream;

/**
 * Public, stateless guest import. A visitor uploads a bank CSV and gets an instant
 * monthly review back — no authentication, nothing persisted. The bank is auto-detected
 * by trying each registered parser.
 */
@Path("/guest")
public class GuestResource {

    private final Logger LOG = Logger.getLogger(GuestResource.class);

    private final GuestImport guestImport;

    public GuestResource(final GuestImport guestImport) {
        this.guestImport = guestImport;
    }

    @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response importGuest(@RestForm("file") InputStream file) {
        try {
            final GuestReview review = guestImport.execute(file);
            return Response.ok(review).build();
        } catch (UnsupportedBankException e) {
            return Response.status(422) // Unprocessable Entity
                    .entity(new GuestImportError("UNSUPPORTED_BANK", e.getMessage()))
                    .build();
        } catch (Exception e) {
            LOG.error("Guest import failed", e);
            return Response.serverError()
                    .entity(new GuestImportError("IMPORT_FAILED", e.getMessage()))
                    .build();
        }
    }
}
