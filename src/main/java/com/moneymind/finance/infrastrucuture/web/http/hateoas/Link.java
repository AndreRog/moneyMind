package com.moneymind.finance.adapters.in.web.http.hateoas;

import com.moneymind.finance.domain.PagedResult;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import java.util.Map;

public record Link(String href) {


    public static <T> Link buildNextLink (
            PagedResult<T> pagedResult,
            UriInfo uriInfo) {
        return buildNextLink(pagedResult, uriInfo, null);
    }

    public static <T> Link buildNextLink (
        PagedResult<T> pagedResult,
        UriInfo uriInfo,
        Map<String, String> extraQueryParams) {

        UriBuilder builder = UriBuilder.fromUri(uriInfo.getAbsolutePath());

        if (pagedResult.cursor() != null && !pagedResult.cursor().isEmpty()){
            builder.queryParam("limit", String.valueOf(pagedResult.limit()));
            builder.queryParam("cursor", pagedResult.cursor());

            if(extraQueryParams != null) {
                for (Map.Entry<String, String> entry : extraQueryParams.entrySet()) {
                    if(entry.getValue() != null){
                        builder.queryParam(entry.getKey(), entry.getValue());
                    }
                }
            }

            return new Link(builder.build().toString());
        }

        return null;
    }
}
