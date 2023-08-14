A Reconciler implements a Kubernetes API for a specific resource by creating, updating or deleting Kubernetes objects, or by making changes to systems external to the cluster (e.g. cloudproviders, github, etc).

The [Kubernetes Java Client](https://github.com/kubernetes-client/java) provides a **Reconciler interface** to implement custom controllers watching events on resources. 
```editor:append-lines-to-file
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
description: First custom resource
text: |2
  package io.spring.controller;

  import io.kubernetes.client.extended.controller.reconciler.Reconciler;
  import io.kubernetes.client.extended.controller.reconciler.Request;
  import io.kubernetes.client.extended.controller.reconciler.Result;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;

  public class FooReconciler implements Reconciler {

      private static final Logger log = LoggerFactory.getLogger(FooReconciler.class);

      @Override
      public Result reconcile(Request request) {

          return new Result(false);
      }
  }
```

Get resource related to request from Cache.
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 15
text: |2
          var namespace = request.getNamespace();
          var name  = request.getName();
          var lister = new Lister<>(informer.getIndexer(), namespace);
          var resource = lister.get(name);
          if (resource == null || resource.getMetadata().getDeletionTimestamp() != null) {
              return new Result(false);
          }
```
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 12
text: |2

      private final SharedIndexInformer<V1Foo> informer;
      public FooReconciler(SharedIndexInformer<V1Foo> informer) {
          this.informer = informer;
      }

```
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 8
text: |2
  import io.kubernetes.client.informer.cache.Lister;
  import io.kubernetes.client.informer.SharedIndexInformer;
  import io.spring.controller.models.V1Foo;
```

Create ConfigMap.
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 30
text: |2

          var configMapContent = Map.of("index.html", "<h1> Hello, " + resource.getSpec().getNickname() + " </h1>");
          var configMap = getConfigMap(name, resource, configMapContent);

```
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 36
text: |2

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
```
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 48
text: |2

      private V1OwnerReference getOwnerReference(V1Foo owner) {
          return new V1OwnerReferenceBuilder().withApiVersion(owner.getApiVersion()).withKind(owner.getKind())
                .withName(owner.getMetadata().getName()).withUid(owner.getMetadata().getUid()).withController().build();
      }
```
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 11
text: |2
  import io.kubernetes.client.openapi.models.*;
  import java.util.Map;
  import java.util.Collections;
```

Apply ConfigMap.
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 36
text: |2
          try {
              applyConfigMap(configMap);
          } catch (ApiException e) {
              log.error("Applying ConfigMap for Foo " + namespace + "/" + name + " failed", e);
              return new Result(true, Duration.ofSeconds(10));
          }

```
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 62
text: |2

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
```
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 14
text: |2
  import io.kubernetes.client.openapi.ApiException;
  import io.kubernetes.client.openapi.apis.CoreV1Api;
  import java.time.Duration;
```

Create and apply Deployment.
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 45
text: |2

          try {
              var deployment = getDeployment(name, resource);
              applyDeployment(deployment);
          } catch (ApiException | IOException e) {
              log.error("Applying Deployment for Foo " + namespace + "/" + name + " failed", e);
              return new Result(true, Duration.ofSeconds(10));
          }

```
```editor:append-lines-to-file
file: ~/controller/src/main/resources/deployment-template.yaml
description: Add deployment YAML template to resources
text: |2
  piVersion: apps/v1
  kind: Deployment
  metadata:
    name: replace-me
  spec:
    selector:
      matchLabels:
        app: replace-me
    replicas: 2
    template:
      metadata:
        labels:
          app: replace-me
      spec:
        containers:
          - name: nginx
            image: nginx:latest
            ports:
              - containerPort: 80
            volumeMounts:
              - name: nginx-index-file
                mountPath: /usr/share/nginx/html/
        volumes:
          - name: nginx-index-file
            configMap:
              name: replace-me
```
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 84
text: |2

        private V1Deployment getDeployment(String name, V1Foo resource) throws IOException {
            var deploymentYaml = Files.readString(Path.of(new ClassPathResource(
                    "deployment-template.yaml").getURI()));
            deploymentYaml.replaceAll("replace-me", name);
            return Yaml.loadAs(deploymentYaml, V1Deployment.class);
        }
```
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 91
text: |2

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
```
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 17
text: |2
  import io.kubernetes.client.openapi.apis.AppsV1Api;
  import io.kubernetes.client.util.Yaml;
  import org.springframework.core.io.ClassPathResource;
  import java.io.IOException;
  import java.nio.file.Files;
  import java.nio.file.Path;
```

Finally, we have to provide an instance of our reconciler as a bean.
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/ControllerConfiguration.java
line: 33
text: |2

    @Bean
    Reconciler reconciler(SharedIndexInformer<V1Foo> parentInformer) {
        return new FooReconciler(parentInformer);
    }
```