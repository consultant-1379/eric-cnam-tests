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

import static com.ericsson.oss.management.tests.acceptance.utils.Constants.EXECUTOR_HEALTH_URL;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.ONBOARDING_HEALTH_URL;
import static com.ericsson.oss.management.tests.acceptance.utils.ServiceProperties.SERVICE_INSTANCE;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class HealthCheck {
    private HealthCheck(){}

    private static final RestTemplate REST_TEMPLATE;

    static {
        REST_TEMPLATE = new RestTemplate();
    }

    @Step("Check the health of the onboarding service")
    public static HttpStatus getOnboardingHealthState() {
        String url = String.format(ONBOARDING_HEALTH_URL, SERVICE_INSTANCE.getOnboardingHost(), SERVICE_INSTANCE.getOnboardingPort());

        ResponseEntity<Void> healthResponse = REST_TEMPLATE.getForEntity(url, Void.class);
        log.info("Onboarding Service health status code is: {}", healthResponse.getStatusCode());
        return healthResponse.getStatusCode();
    }

    @Step("Check the health of the executor service")
    public static HttpStatus getExecutorHealthState() {
        String url = String.format(EXECUTOR_HEALTH_URL, SERVICE_INSTANCE.getExecutorHost(), SERVICE_INSTANCE.getExecutorPort());

        ResponseEntity<String> healthResponse = REST_TEMPLATE.getForEntity(url, String.class);
        log.info("Helmfile Executor health status code is: {}", healthResponse.getStatusCode());
        return healthResponse.getStatusCode();
    }

}
