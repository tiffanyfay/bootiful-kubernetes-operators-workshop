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
line: 26
text: |2
      var lister = new Lister<>(informer.getIndexer(), request.getNamespace());
      var resource = lister.get(request.getName());
      if (resource == null || resource.getMetadata().getDeletionTimestamp() != null) {
            return new Result(false);
      }

```
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 23
text: |2

    private final SharedIndexInformer<V1Foo> informer;
    public FooReconciler(SharedIndexInformer<V1Foo> informer) {
        this.informer = informer;
    }
```
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 7
text: |2
  import io.kubernetes.client.informer.cache.Lister;
  import io.kubernetes.client.informer.SharedIndexInformer;
  import io.spring.controller.models.V1Foo;
```

Create ConfigMap.
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 26
text: |2
      var configMapContent = Map.of("index.html", "<h1> Hello, " + resource.getSpec().getNickname() + " </h1>");
      var configMap = getConfigMap(name, resource, configMapContent);

```
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 23
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
line: 23
text: |2

    private V1OwnerReference getOwnerReference(V1Foo owner) {
      return new V1OwnerReferenceBuilder().withApiVersion(owner.getApiVersion()).withKind(owner.getKind())
              .withName(owner.getMetadata().getName()).withUid(owner.getMetadata().getUid()).withController().build();
    }
```
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 7
text: |2
  import io.kubernetes.client.openapi.models.*;
  import java.util.Map;
  import java.util.Collections;
```

Apply ConfigMap.
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 26
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
line: 23
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
line: 7
text: |2
  import io.kubernetes.client.openapi.ApiException;
  import io.kubernetes.client.openapi.apis.CoreV1Api;
  import java.time.Duration;
```

Create and apply Deployment.
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/FooReconciler.java
line: 26
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
line: 23
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
line: 23
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
line: 7
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
line: 30
text: |2

    @Bean
    Reconciler reconciler(SharedIndexInformer<V1Foo> parentInformer) {
        return new FooReconciler(parentInformer);
    }
```

**TMP**

Next, we'll need to define the `Reconciler` itself which is biggest chunk.

We'll need a new thing, called `AppsV1Api`. This API sidesteps all the caching and indexing and allows us to talk directly to the API server. You could achieve this without using the API, but it simplifies things sometimes and it's instructional to see it in action, so:

```editor:insert-lines-before-line
file: samples/src/main/java/io/spring/ControllersApplication.java
line: 12
text: |1

  @Bean
  AppsV1Api appsV1Api(ApiClient apiClient) {
    return new AppsV1Api(apiClient);
  }
```

And then one as well for `CoreV1Api` for a `ConfigMap`:
```editor:insert-lines-before-line
file: samples/src/main/java/io/spring/ControllersApplication.java
line: 12
text: |1

  @Bean
  CoreV1Api coreV1Api(ApiClient apiClient) {
    return new CoreV1Api(apiClient);
  }
```

The reconciler will take in a YAML ConfigMap and Deployment so that we have less code to write here. TODO write this better.

The Deployment is just a basic NGINX deployment:
```editor:open-file
file: samples/src/main/resources/deployment.yaml
```

And the ConfigMap allows us to modify the the index.html page:
```editor:open-file
file: samples/src/main/resources/configmap.yaml
```

Now we can create the reconciler. It takes in these YAML files as well as the informers and AppsV1Api and CoreV1Api.

```editor:insert-lines-before-line
file: samples/src/main/java/io/spring/ControllersApplication.java
line: 52
text: |1

  /**
   * the Reconciler won't get an event telling it that the cluster has changed, but
   * instead it looks at cluster state and determines that something has changed
   */
  @Bean
  Reconciler reconciler(@Value("classpath:configmap.yaml") Resource configMapYaml,
              @Value("classpath:deployment.yaml") Resource deploymentYaml,
              SharedIndexInformer<V1Foo> v1FooSharedIndexInformer, AppsV1Api appsV1Api, CoreV1Api coreV1Api) {
    return request -> {
      try {

      } //
      catch (Throwable e) {
        log.error("we've got an outer error.", e);
        return new Result(true, Duration.ofSeconds(60));
      }
      return new Result(false);
    };
  }
```

TODO:
```editor:insert-lines-before-line
file: samples/src/main/java/io/spring/ControllersApplication.java
line: 63
text: |1
        // create new one on k apply -f foo.yaml
        String requestName = request.getName();
        String key = request.getNamespace() + '/' + requestName;
        V1Foo foo = v1FooSharedIndexInformer.getIndexer().getByKey(key);
        if (foo == null) { // deleted. we use ownerreferences so dont need to do
          // anything special here
          return new Result(false);
        }

        String namespace = foo.getMetadata().getNamespace();
        String pretty = "true";
        String dryRun = null;
        String fieldManager = "";
        String fieldValidation = "";
```

TODO:
```editor:insert-lines-before-line
file: samples/src/main/java/io/spring/ControllersApplication.java
line: 79
text: |1
        // parameterize configmap
        String configMapName = "configmap-" + requestName;
        V1ConfigMap configMap = loadYamlAs(configMapYaml, V1ConfigMap.class);
        String html = "<h1> Hello, " + foo.getSpec().getName() + " </h1>";
        configMap.getData().put("index.html", html);
        configMap.getMetadata().setName(configMapName);
        createOrUpdate(V1ConfigMap.class, () -> {
          addOwnerReference(requestName, foo, configMap);
          return coreV1Api.createNamespacedConfigMap(namespace, configMap, pretty, dryRun, fieldManager,
              fieldValidation);
        }, () -> coreV1Api.replaceNamespacedConfigMap(configMapName, namespace, configMap,
              pretty, dryRun, fieldManager, fieldValidation));
```

TODO:
```editor:insert-lines-before-line
file: samples/src/main/java/io/spring/ControllersApplication.java
line: 91
text: |1

        // parameterize deployment
        String deploymentName = "deployment-" + requestName;
        V1Deployment deployment = loadYamlAs(deploymentYaml, V1Deployment.class);
        deployment.getMetadata().setName(deploymentName);
        List<V1Volume> volumes = deployment.getSpec().getTemplate().getSpec().getVolumes();
        Assert.isTrue(volumes.size() == 1, () -> "there should be only one V1Volume");
        volumes.forEach(vol -> vol.getConfigMap().setName(configMapName));
        createOrUpdate(V1Deployment.class, () -> {
          deployment.getSpec().getTemplate().getMetadata()
              .setAnnotations(Map.of("bootiful-update", Instant.now().toString()));
          addOwnerReference(requestName, foo, deployment);
          return appsV1Api.createNamespacedDeployment(namespace, deployment, pretty, dryRun, fieldManager,
              fieldValidation);
        }, () -> {
          updateAnnotation(deployment);
          return appsV1Api.replaceNamespacedDeployment(deploymentName, namespace, deployment, pretty, dryRun,
              fieldManager, fieldValidation);
        });
```

Here's where the rubber meets the road: our reconciler will create a new `Deployment` and `ConfigMap` every time a new `Foo` is created. We like you too much to programmatically build up the `Deployment` from scratch in Java, so we'll just reuse a pre-written YAML definition (`/deployment.yaml`) of a `Deployment` and then reify it, changing some of its parameters, and submit that.

```editor:insert-lines-before-line
file: samples/src/main/java/io/spring/ControllersApplication.java
line: 117
text: |1

  static class FooControllerRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
      for (var path : new String[] { "/configmap.yaml", "/deployment.yaml" }) {
        hints.resources().registerResource(new ClassPathResource(path));
      }
    }
  }
```

```editor:insert-lines-before-line
file: samples/src/main/java/io/spring/ControllersApplication.java
line: 127
text: |1

  @SneakyThrows
  private static <T> T loadYamlAs(Resource resource, Class<T> clzz) {
    var yaml = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
    return Yaml.loadAs(yaml, clzz);
  }
```

We need to define `createOrUpdate`. It is as the name tells -- if the resource doesn't exist, create it, else update it.

```editor:insert-lines-before-line
file: samples/src/main/java/io/spring/ControllersApplication.java
line: 133
text: |1

  static private <T> void createOrUpdate(Class<T> clazz, ApiSupplier<T> creator, ApiSupplier<T> updater) {
    try {
      creator.get();
      log.info("It worked! we created a new " + clazz.getName() + "!");
    } //
    catch (ApiException throwable) {
      int code = throwable.getCode();
      if (code == 409) { // already exists
        log.info("the " + clazz.getName() + " already exists. Replacing.");
        try {
          updater.get();
          log.info("successfully updated the " + clazz.getName());
        }
        catch (ApiException ex) {
          log.error("got an error on update", ex);
        }
      } //
      else {
        log.info("got an exception with code " + code + " while trying to create the " + clazz.getName());
      }
    }
  }

  @FunctionalInterface
  interface ApiSupplier<T> {

    T get() throws ApiException;

  }
```

We also need to update the Deployment annotation:

```editor:insert-lines-before-line
file: samples/src/main/java/io/spring/ControllersApplication.java
line: 156
text: |1

  private void updateAnnotation(V1Deployment deployment) {
    Objects.requireNonNull(Objects.requireNonNull(deployment.getSpec()).getTemplate().getMetadata())
        .setAnnotations(Map.of("bootiful-update", Instant.now().toString()));
  }
```

And add the owner reference:

```editor:insert-lines-before-line
file: samples/src/main/java/io/spring/ControllersApplication.java
line: 161
text: |1

  private static V1ObjectMeta addOwnerReference(String requestName, V1Foo foo, KubernetesObject kubernetesObject) {
    Assert.notNull(foo, () -> "the V1Foo must not be null");
    return kubernetesObject.getMetadata().addOwnerReferencesItem(new V1OwnerReference().kind(foo.getKind())
        .apiVersion(foo.getApiVersion()).controller(true).uid(foo.getMetadata().getUid()).name(requestName));
  }
```




TMP 


Now it's getting a little bit more complex. Controllers are the core of Kubernetes. It’s a controller’s job to ensure that, for any given object, the actual state of the world (both the cluster state and potentially external state like running containers for Kubelet or load balancers for a cloud provider) matches the desired state in the object. Each controller focuses on one root Kubernetes resource but may interact with other Kubernetes resources. We call this process reconciling. In our case the Kubernetes resource is a custom resource definition called GitHubRepository.

file: tap-cartographer-workshop/github-source-controller/k8s/crds/github-repository-crd.yaml
Let's now have a look at our reconcile function.

file: tap-cartographer-workshop/github-source-controller/src/main/java/com/example/GitHubSourceReconciler.java
It's passed a reconciliation request that includes information about a custom resource of the type GitHubRepository which's state has to be matched to the actual state of the world (in our example every 30 seconds). The status.artifact information of the custom resource will be updated with the latest revision (status.artifact.revision) and tarball url (status.artifact.url) from the HashMap for the configured Git repository and branch if available.

To have actual instances of the Reconciler and Controller running in our application, we have to register them as Beans and provide all the required parameters via additional configuration.