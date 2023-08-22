A `SharedIndexInformer`, which you are already aware of, is a **special form of an Informer, which is a cache for a resource**, so the **controller does not need to continuously poll the Kubernetes cluster** (API server) to check if there are any CRD updates.

There are `SharedInformer`s that **share data across multiple instances of the `Informer`** so that they're not duplicated. A `SharedInformer` has a shared data cache and is **capable of distributing notifications for changes to the cache to multiple listeners who registered with it**. There is one behavior change compared to a standard `Informer`: when you receive a notification, the cache will be _at least_ as fresh as the notification, but it _may_ be more fresh. You should not depend on the contents of the cache exactly matching the state implied by the notification. The notification is binding.

`SharedIndexInformer` only adds one more thing to the picture: the **ability to look up items by various keys**. 

To **configure a bean of type SharedIndexInformer<V1Foo>**, we first need to create a `GenericKubernetesApi` for our generated Foo classes.
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/ControllerConfiguration.java
line: 18
description: Create GenericKubernetesApi for Foo classes
text: |2

      @Bean
      GenericKubernetesApi<V1Foo, V1FooList> foosApi(ApiClient apiClient) {
        return new GenericKubernetesApi<>(V1Foo.class, V1FooList.class, "spring.io", "v1", "foos", apiClient);
      }
```

Now, we'll leverage the `SharedInformerFactory` class, which you are also already aware of, to construct and register the `SharedIndexInformer` for our Foo custom resource.
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/ControllerConfiguration.java
line: 23
description: Configure SharedIndexInformer
text: |2

      @Bean
      SharedIndexInformer<V1Foo> foosSharedIndexInformer(SharedInformerFactory sharedInformerFactory,
          GenericKubernetesApi<V1Foo, V1FooList> api) {
        return sharedInformerFactory.sharedIndexInformerFor(api, V1Foo.class, 0);
      }
```
We also have to add the missing imports.
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/ControllerConfiguration.java
line: 9
description: Add imports
text: |2
  import io.spring.controller.models.V1FooList;
  import io.kubernetes.client.util.generic.GenericKubernetesApi;
  import io.kubernetes.client.openapi.ApiClient;
```

If we run the tests of our application again, we can see that things are still broken.
```terminal:execute
command: |
  (cd controller && ./gradlew bootTest)
clear: true
```
```
Parameter 2 of method controller in io.spring.controller.ControllerConfiguration required a bean of type 'io.kubernetes.client.extended.controller.reconciler.Reconciler' that could not be found.
```

In the next section, we will finally implement the functionality of our custom resource in the form of a **Reconciler**, which will be provided as a bean to fix the error.