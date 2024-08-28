/*******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/
package com.ericsson.oss.management.tests.acceptance.utils;

import static com.ericsson.oss.management.tests.acceptance.utils.Constants.DISABLE_KUBE_AUTOCONFIGURATION;

import com.ericsson.oss.management.tests.acceptance.models.ServiceInstance;

public final class ServiceProperties {
    private ServiceProperties() { }

    public static final ServiceInstance SERVICE_INSTANCE;

    static {
        // This property is required to disable automatic configuration, so kubernetes client
        // could not use default kubeConfig in case of issues with the desired one.
        System.setProperty(DISABLE_KUBE_AUTOCONFIGURATION, Boolean.TRUE.toString());

        String onboardingHost = System.getProperty(Constants.ONBOARDING_HOST_KEY);
        String onboardingPort = System.getProperty(Constants.ONBOARDING_PORT_KEY);
        String executorHost = System.getProperty(Constants.EXECUTOR_HOST_KEY);
        String executorPort = System.getProperty(Constants.EXECUTOR_PORT_KEY);
        String isLocalString = System.getProperty(Constants.IS_LOCAL);
        boolean isLocal = Boolean.parseBoolean(isLocalString);
        String namespace = System.getProperty(Constants.NAMESPACE);
        String clusterConnectionInfoPath = System.getProperty(Constants.KUBE_CONFIG_PATH);
        SERVICE_INSTANCE = new ServiceInstance(onboardingHost, onboardingPort, executorHost, executorPort,
                                               isLocal, namespace, clusterConnectionInfoPath);
    }

}