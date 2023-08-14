package io.spring.controller;

import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Yaml;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import io.spring.controller.models.V1Foo;
import io.spring.controller.models.V1FooList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

public class FooReconciler implements Reconciler {

    private static final Logger log = LoggerFactory.getLogger(FooReconciler.class);

    private final SharedIndexInformer<V1Foo> informer;
    private final  GenericKubernetesApi<V1Foo, V1FooList> api;
    public FooReconciler(SharedIndexInformer<V1Foo> informer, GenericKubernetesApi<V1Foo, V1FooList> api) {
        this.informer = informer;
        this.api = api;
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

    private void applyDeployment(V1Deployment deployment) throws ApiException {
        var deploymentApi = new AppsV1Api();
        var name = deployment.getMetadata().getName();
        var namespace = deployment.getMetadata().getNamespace();
        var deploymentList = deploymentApi.listNamespacedDeployment(namespace, null, null, null, null, null, null, null, null, null, null);
        boolean deploymentExist = deploymentList.getItems().stream().anyMatch(item -> item.getMetadata().getName().equals(name));
        if (deploymentExist) {
            deploymentApi.replaceNamespacedDeployment(name, namespace, deployment, null, null, null, null);
        } else {
            deploymentApi.createNamespacedDeployment(namespace, deployment, "true", null, null, null);
        }
    }

    private V1Deployment getDeployment(String name, V1Foo resource) throws IOException {
        var deploymentYaml = Files.readString(Path.of(new ClassPathResource(
                "deployment-template.yaml").getURI()));
        deploymentYaml.replaceAll("replace-me", name);
        return Yaml.loadAs(deploymentYaml, V1Deployment.class);
    }

    private static void applyConfigMap(V1ConfigMap configMap) throws ApiException {
        var configMapApi = new CoreV1Api();
        var name = configMap.getMetadata().getName();
        var namespace = configMap.getMetadata().getNamespace();
        var configMapList = configMapApi.listNamespacedConfigMap(namespace, null, null, null, null, null, null, null, null, null, null);
        boolean configMapExist = configMapList.getItems().stream().anyMatch(item -> item.getMetadata().getName().equals(name));
        if (configMapExist) {
            configMapApi.replaceNamespacedConfigMap(name, namespace, configMap, null, null, null, null);
        } else {
            configMapApi.createNamespacedConfigMap(namespace, configMap, "true", null, null, null);
        }
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
}