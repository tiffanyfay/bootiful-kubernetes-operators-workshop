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