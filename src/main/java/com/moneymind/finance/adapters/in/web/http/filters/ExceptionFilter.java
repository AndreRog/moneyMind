package com.moneymind.finance.adapters.in.web.http.filters;

import com.moneymind.finance.adapters.in.web.http.ExceptionCodes;
import com.moneymind.finance.exceptions.ApplicationError;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class ExceptionFilter implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(ExceptionFilter.class);
    @Context
    UriInfo uriInfo;

    public static class Problems {
        public static final String ERROR = "/error";
    }

    @Override
    public Response toResponse(Exception exception)
    {
        LOG.error(exception);
        final String errorURI = UriBuilder.fromUri(uriInfo.getBaseUri()).path(Problems.ERROR).build().toString();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(
                        new ApplicationError(errorURI, ExceptionCodes.INTERNAL_SERVER_ERROR.getTitle())
                )
                .build();
    }
}
