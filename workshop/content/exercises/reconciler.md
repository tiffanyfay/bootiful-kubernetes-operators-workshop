A Reconciler implements a Kubernetes API for a specific resource by creating, updating or deleting Kubernetes objects, or by making changes to systems external to the cluster (e.g. cloudproviders, github, etc).

The [Kubernetes Java Client](https://github.com/kubernetes-client/java) provides a **Reconciler interface** to implement custom controllers watching events on resources. 
```editor:append-lines-to-file
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
description: Create Reconciler skeleton
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
The `reconcile` method has one parameter with of type `Request`, which contains the information necessary to reconcile a resource object. This includes the information to uniquely identify the object - its name and namespace. It does not contain information about any specific event or the object's contents itself.

The method returns a `Result` object with the information of whether a request was handled successfully, and based on that whether it should be requeued. It's also possible to configure when it should be requeued with a duration parameter.

Let's start with the implementation of the actual functionality of our custom resource.
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 15
description: Get related resource from cache
text: |2
          var namespace = request.getNamespace();
          var name  = request.getName();
          var lister = new Lister<>(informer.getIndexer(), namespace);
          var resource = lister.get(name);
          if (resource == null || resource.getMetadata().getDeletionTimestamp() != null) {
              return new Result(false);
          }
```

We are using a `Lister` with the information provided by the request to **get the related resource from the cache** of our configured running informer - which we have to inject as a dependency.
If there is no related resource in the list, it is probably deleted, and we mark the request as handled.

```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 12
description: Inject informer dependency
text: |2

      private final SharedIndexInformer<V1Foo> informer;
      public FooReconciler(SharedIndexInformer<V1Foo> informer) {
          this.informer = informer;
      }

```
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 8
description: Add imports
text: |2
  import io.kubernetes.client.informer.cache.Lister;
  import io.kubernetes.client.informer.SharedIndexInformer;
  import io.spring.controller.models.V1Foo;
```

The Kubernetes Java Client provides related classes for all the out-of-the-box Kubernetes resources like deployments (V1Deployment), services (V1Service) etc.

Now we'll construct a `V1ConfigMap` object containing all the required information for Kubernetes to create a `ConfigMap` resource including the individual greeting as HTML for a Foo.

```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 30
description: Construct V1ConfigMap object (1/3)
text: |2

          var configMapContent = Map.of("index.html", "<h1> Hello, " + resource.getSpec().getNickname() + " </h1>");
          var configMap = getConfigMap(name, resource, configMapContent);
```
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 36
description: Construct V1ConfigMap object (2/3)
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
In addition to the classes, the Kubernetes Java Client also provides builders for them to make the object creation a little bit more readable.
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 48
description: Construct V1ConfigMap object (3/3)
text: |2

      private V1OwnerReference getOwnerReference(V1Foo owner) {
          return new V1OwnerReferenceBuilder().withApiVersion(owner.getApiVersion()).withKind(owner.getKind())
                .withName(owner.getMetadata().getName()).withUid(owner.getMetadata().getUid()).withController().build();
      }
```
As you can see, we are adding an **owner reference to the ConfigMap**. This links it to the Foo resource and it will be automatically deleted if the related Foo resource gets deleted, so we don't have to care about it.

```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 11
description: Add imports
text: |2
  import io.kubernetes.client.openapi.models.*;
  import java.util.Map;
  import java.util.Collections;
```

After we have a `V1ConfigMap` object will all the information, it's time to apply it to our Kubernetes cluster.
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 36
description: Apply ConfigMap to Kubernetes (1/2)
text: |2
          try {
              applyConfigMap(configMap);
          } catch (ApiException e) {
              log.error("Applying ConfigMap for Foo " + namespace + "/" + name + " failed", e);
              return new Result(true, Duration.ofSeconds(10));
          }

```
In the case of an API error, we are requeuing the request after ten seconds.
```editor:select-matching-text
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
description: Select constructor to add dependency injection of CoreV1Api instance
text: "public FooReconciler(SharedIndexInformer<V1Foo> informer) {"
```
```editor:replace-text-selection
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
description: Add dependency injection of CoreV1Api instance
text: |2
  private final CoreV1Api coreV1Api;
      public FooReconciler(SharedIndexInformer<V1Foo> informer, CoreV1Api coreV1Api) {
          this.coreV1Api = coreV1Api;
```
To apply the ConfigMap to the Kubernetes cluster via the Kubernetes API, we could configure a `GenericKubernetesApi` instance for it, but the Kubernetes Java Client also provides classes with a higher abstraction for the out-of-the-box Kubernetes resources. Those are structured based on the API the resources belong to. 
So for ConfigMaps, it's the `CoreV1Api` class, which we inject an instance of via the constructor and configure later. 
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
description: Apply ConfigMap to Kubernetes (2/2)
line: 65
text: |2

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
```
As `CoreV1Api` class doesn't provide an apply functionality, we have to implement it ourselves. 
To get a list of all ConfigMaps in the namespace, we are using the `coreV1Api.listNamespacedConfigMap` method. With the resulting list, it will be then checked whether a ConfigMap with the name already exists in the namespace.
If that's the case, the `coreV1Api.replaceNamespacedConfigMap` will update the ConfigMap resource with the updated configuration.
Otherwise, the `coreV1Api.createNamespacedConfigMap` method will be used to create the configured ConfigMap in the cluster.
As you can see all those methods have several additional parameters, which we set to null, to just use the defaults.

```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 14
description: Add imports
text: |2
  import io.kubernetes.client.openapi.ApiException;
  import io.kubernetes.client.openapi.apis.CoreV1Api;
  import java.time.Duration;
```

The `Deployment` resource of an NGINX webserver is more or less **created in the cluster in the same way as the `ConfigMap`**
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 48
description: Construct V1Deployment object, and apply it to Kubernetes (1/2)
text: |2

          try {
              var deployment = getDeployment(name, resource);
              applyDeployment(deployment);
          } catch (ApiException | IOException e) {
              log.error("Applying Deployment for Foo " + namespace + "/" + name + " failed", e);
              return new Result(true, Duration.ofSeconds(10));
          }

```
As for a Deployment, there is a lot more to configure than for a ConfigMap resource, we'll use a YAML template with placeholders to be replaced with values for the individual Foo resources.
```editor:append-lines-to-file
file: ~/controller/src/main/resources/deployment-template.yaml
description: Add deployment YAML template to resources
text: |2
  apiVersion: apps/v1
  kind: Deployment
  metadata:
    name: NAME
    namespace: NAMESPACE
  spec:
    selector:
      matchLabels:
        app: NAME
    replicas: 2
    template:
      metadata:
        labels:
          app: NAME
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
              name: NAME
```
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 88
description: Add method to load YAML template and replace placeholders
text: |2

      private V1Deployment getDeployment(String name, V1Foo resource) throws IOException {
          var deploymentYaml = Files.readString(Path.of(new ClassPathResource(
                  "deployment-template.yaml").getURI()));
          deploymentYaml = deploymentYaml.replaceAll("NAMESPACE", resource.getMetadata().getNamespace())
          		    .replaceAll("NAME", name);
          return Yaml.loadAs(deploymentYaml, V1Deployment.class);
      }
```
The static `Yaml.loadAs` method of the Kubernetes Java Client provides the functionality to map a YAML string into the related resource instance class.
```editor:select-matching-text
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
description: Select constructor to add dependency injection of AppsV1Api instance
text: "public FooReconciler(SharedIndexInformer<V1Foo> informer, CoreV1Api coreV1Api) {"
```
```editor:replace-text-selection
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
description: Add dependency injection of AppsV1Api instance
text: |2
  private final AppsV1Api appsV1Api;
      public FooReconciler(SharedIndexInformer<V1Foo> informer, CoreV1Api coreV1Api, AppsV1Api appsV1Api) {
          this.appsV1Api = appsV1Api;
```
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 99
description: Construct V1Deployment object, and apply it to Kubernetes (1/2)
text: |2

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
```
As the implementation of the `applyDeployment` is similar to the `applyConfigMap` method, we could remove some lines of duplicate code by using a `GenericKubernetesApi` instead of the strongly typed ones but we would lose the higher abstraction.
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 17
description: Add imports
text: |2
  import io.kubernetes.client.openapi.apis.AppsV1Api;
  import io.kubernetes.client.util.Yaml;
  import org.springframework.core.io.ClassPathResource;
  import java.io.IOException;
  import java.nio.file.Files;
  import java.nio.file.Path;
```

Finally, we have to provide an instance of our reconciler and the API classes as a bean.
**Using the injected `ApiClient` instance is important here**, as otherwise the APIs will be configured to use the default configuration and not what you for example provide as configuration with a mounted Kubernetes service account token.
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/ControllerConfiguration.java
line: 33
description: Provide an instance of our reconciler and the API classes as a bean
text: |2

      @Bean
      AppsV1Api appsV1Api(ApiClient apiClient) {
          return new AppsV1Api(apiClient);
      }

      @Bean
      CoreV1Api coreV1Api(ApiClient apiClient) {
          return new CoreV1Api(apiClient);
      }

      @Bean
      Reconciler reconciler(SharedIndexInformer<V1Foo> parentInformer,
                            CoreV1Api coreV1Api,
                            AppsV1Api appsV1Api) {
          return new FooReconciler(parentInformer, coreV1Api, appsV1Api);
      }
```
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/ControllerConfiguration.java
line: 17
text: |2
  import io.kubernetes.client.openapi.apis.AppsV1Api;
  import io.kubernetes.client.openapi.apis.CoreV1Api;
```