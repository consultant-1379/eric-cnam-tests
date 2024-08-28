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

package com.ericsson.oss.management.tests.acceptance.steps;

import com.ericsson.oss.management.tests.acceptance.models.DeploymentStateResponseDTO;
import com.ericsson.oss.management.tests.acceptance.models.LightWeightModeRequestDto;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ericsson.oss.management.tests.acceptance.utils.Constants.DEPLOYMENT_STATUS_LW_URL;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.RESPONSE_PREFIX;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.WORKLOAD_INSTANCES_LW_URL;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.WORKLOAD_INSTANCE_NAME_KEY;
import static com.ericsson.oss.management.tests.acceptance.utils.ServiceProperties.SERVICE_INSTANCE;

@Slf4j
public final class LightWeightOperations {

    private LightWeightOperations(){}

    private static final RestTemplate REST_TEMPLATE;

    static {
        REST_TEMPLATE = new RestTemplate();
    }

    @Step("Instantiate via LightWeight mode with name {instance.workloadInstanceName}")
    public static ResponseEntity<Map<String, Object>> instantiate(final LightWeightModeRequestDto instance) {
        log.info("Instantiate via LW");
        String url = String.format(WORKLOAD_INSTANCES_LW_URL, SERVICE_INSTANCE.getExecutorHost(), SERVICE_INSTANCE.getExecutorPort());
        HttpEntity<Map<String, Object>> requestEntity = createInstantiateLWRequest(instance);
        ResponseEntity<Map<String, Object>> response = REST_TEMPLATE.exchange(url, HttpMethod.POST,
                                                                                  requestEntity,
                                                                                  new ParameterizedTypeReference<>() { });
        log.info(RESPONSE_PREFIX, response);
        return response;
    }

    @Step("Update via LightWeight mode with name {instance.workloadInstanceName}")
    public static ResponseEntity<Map<String, Object>> update(final LightWeightModeRequestDto instance) {
        log.info("Update via LW");
        String url = String.format(WORKLOAD_INSTANCES_LW_URL, SERVICE_INSTANCE.getExecutorHost(), SERVICE_INSTANCE.getExecutorPort());
        HttpEntity<Map<String, Object>> requestEntity = createInstantiateLWRequest(instance);
        ResponseEntity<Map<String, Object>> response = REST_TEMPLATE.exchange(url, HttpMethod.PUT,
                requestEntity,
                new ParameterizedTypeReference<>() { });
        log.info(RESPONSE_PREFIX, response);
        return response;
    }

    @Step("Send get deployment status request with LW mode for instance: {instance.workloadInstanceName}")
    public static ResponseEntity<DeploymentStateResponseDTO> getDeploymentStatus(final LightWeightModeRequestDto instance) {
        String url = String.format(DEPLOYMENT_STATUS_LW_URL, SERVICE_INSTANCE.getExecutorHost(),
                SERVICE_INSTANCE.getExecutorPort(), instance.getWorkloadInstanceName());
        HttpEntity<Object> objectHttpEntity = new HttpEntity<>(getHeaders());
        ResponseEntity<DeploymentStateResponseDTO> response = REST_TEMPLATE.exchange(url, HttpMethod.GET,
                                                                                     objectHttpEntity, DeploymentStateResponseDTO.class);
        log.info(RESPONSE_PREFIX, response);
        return response;
    }

    private static HttpEntity<Map<String, Object>> createInstantiateLWRequest(
            final LightWeightModeRequestDto instance) {
        Map<String, Object> dto = new HashMap<>();
        dto.put(WORKLOAD_INSTANCE_NAME_KEY, instance.getWorkloadInstanceName());
        return new HttpEntity<>(dto, getHeaders());
    }

    private static HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}