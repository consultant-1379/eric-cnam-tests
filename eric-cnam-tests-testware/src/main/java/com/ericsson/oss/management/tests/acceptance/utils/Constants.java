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

public final class Constants {
    private Constants() { }

    public static final String ONBOARDING_HOST_KEY = "onboarding.host";
    public static final String ONBOARDING_PORT_KEY = "onboarding.port";
    public static final String EXECUTOR_HOST_KEY = "executor.host";
    public static final String EXECUTOR_PORT_KEY = "executor.port";
    public static final String IS_LOCAL = "isLocal";
    public static final String NAMESPACE = "func_namespace";
    public static final String KUBE_CONFIG_PATH = "kubeconfig_path";
    public static final String ONBOARDING_HEALTH_URL = "http://%s:%s/cnonb/v1/health";
    public static final String ONBOARDING_ONBOARD_URL = "http://%s:%s/cnonb/v1/onboarding";
    public static final String WORKLOAD_INSTANCE_URL = "http://%s:%s/cnonb/v1/workload_instances";
    public static final String EXECUTOR_HEALTH_URL = "http://%s:%s/actuator/health";
    public static final String CSAR_ARTIFACTORY_URL = "https://arm.sero.gic.ericsson.se/artifactory/" +
            "proj-eric-lcm-helm-executor-artifacts-generic-local/CI/CSAR/";
    public static final String CLUSTER_CONFIG_INFO_PATH = "src/main/resources/cluster-connection-info.config";
    public static final String VALUES_PATH = "src/main/resources/values.yaml";
    public static final String DISABLE_KUBE_AUTOCONFIGURATION = "kubernetes.disable.autoConfig";
    public static final String RESOURCES_DIR = "src/main/resources";
    public static final String CSARS_DIR = "csars";
    public static final String CSAR_FOR_UPDATE = "test-csar-1.0.0+1.csar";
    public static final String CSAR_FOR_INSTANTIATE = "test-csar-1.0.0+2.csar";
    public static final String STATUS_CODE = "StatusCode";
    public static final String HELMFILE_KEY = "helmfileUrl";
    public static final String HELM_CHARTS_KEY = "helmChartUrls";
    public static final String URL_KEY = "url";
    public static final String CSAR_ARCHIVE_KEY = "csarArchive";
    public static final String HELM_CHART_REGISTRY_ENABLED = "eric-lcm-helm-chart-registry.enabled";
    public static final String HELMFILE_NAME_FOR_UPDATE = "lightweight-acceptance-test-helmfile-helmfile:1.0.0_1";
    public static final String HELMFILE_NAME_FOR_INSTANTIATE = "lightweight-acceptance-test-helmfile-helmfile:1.0.0_2";
    public static final String WORKLOAD_INSTANCE_NAME = "successfulpost";
    public static final String CRD_NAMESPACE = "crdnamespace";
    public static final String VALUES = "values";
    public static final String CLUSTER_CONNECTION_INFO = "clusterConnectionInfo";

    public static final String WORKLOAD_INSTANCES_LW_URL = "http://%s:%s/cnwlcm/v1/light_weight_mode/workload_instances";
    public static final String DEPLOYMENT_STATUS_LW_URL = "http://%s:%s/cnwlcm/v1/light_weight_mode/%s";
    public static final String WORKLOAD_INSTANCE_NAME_KEY = "workloadInstanceName";
    public static final String RESPONSE_PREFIX = "Response is {}";

}