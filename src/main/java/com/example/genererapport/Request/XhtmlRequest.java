package com.example.genererapport.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data

public class XhtmlRequest {
    @JsonProperty("XhtmlContent")
    private String XhtmlContent;

    @JsonProperty("basePath")
    private String basePath;

    @JsonProperty("parameters")
    private Map<String, Parameter> parameters;

    @Data
    public static class Parameter {
        @JsonProperty("type")
        private String type;
    }
}
