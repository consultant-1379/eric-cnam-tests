package com.ericsson.oss.management.tests.acceptance.models;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class DeploymentStateResponseDTO {

    private String workloadInstanceName;
    private String clusterName;
    private String namespace;
    private Map<String, String> pods = new HashMap<>();

}
