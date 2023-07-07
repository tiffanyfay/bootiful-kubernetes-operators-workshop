What's an `Informer`, you ask? An informer "is a" - and we're not making this up - controller together with the ability to distribute its `Queue`-related operations to an appropriate event handler. Instead of needing to continually query the Kubernetes API an informer stores data in a cache.

There are `SharedInformer`s that share data across multiple instances of the `Informer` so that they're not duplicated. A `SharedInformer` has a shared data cache and is capable of distributing notifications for changes to the cache to multiple listeners who registered with it. There is one behavior change compared to a standard `Informer`: when you receive a notification, the cache will be _at least_ as fresh as the notification, but it _may_ be more fresh. You should not depend on the contents of the cache exactly matching the state implied by the notification. The notification is binding.

`SharedIndexInformer` only adds one more thing to the picture: the ability to lookup items by various keys. So, a controller sometimes needs a conceptually-a-controller to be a controller. Got it? Got it.

Let's add the `SharedIndexInformer<V1Foo>`.

```editor:insert-lines-before-line
file: samples/src/main/java/io/spring/ControllersApplication.java
line: 12
text: |1

	@Bean
	SharedIndexInformer<V1Foo> foosSharedIndexInformer(SharedInformerFactory sharedInformerFactory,
			GenericKubernetesApi<V1Foo, V1FooList> api) {
		return sharedInformerFactory.sharedIndexInformerFor(api, V1Foo.class, 0);
	}
```

This in turn implies a dependency on `GenericKubernetesApi<V1Foo,V1FooList>`.

```editor:insert-lines-before-line
file: samples/src/main/java/io/spring/ControllersApplication.java
line: 12
text: |1

	@Bean
	GenericKubernetesApi<V1Foo, V1FooList> foosApi(ApiClient apiClient) {
		return new GenericKubernetesApi<>(V1Foo.class, V1FooList.class, "spring.io", "v1", "foos", apiClient);
	}
```