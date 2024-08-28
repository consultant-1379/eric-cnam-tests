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
package com.ericsson.oss.management.tests.acceptance.tests;

import static com.ericsson.oss.management.tests.acceptance.utils.Constants.CLUSTER_CONFIG_INFO_PATH;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.HELM_CHART_REGISTRY_ENABLED;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.WORKLOAD_INSTANCE_NAME;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.WORKLOAD_INSTANCE_NAME_KEY;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.CSAR_ARTIFACTORY_URL;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.CSAR_FOR_UPDATE;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.CSAR_FOR_INSTANTIATE;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.STATUS_CODE;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.HELMFILE_NAME_FOR_UPDATE;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.HELMFILE_NAME_FOR_INSTANTIATE;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.URL_KEY;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.HELMFILE_KEY;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.HELM_CHARTS_KEY;
import static com.ericsson.oss.management.tests.acceptance.utils.ServiceProperties.SERVICE_INSTANCE;
import static com.ericsson.oss.management.tests.acceptance.utils.FileUtils.copyFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.ericsson.oss.management.tests.acceptance.models.DeploymentStateResponseDTO;
import com.ericsson.oss.management.tests.acceptance.models.LightWeightModeRequestDto;
import com.ericsson.oss.management.tests.acceptance.models.WorkloadInstancePutRequestDto;
import com.ericsson.oss.management.tests.acceptance.steps.LightWeightOperations;
import com.ericsson.oss.management.tests.acceptance.utils.KubernetesAPIUtils;
import io.fabric8.kubernetes.client.Config;

import com.ericsson.oss.management.tests.acceptance.steps.OnboardingSteps;
import com.ericsson.oss.management.tests.acceptance.steps.Operations;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ericsson.oss.management.tests.acceptance.steps.HealthCheck;

import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

import java.util.concurrent.TimeUnit;

@Slf4j
public class FullFlowLightweightTest {

    private KubernetesAPIUtils kubernetesAPIUtils;

    private String clusterConnectionInfoPath;

    @BeforeClass
    public void setup() {
        String kubeconfigPath = SERVICE_INSTANCE.isLocal() ? Config.getKubeconfigFilename() : SERVICE_INSTANCE.getClusterConnectionInfoPath();
        clusterConnectionInfoPath = copyFile(kubeconfigPath, CLUSTER_CONFIG_INFO_PATH).toString();
        kubernetesAPIUtils = KubernetesAPIUtils.configureKubernetesUtils(clusterConnectionInfoPath);
    }

    @AfterMethod
    public void deleteNamespace() {
        kubernetesAPIUtils.deleteNamespace(SERVICE_INSTANCE.getNamespace());
    }

    @Test(description = "Full flow LW test case")
    public void shouldRunFullLightweightFlowSuccessfully() {
        HttpStatus onboardingHealthState = HealthCheck.getOnboardingHealthState();
        assertThat(onboardingHealthState).isEqualTo(HttpStatus.OK);

        Map<String, Object> onboardResponse = onboardCsar(CSAR_FOR_INSTANTIATE, HELMFILE_NAME_FOR_INSTANTIATE);

        String helmfileUrl = String.valueOf(onboardResponse.get(HELMFILE_KEY));

        createWorkloadInstance(helmfileUrl);

        HttpStatus executorHealthState = HealthCheck.getExecutorHealthState();
        assertThat(executorHealthState).isEqualTo(HttpStatus.OK);

        // Build json for instantiate (HE) request
        LightWeightModeRequestDto instance = new LightWeightModeRequestDto();
        instance.setWorkloadInstanceName(WORKLOAD_INSTANCE_NAME);

        instantiateViaHelmfileExecutor(instance);

        await().pollInterval(60, TimeUnit.SECONDS)
                .atMost(180, TimeUnit.SECONDS)
                .until(() -> kubernetesAPIUtils.checkThatAllPodsInNamespaceWithRunningState(SERVICE_INSTANCE.getNamespace()));

        sendGetDeploymentStatusViaHelmfileExecutor(instance, 2);

        onboardingHealthState = HealthCheck.getOnboardingHealthState();
        assertThat(onboardingHealthState).isEqualTo(HttpStatus.OK);

        onboardResponse = onboardCsar(CSAR_FOR_UPDATE, HELMFILE_NAME_FOR_UPDATE);
        helmfileUrl = String.valueOf(onboardResponse.get(HELMFILE_KEY));

        WorkloadInstancePutRequestDto instancePutRequestDto = createWorkloadInstancePutRequestDto(helmfileUrl,
                WORKLOAD_INSTANCE_NAME, Map.of(HELM_CHART_REGISTRY_ENABLED, false));

        updateWorkloadInstance(instancePutRequestDto);

        assertThat(executorHealthState).isEqualTo(HttpStatus.OK);

        updateViaHelmfileExecutor(instance);

        await().pollInterval(60, TimeUnit.SECONDS)
                .atMost(180, TimeUnit.SECONDS)
                .until(() -> kubernetesAPIUtils.checkThatAllPodsInNamespaceWithRunningState(SERVICE_INSTANCE.getNamespace()));

        sendGetDeploymentStatusViaHelmfileExecutor(instance, 1);
    }

    private void instantiateViaHelmfileExecutor(LightWeightModeRequestDto instance) {
        ResponseEntity<Map<String, Object>> responseInstantiate = LightWeightOperations.instantiate(instance);

        assertThat(responseInstantiate).isNotNull();
        assertThat(responseInstantiate.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(responseInstantiate.getBody())
                .isNotNull()
                .containsEntry(WORKLOAD_INSTANCE_NAME_KEY, WORKLOAD_INSTANCE_NAME);
    }

    private void updateViaHelmfileExecutor(LightWeightModeRequestDto instance) {
        ResponseEntity<Map<String, Object>> responseInstantiate = LightWeightOperations.update(instance);

        assertThat(responseInstantiate).isNotNull();
        assertThat(responseInstantiate.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(responseInstantiate.getBody())
                .isNotNull()
                .containsEntry(WORKLOAD_INSTANCE_NAME_KEY, WORKLOAD_INSTANCE_NAME);
    }

    private void sendGetDeploymentStatusViaHelmfileExecutor(LightWeightModeRequestDto instance, int expectedAmountPods) {
        ResponseEntity<DeploymentStateResponseDTO> deploymentStatus = LightWeightOperations.getDeploymentStatus(instance);

        assertThat(deploymentStatus).isNotNull();
        assertThat(deploymentStatus.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deploymentStatus.getBody())
                .isNotNull()
                .extracting(DeploymentStateResponseDTO::getWorkloadInstanceName,
                            DeploymentStateResponseDTO::getNamespace,
                            DeploymentStateResponseDTO::getClusterName)
                .containsExactly(instance.getWorkloadInstanceName(), SERVICE_INSTANCE.getNamespace(), kubernetesAPIUtils.getClusterName());
        assertThat(deploymentStatus.getBody().getPods())
                .isNotNull()
                .hasSize(expectedAmountPods)
                .allSatisfy((podName, podState) -> {
                    assertThat(podName).containsAnyOf("eric-lcm-container-registry", "eric-lcm-helm-chart-registry");
                    assertThat(podState).isEqualTo("Running");
                });
    }

    private Object createWorkloadInstance(String helmFileUrl) {
        ResponseEntity<Map<String, Object>> responseEntity = OnboardingSteps.createWorkloadInstance(helmFileUrl,
                clusterConnectionInfoPath);
        Map<String, Object> ids = getBodyFromResponse(responseEntity);
        assertThat(ids.get(STATUS_CODE)).isEqualTo(HttpStatus.CREATED);

        Object workloadInstance = ids.get(URL_KEY);
        log.info("Response after create a workload instance is {}", workloadInstance);
        return workloadInstance;
    }

    private Object updateWorkloadInstance(WorkloadInstancePutRequestDto instancePutRequestDto) {
        ResponseEntity<Map<String, Object>> responseEntity = OnboardingSteps.updateWorkloadInstance(
                instancePutRequestDto,
                clusterConnectionInfoPath);
        Map<String, Object> ids = getBodyFromResponse(responseEntity);
        assertThat(ids.get(STATUS_CODE)).isEqualTo(HttpStatus.ACCEPTED);

        Object workloadInstance = ids.get(URL_KEY);
        log.info("Response after update a workload instance is {}", workloadInstance);
        return workloadInstance;
    }

    private Map<String, Object> onboardCsar(String csarName, String helmfileName) {
        FileSystemResource scar = Operations.downloadArtifact(CSAR_ARTIFACTORY_URL, csarName);
        ResponseEntity<Map<String, Object>> responseEntity = OnboardingSteps.onboard(scar);
        Map<String, Object> ids = getBodyFromOnboardingResponse(responseEntity);
        log.info("Response after onboard the csar archive is {}", ids);

        assertThat(ids.get(STATUS_CODE)).isEqualTo(HttpStatus.CREATED);

        //Verify response body
        assertThat(responseEntity.getBody())
                .isNotNull()
                .hasSize(2)
                .containsKeys(HELMFILE_KEY)
                .containsKeys(HELM_CHARTS_KEY);

        assertThat(responseEntity.getBody())
                .extractingByKey(HELM_CHARTS_KEY)
                .asList()
                .hasSize(2);

        String helmfileUrl = String.valueOf(ids.get(HELMFILE_KEY));
        assertThat(helmfileUrl).contains(helmfileName);

        return ids;
    }

    private Map<String, Object> getBodyFromResponse(ResponseEntity<Map<String, Object>> responseEntity) {
        Map<String, Object> ids = new HashMap<>();
        Map<String, Object> responseBody = Optional.ofNullable(responseEntity.getBody()).orElseThrow();
        ids.put(URL_KEY, responseBody
                .get(URL_KEY));
        ids.put(STATUS_CODE, responseEntity.getStatusCode());
        return ids;
    }

    private Map<String, Object> getBodyFromOnboardingResponse(ResponseEntity<Map<String, Object>> response) {
        Map<String, Object> ids = new HashMap<>();
        Map<String, Object> responseBody = Optional.ofNullable(response.getBody()).orElseThrow();
        ids.put(HELMFILE_KEY, responseBody
                .get(HELMFILE_KEY));
        ids.put(HELM_CHARTS_KEY, responseBody
                .get(HELM_CHARTS_KEY));
        ids.put(STATUS_CODE, response.getStatusCode());
        return ids;
    }

    private static WorkloadInstancePutRequestDto createWorkloadInstancePutRequestDto(String helmfileUrl,
                                                                                     String workloadInstanceName,
                                                                                     Map<String, Object> additionParameters) {
        WorkloadInstancePutRequestDto instancePutRequestDto = new WorkloadInstancePutRequestDto();
        instancePutRequestDto.setHelmSourceUrl(helmfileUrl);
        instancePutRequestDto.setWorkloadInstanceName(workloadInstanceName);
        instancePutRequestDto.setAdditionalParameters(additionParameters);
        return instancePutRequestDto;
    }

}
