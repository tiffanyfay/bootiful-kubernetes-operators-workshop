The Kubernetes declarative API enforces a separation of responsibilities. You already declare the desired state of your resource. 
Now it's time to create a Kubernetes controller that implements the actual functionality and keeps the current state of Kubernetes objects in sync with your declared desired state. This is in contrast to an imperative API, where you instruct a server what to do.

As the title of this workshop implies, we will use Spring Boot to implement our custom controller.

#### Getting Started with Your Spring Boot-based Controller
The best way to start building it from scratch is to go to [start.spring.io](https://start.spring.io) and generate a new project with the latest **Spring Boot 3** version.
You have several other options to choose from, for this workshop, we're fine with the defaults like **Gradle-Groovy** as dependency management / build automation tool.

A project with the configuration is already generated for you, so you only have to unzip it.
```terminal:execute
command: |
  cd ~ && unzip samples/controller.zip
clear: true
```

The Spring Initializr will get us most of the way (it does a lot of code generation) but we need to add a dependency not available at start.spring.io – the official Java client for Kubernetes.

```editor:insert-lines-before-line
file: controller/build.gradle
line: 20
text: |1
     implementation 'io.kubernetes:client-java-spring-integration:18.0.1'
```

#### Generating Java Classes for the CRD
In order to work with the Kubernetes CRD, we would need to get Java class representations of the CRD.

To generate the class files from a custom resource definition, there is a [containerized utility available](https://github.com/kubernetes-client/java/blob/master/docs/generate-model-from-third-party-resources.md).

As this utility will create a [kind](https://kind.sigs.k8s.io/) cluster to fetch the OpenApi specification of the custom resource from it, we'll use a more lightweight approach and do the same from our workshop cluster.
```terminal:execute
command: |
  mkdir generated && cd $_

  kubectl get --raw="/openapi/v2" > openapi.yaml

  docker run --rm --user 1001:1001 -v $PWD:/local \
    openapitools/openapi-generator-cli generate \
      -i /local/openapi.yaml -g java -o /local/result \
      --additional-properties=modelPackage="io.spring.controller.models" \
      --global-property=models="io.spring.v1.Foo:io.spring.v1.FooList"

  cd .. && cp -r generated/result/src/main/java/io/spring/controller/ controller/src/main/java/io/spring && rm -rf generated
clear: true
```
#### TMP

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