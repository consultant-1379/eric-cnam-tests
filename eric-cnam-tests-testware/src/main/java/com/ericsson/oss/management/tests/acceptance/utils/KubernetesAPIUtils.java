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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.ericsson.oss.management.tests.acceptance.exceptions.SetupException;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KubernetesAPIUtils {

    private final KubernetesClient kubernetesClient;

    public KubernetesAPIUtils(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    public static KubernetesAPIUtils configureKubernetesUtils(String kubeconfigPath) {
        try {
            log.info("Read kubeconfig content: {}", kubeconfigPath);
            var kubeconfigContent = Files.readString(Path.of(kubeconfigPath));
            log.info("Create config base on kubernetes content");
            Config config = Config.fromKubeconfig(kubeconfigContent);
            validateKubeConfig(config);
            log.info("Build kubernetes client base on config. Cluster name: {}", config.getCurrentContext().getContext().getCluster());
            KubernetesClient kubernetesClient = new KubernetesClientBuilder().withConfig(config).build();
            return new KubernetesAPIUtils(kubernetesClient);
        } catch (IOException e) {
            throw new SetupException(String.format("Can't configure kubernetes client from kubeconfig file by path: %s",
                                                     kubeconfigPath), e);
        }
    }

    public void deleteNamespace(String namespace) {
        kubernetesClient.namespaces().withName(namespace).delete();
    }

    public boolean checkThatAllPodsInNamespaceWithRunningState(String namespace) {
        List<Pod> pods = kubernetesClient.pods().inNamespace(namespace).list().getItems();
        if (!pods.isEmpty()) {
            return pods.stream()
                    .map(pod -> pod.getStatus().getPhase())
                    .allMatch(podPhase -> podPhase.equalsIgnoreCase("Running") || podPhase.equalsIgnoreCase("Completed"));
        }
        return false;
    }

    public String getClusterName() {
        return kubernetesClient.getConfiguration().getCurrentContext().getContext().getCluster();
    }

    private static void validateKubeConfig(Config config) {
        if (config.getCurrentContext() == null) {
            throw new SetupException("Configuration from kubeconfig failed. Kubeconfig must contain valid context.");
        }
    }
}
