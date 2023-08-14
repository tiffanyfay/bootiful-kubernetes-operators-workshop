What's an **Informer**, you ask? An informer "is a" - and we're not making this up - controller together with the ability to distribute its `Queue`-related operations to an appropriate event handler. Instead of needing to continually query the Kubernetes API an informer stores data in a cache.

There are `SharedInformer`s that share data across multiple instances of the `Informer` so that they're not duplicated. A `SharedInformer` has a shared data cache and is capable of distributing notifications for changes to the cache to multiple listeners who registered with it. There is one behavior change compared to a standard `Informer`: when you receive a notification, the cache will be _at least_ as fresh as the notification, but it _may_ be more fresh. You should not depend on the contents of the cache exactly matching the state implied by the notification. The notification is binding.

`SharedIndexInformer` only adds one more thing to the picture: the ability to lookup items by various keys. So, a controller sometimes needs a conceptually-a-controller to be a controller. Got it? Got it.

First, we need to create a `GenericKubernetesApi` for our generated Foo classes.

```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/ControllerConfiguration.java
line: 9
text: |2
  import io.spring.controller.models.V1FooList;
  import io.kubernetes.client.util.generic.GenericKubernetesApi;
  import io.kubernetes.client.openapi.ApiClient;
```
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/ControllerConfiguration.java
line: 16
text: |2

    @Bean
    GenericKubernetesApi<V1Foo, V1FooList> foosApi(ApiClient apiClient) {
      return new GenericKubernetesApi<>(V1Foo.class, V1FooList.class, "spring.io", "v1", "foos", apiClient);
    }
```

Now, let's add the `SharedIndexInformer`.

```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/ControllerConfiguration.java
line: 24
text: |2

    @Bean
    SharedIndexInformer<V1Foo> foosSharedIndexInformer(SharedInformerFactory sharedInformerFactory,
        GenericKubernetesApi<V1Foo, V1FooList> api) {
      return sharedInformerFactory.sharedIndexInformerFor(api, V1Foo.class, 0);
    }
```

If we run the tests of our applications, we can see that things are still broken.
```terminal:execute
command: |
  (cd controller && ./gradlew test)
clear: true
```
```
Parameter 2 of method controller in io.spring.controller.ControllerConfiguration required a bean of type 'io.kubernetes.client.extended.controller.reconciler.Reconciler' that could not be found.
```

In the next section, we will finally implement the functionality of our customer resource in the form of a **Reconciler**, which will be provided as a bean to fix the error.