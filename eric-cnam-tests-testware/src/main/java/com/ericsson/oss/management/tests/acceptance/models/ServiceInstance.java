package com.ericsson.oss.management.tests.acceptance.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class ServiceInstance {

    private String onboardingHost;
    private String onboardingPort;
    private String executorHost;
    private String executorPort;
    private boolean isLocal;
    private String namespace;
    private String clusterConnectionInfoPath;

}
