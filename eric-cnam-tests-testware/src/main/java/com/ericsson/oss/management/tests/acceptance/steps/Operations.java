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

import com.ericsson.oss.management.tests.acceptance.exceptions.InternalRuntimeException;
import com.ericsson.oss.management.tests.acceptance.utils.FileUtils;
import io.qameta.allure.Step;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.ericsson.oss.management.tests.acceptance.utils.Constants.CSARS_DIR;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.CSAR_ARTIFACTORY_URL;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.CSAR_FOR_INSTANTIATE;
import static com.ericsson.oss.management.tests.acceptance.utils.Constants.RESOURCES_DIR;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public final class Operations {

    private static final RestTemplate REST_TEMPLATE;

    static {
        REST_TEMPLATE = new RestTemplate();
    }

    private Operations() {
    }

    @Step("Download and save file from artifactory")
    public static FileSystemResource downloadArtifact(String url, String artifactName)  {
        log.info("Start to download artifact {}", artifactName);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.MULTIPART_FORM_DATA));

        ResponseEntity<byte[]> response = REST_TEMPLATE.exchange(url + artifactName,
                HttpMethod.GET, new HttpEntity<>(headers), byte[].class, artifactName);
        if (response.getBody() == null) {
            throw new InternalRuntimeException(String.format("Downloaded artifact %s is null", artifactName));
        }
        Path dir = FileUtils.createDirectory(CSARS_DIR);
        Path filePath = Paths.get(String.valueOf(dir), artifactName);
        try {
            Files.write(filePath, response.getBody());
        } catch (IOException e) {
            throw new InternalRuntimeException(
                    String.format("Failed to write file %s. Details: %s", artifactName, e.getMessage()));
        }
        log.info("Artifact {} downloaded successfully", artifactName);
        return new FileSystemResource(filePath);
    }

    @Step("Download specified artifact from the artifactory and upload to the registry via onboarding service API")
    public static void shouldDownloadCsarFromTheArtifactoryAndOnboardToRegistry() {
        //this is an initial setup to test on a local evn, and it's going to be adapted in scope of task CAM-1622.
        FileSystemResource artifact = downloadArtifact(CSAR_ARTIFACTORY_URL, CSAR_FOR_INSTANTIATE);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept((List.of(MediaType.APPLICATION_JSON)));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("csarArchive", artifact);

        ResponseEntity<String> response = REST_TEMPLATE.exchange(
                "http://localhost:8080/cnonb/v1/onboarding",
                // TODO: 2023-03-13 CAM-1622: Adapt url above to make it dynamic depends on test environment
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        deleteDownloadedArtifact(CSAR_FOR_INSTANTIATE);
    }

    private static void deleteDownloadedArtifact(String artifactName) {
        Path filePath = Paths.get(RESOURCES_DIR + "/" + CSARS_DIR, artifactName);
        if (Files.exists(filePath)) {
            try {
                Files.delete(filePath);
            } catch (IOException e) {
                throw new InternalRuntimeException(
                        String.format("Failed to delete file %s. Details %s", artifactName, e.getMessage()));
            }
        }
    }
}
