As a reminder:
Kubernetes is essentially a collection of controllers (or "reconciliation loops") that watch the desired state of the system (stored in the infamous "YAML database" that we were evoking earlier, exposed through the Kubernetes API server, and typically implemented in the back-end with etcd) and the actual state of the system. Whenever the two states differ (either because someone made changes to the desired state, or because something happened and changed the actual state), the relevant controller will try its best to reconcile both states by taking actions to change the actual state.

So, logically, a [Kubernetes operator](https://kubernetes.io/docs/concepts/extend-kubernetes/operator/) is two things: a CRD definition and a controller that reacts to the lifecycle of new instances of that CRD. We've already defined the CRD itself and looked at the generated code for the CRD instance itself. We're halfway there! We just need the controller itself. That'll be our first Spring `@Bean`.

TODO: explain what is happening in the file

We’ll need a new project from the [Spring Initializr](https://start.spring.io/) with **Spring Boot 3.0 or later**. Maven or Gradle-Groovy can be used; we are using **Gradle-Groovy** for this workshop. We also need **GraalVM Native Support** and **Lombok** dependencies.

If you're later creating this from the actual tool then you also need to set these:

- Group: `io.spring`
- Artifact: `controllers`
- Package: `io.spring`

By default it would have put everything in the package `io.spring.controllers`, but to keep things simpler, we’ve moved everything up one package.

#### Customize the build file
The Spring Initializr will get us most of the way (it does a lot of code generation) but we need to add an extra dependency – the official Java client for Kubernetes with AOT.

```editor:open-file
file: samples/build.gradle
```

```editor:insert-lines-before-line
file: samples/build.gradle
line: 27
text: |1
     implementation 'io.kubernetes:client-java-spring-aot-integration:17.0.0'
```


#### Run the Code Generator

We'll need a little script to help us code-generate the Java code for our CRD.

```shell
    ./samples/bin/regen_crds.sh
```

We already ran it and the files are in `samples/src/main/java/io/spring/models`.




```editor:insert-lines-before-line
file: samples/src/main/java/io/spring/ControllersApplication.java
line: 12
text: |1

    @Bean(destroyMethod = "shutdown")
    Controller controller(SharedInformerFactory sharedInformerFactory,
                          SharedIndexInformer<V1Foo> fooNodeInformer,
                          Reconciler reconciler) {
        var builder = ControllerBuilder //
                .defaultBuilder(sharedInformerFactory)//
                .watch((q) -> ControllerBuilder //
                        .controllerWatchBuilder(V1Foo.class, q)
                        .withResyncPeriod(Duration.ofHours(1)).build() //
                ) //
                .withWorkerCount(2);
        return builder
                .withReconciler(reconciler) //
                .withReadyFunc(fooNodeInformer::hasSynced) // optional: only start once the index is synced
                .withName("fooController") ///
                .build();

    }
```

Things are broken! We don't have any of the three dependencies expressed here: `SharedInformerFactory`, `SharedIndexInformer<V1Foo>`, and `Reconciler`.