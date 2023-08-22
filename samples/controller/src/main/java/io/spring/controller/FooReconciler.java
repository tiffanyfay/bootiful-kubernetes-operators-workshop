package io.spring.controller;

import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.spring.controller.models.V1Foo;
import io.kubernetes.client.openapi.models.*;
import java.util.Map;
import java.util.Collections;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import java.time.Duration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.util.Yaml;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
import org.springframework.util.FileCopyUtils;
import java.io.InputStreamReader;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class FooReconciler implements Reconciler {

    private static final Logger log = LoggerFactory.getLogger(FooReconciler.class);

    private final SharedIndexInformer<V1Foo> informer;
    private final CoreV1Api coreV1Api;
    private final AppsV1Api appsV1Api;
    public FooReconciler(SharedIndexInformer<V1Foo> informer, CoreV1Api coreV1Api, AppsV1Api appsV1Api) {
        this.appsV1Api = appsV1Api;

        this.coreV1Api = coreV1Api;

        this.informer = informer;
    }

    @Override
    public Result reconcile(Request request) {
        var namespace = request.getNamespace();
        var name  = request.getName();
        var lister = new Lister<>(informer.getIndexer(), namespace);
        var resource = lister.get(name);
        if (resource == null || resource.getMetadata().getDeletionTimestamp() != null) {
            return new Result(false);
        }

        var configMapContent = Map.of("index.html", "<h1> Hello, " + resource.getSpec().getNickname() + " </h1>");
        var configMap = getConfigMap(name, resource, configMapContent);
        try {
            applyConfigMap(configMap);
        } catch (ApiException e) {
            log.error("Applying ConfigMap for Foo " + namespace + "/" + name + " failed", e);
            return new Result(true, Duration.ofSeconds(10));
        }

        try {
            var deployment = getDeployment(name, resource);
            applyDeployment(deployment);
        } catch (ApiException | IOException e) {
            log.error("Applying Deployment for Foo " + namespace + "/" + name + " failed", e);
            return new Result(true, Duration.ofSeconds(10));
        }

        return new Result(false);
    }

    private V1ConfigMap getConfigMap(String name, V1Foo resource, Map<String, String> configMapContent) {
        return new V1ConfigMapBuilder()
              .withApiVersion("v1")
              .withNewMetadata()
                  .withName(name)
                  .withNamespace(resource.getMetadata().getNamespace())
                  .withOwnerReferences(Collections.singletonList(getOwnerReference(resource)))
              .endMetadata()
              .withData(configMapContent)
              .build();
    }

    private V1OwnerReference getOwnerReference(V1Foo owner) {
        return new V1OwnerReferenceBuilder().withApiVersion(owner.getApiVersion()).withKind(owner.getKind())
              .withName(owner.getMetadata().getName()).withUid(owner.getMetadata().getUid()).withController().build();
    }

    private void applyConfigMap(V1ConfigMap configMap) throws ApiException {
        var namespace = configMap.getMetadata().getNamespace();
        if (configMapExists(configMap)) {
            coreV1Api.replaceNamespacedConfigMap(configMap.getMetadata().getName(), namespace, configMap, null, null, null, null);
        } else {
            coreV1Api.createNamespacedConfigMap(namespace, configMap, "true", null, null, null);
        }
    }

    private boolean configMapExists(V1ConfigMap configMap) throws ApiException {
        var configMapList = coreV1Api.listNamespacedConfigMap(configMap.getMetadata().getNamespace(), null, null, null, null, null, null, null, null, null, null);
        return configMapList.getItems().stream().anyMatch(item -> item.getMetadata().getName().equals(configMap.getMetadata().getName()));
    }

    private V1Deployment getDeployment(String name, V1Foo resource) throws IOException {
        var deploymentYaml = FileCopyUtils.copyToString(new InputStreamReader(new ClassPathResource(
              "deployment-template.yaml").getInputStream()));
        deploymentYaml = deploymentYaml.replaceAll("NAMESPACE", resource.getMetadata().getNamespace())
                  .replaceAll("NAME", name);
        return Yaml.loadAs(deploymentYaml, V1Deployment.class);
    }

    private void applyDeployment(V1Deployment deployment) throws ApiException {
        var namespace = deployment.getMetadata().getNamespace();
        if (deploymentExists(deployment)) {
            appsV1Api.replaceNamespacedDeployment(deployment.getMetadata().getName(), namespace, deployment, null, null, null, null);
        } else {
            appsV1Api.createNamespacedDeployment(namespace, deployment, "true", null, null, null);
        }
    }

    private boolean deploymentExists(V1Deployment deployment) throws ApiException {
        var deploymentList = appsV1Api.listNamespacedDeployment(deployment.getMetadata().getNamespace(), null, null, null, null, null, null, null, null, null, null);
        return deploymentList.getItems().stream().anyMatch(item -> item.getMetadata().getName().equals(deployment.getMetadata().getName()));
    }

    static class ResourceAccessHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.resources().registerPattern("deployment-template.yaml");
        }
    }
}
