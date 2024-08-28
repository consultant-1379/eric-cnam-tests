package com.ericsson.oss.management.tests.acceptance.steps;

import com.ericsson.oss.management.tests.acceptance.models.WorkloadInstancePostRequestDto;
import com.ericsson.oss.management.tests.acceptance.models.WorkloadInstancePutRequestDto;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;
import java.util.Map;

import static com.ericsson.oss.management.tests.acceptance.utils.Constants.ONBOARDING_ONBOARD_URL;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.WORKLOAD_INSTANCE_URL;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.RESPONSE_PREFIX;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.RESOURCES_DIR;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.CSARS_DIR;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.CSAR_ARCHIVE_KEY;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.VALUES_PATH;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.WORKLOAD_INSTANCE_NAME;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.CRD_NAMESPACE;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.VALUES;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.CLUSTER_CONNECTION_INFO;
import static com.ericsson.oss.management.tests.acceptance.utils.ServiceProperties.SERVICE_INSTANCE;
import static com.ericsson.oss.management.tests.acceptance.utils.FileUtils.getFileResource;
import static com.ericsson.oss.management.tests.acceptance.utils.FileUtils.deleteDirectory;

@Slf4j
public final class OnboardingSteps {

    private OnboardingSteps(){}

    private static final RestTemplate REST_TEMPLATE;

    static {
        REST_TEMPLATE = new RestTemplate();
    }

    @Step("Onboard the csar archive")
    public static ResponseEntity<Map<String, Object>> onboard(FileSystemResource csarPackage) {
        log.info("Onboard a csar archive");
        String url = String.format(ONBOARDING_ONBOARD_URL, SERVICE_INSTANCE.getOnboardingHost(), SERVICE_INSTANCE.getOnboardingPort());
        HttpEntity<MultiValueMap<String, Object>> requestEntity = createOnboardRequestBody(csarPackage);

        ResponseEntity<Map<String, Object>> responseEntity  = REST_TEMPLATE.exchange(url, HttpMethod.POST, requestEntity,
                new ParameterizedTypeReference<>() { });

        log.info(RESPONSE_PREFIX, responseEntity);
        deleteDirectory(Path.of(RESOURCES_DIR + "/" + CSARS_DIR));
        return responseEntity;
    }

    @Step("Create a Workload instance")
    public static ResponseEntity<Map<String, Object>> createWorkloadInstance(String helmFilePath,
                                                                                 String pathToClusterConnectionInfo) {
        log.info("Create a Workload instance");
        var url = String.format(WORKLOAD_INSTANCE_URL, SERVICE_INSTANCE.getOnboardingHost(), SERVICE_INSTANCE.getOnboardingPort());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = createWorkloadInstancesHttpEntity(helmFilePath,
                pathToClusterConnectionInfo, VALUES_PATH);
        ResponseEntity<Map<String, Object>> response = REST_TEMPLATE.exchange(url, HttpMethod.POST, requestEntity,
                new ParameterizedTypeReference<>() { });
        log.info(RESPONSE_PREFIX, response);
        return response;
    }

    @Step("Update the Workload instance")
    public static ResponseEntity<Map<String, Object>> updateWorkloadInstance(WorkloadInstancePutRequestDto instancePutRequestDto,
                                                                             String pathToClusterConnectionInfo) {
        var url = String.format(WORKLOAD_INSTANCE_URL, SERVICE_INSTANCE.getOnboardingHost(), SERVICE_INSTANCE.getOnboardingPort());
        HttpEntity<MultiValueMap<String, Object>> requestEntity = updateWorkloadInstanceHttpEntity(instancePutRequestDto,
                pathToClusterConnectionInfo, VALUES_PATH);
        ResponseEntity<Map<String, Object>> response = REST_TEMPLATE.exchange(url, HttpMethod.PUT, requestEntity,
                new ParameterizedTypeReference<>() { });
        log.info(RESPONSE_PREFIX, response);
        return response;
    }

    private static HttpEntity<MultiValueMap<String, Object>> createOnboardRequestBody(
            final FileSystemResource csar) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add(CSAR_ARCHIVE_KEY, csar);

        return new HttpEntity<>(body, headers);
    }

    private static HttpEntity<MultiValueMap<String, Object>> createWorkloadInstancesHttpEntity(String helmFilePath,
                                                                                               String pathToClusterConnectionInfoFile,
                                                                                               String pathToValuesFile) {
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        body.add("workloadInstancePostRequestDto", createWorkloadInstancePostRequestDto(helmFilePath));
        if (pathToValuesFile != null) {
            body.add(VALUES, getFileResource(pathToValuesFile));
        }
        if (pathToClusterConnectionInfoFile != null) {
            body.add(CLUSTER_CONNECTION_INFO, getFileResource(pathToClusterConnectionInfoFile));
        }
        return new HttpEntity<>(body, headers);
    }

    private static HttpEntity<MultiValueMap<String, Object>> updateWorkloadInstanceHttpEntity(WorkloadInstancePutRequestDto instancePutRequestDto,
                                                                                              String pathToClusterConnectionInfoFile,
                                                                                              String pathToValuesFile) {
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        body.add("workloadInstancePutRequestDto", instancePutRequestDto);
        if (pathToValuesFile != null) {
            body.add(VALUES, getFileResource(pathToValuesFile));
        }
        if (pathToClusterConnectionInfoFile != null) {
            body.add(CLUSTER_CONNECTION_INFO, getFileResource(pathToClusterConnectionInfoFile));
        }
        return new HttpEntity<>(body, headers);
    }

    private static WorkloadInstancePostRequestDto createWorkloadInstancePostRequestDto(String helmFilePath) {
        return WorkloadInstancePostRequestDto.builder()
                .workloadInstanceName(WORKLOAD_INSTANCE_NAME)
                .crdNamespace(CRD_NAMESPACE)
                .helmSourceUrl(helmFilePath)
                .namespace(SERVICE_INSTANCE.getNamespace())
                .timeout(3)
                .build();
    }
}
