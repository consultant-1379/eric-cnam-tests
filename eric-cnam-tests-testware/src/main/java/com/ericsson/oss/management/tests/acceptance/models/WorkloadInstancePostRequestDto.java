package com.ericsson.oss.management.tests.acceptance.models;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Builder
@AllArgsConstructor
@Data
public class WorkloadInstancePostRequestDto {
    private String workloadInstanceName;
    private String namespace;
    private String crdNamespace;
    private Integer timeout;
    private String helmSourceUrl;
    private Map<String, Object> additionalParameters;
}
