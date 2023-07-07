As a reminder:
Kubernetes is essentially a collection of controllers (or "reconciliation loops") that watch the desired state of the system (stored in the infamous "YAML database" that we were evoking earlier, exposed through the Kubernetes API server, and typically implemented in the back-end with etcd) and the actual state of the system. Whenever the two states differ (either because someone made changes to the desired state, or because something happened and changed the actual state), the relevant controller will try its best to reconcile both states by taking actions to change the actual state.

So, logically, a [Kubernetes operator](https://kubernetes.io/docs/concepts/extend-kubernetes/operator/) is two things: a CRD definition and a controller that reacts to the lifecycle of new instances of that CRD. We've already defined the CRD itself and looked at the generated code for the CRD instance itself. We're halfway there! We just need the controller itself. That'll be our first Spring `@Bean`.

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