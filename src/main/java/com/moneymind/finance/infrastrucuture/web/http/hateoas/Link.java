package com.moneymind.finance.adapters.in.web.http.hateoas;

import com.moneymind.finance.domain.SearchResponse;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import java.util.Map;

public record Link(String href) {
    public static <T> Link buildNextLink (
        SearchResponse<T> searchResponse,
        UriInfo uriInfo,
        Map<String, String> extraQueryParams) {

        UriBuilder builder = UriBuilder.fromUri(uriInfo.getAbsolutePath());

        if (searchResponse.cursor() != null && !searchResponse.cursor().isEmpty()){
            builder.queryParam("limit", String.valueOf(searchResponse.limit()));
            builder.queryParam("cursor", searchResponse.cursor());

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
