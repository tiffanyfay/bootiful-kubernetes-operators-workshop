The **Kubernetes declarative API enforces a separation of responsibilities**. You already **declared the desired state of your resource**. 
Now it's time to create a **Kubernetes controller that implements the actual functionality and keeps the current state of Kubernetes objects in sync with your declared desired state**. This is in contrast to an imperative API, where you instruct a server what to do.

As the title of this workshop implies, we will use Spring Boot to implement our custom controller.

#### Getting Started with Your Spring Boot-based Controller
The **best way to start building it from scratch** is to go to [start.spring.io](https://start.spring.io) and generate a new project with the latest **Spring Boot 3** version.
You have several other options to choose from, for this workshop, we're fine with the defaults like **Gradle-Groovy** as dependency management / build automation tool.

**A project with the configuration is already generated for you, so you only have to unzip it.**
```terminal:execute
command: |
  cd ~ && unzip samples/controller.zip
clear: true
```

The **Spring Initializr will get us most of the way** (it does a lot of code generation), but we **need to add the official Java client for Kubernetes dependency** not available at start.spring.io.

```editor:insert-lines-before-line
file: controller/build.gradle
line: 20
description: Add additional dependencies
text: |1
     implementation 'io.kubernetes:client-java-spring-integration:19.0.1'
```

#### Generating Java Classes for the CRD
In order to work with the Kubernetes CRD, we would **need to get Java class representations of the CRD**.

To **generate the class files from a custom resource definition**, there is a [containerized utility](https://github.com/kubernetes-client/java/blob/master/docs/generate-model-from-third-party-resources.md#remote-generate-via-github-action) available.

As this utility will create a [kind](https://kind.sigs.k8s.io/) Kubernetes cluster to fetch the OpenApi specification of the custom resource from it, we'll use a **more lightweight approach in this workshop and download already generated Java classes by a [GitHub Action using this containerized utility](https://github.com/kubernetes-client/java/blob/master/docs/generate-model-from-third-party-resources.md#remote-generate-via-github-action).**
```terminal:execute
command: |
  wget https://github.com/timosalm/bootiful-kubernetes-operators-workshop/releases/download/2023-10-08/generated-crd-classes.zip && unzip generated-crd-classes.zip && cp -r src/main/java/io/spring/controller/ controller/src/main/java/io/spring && rm -rf src && rm generated-crd-classes.zip
description: Download generated Java classes and move them to controller project
clear: true
```
```editor:open-file
file: controller/src/main/java/io/spring/controller/models/V1Foo.java
```

Now we have the Java class representations of the CRD and can start with the implementation of the controller itself.

#### Implementing the Controller

The **Kubernetes Java Client provides everything we need to implement the controller**. 

The **`ControllerBuilder` builder library helps us to configure the basic controller functionality** and provide an instance of it.
```editor:append-lines-to-file
file: ~/controller/src/main/java/io/spring/controller/ControllerConfiguration.java
description: Create initial controller configuration
text: |2
  package io.spring.controller;
  
  import io.kubernetes.client.extended.controller.Controller;
  import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
  import io.kubernetes.client.extended.controller.reconciler.Reconciler;
  import io.kubernetes.client.informer.SharedIndexInformer;
  import io.kubernetes.client.informer.SharedInformerFactory;
  import io.spring.controller.models.V1Foo;
  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;

  import java.time.Duration;

  @Configuration
  public class ControllerConfiguration {

      @Bean
      Controller controller(SharedInformerFactory sharedInformerFactory,
                            SharedIndexInformer<V1Foo> informer,
                            Reconciler reconciler) {
          return ControllerBuilder
                  .defaultBuilder(sharedInformerFactory)
                  .watch(q -> ControllerBuilder
                          .controllerWatchBuilder(V1Foo.class, q)
                          .withResyncPeriod(Duration.ofSeconds(30))
                          .build())
                  .withReconciler(reconciler)
                  .withReadyFunc(informer::hasSynced)
                  .withWorkerCount(2)
                  .build();
      }
  }
```
For the configuration of our controller instance, we are injecting three dependencies:
- A `SharedIndexInformer` is a **special form for an Informer**, which is a **cache for a resource**, so the **controller does not need to continuously poll the Kubernetes cluster** (API server) to check if there are any new CRD instances created, updated, or deleted
- The `SharedInformerFactory` class is used to **construct and register all defined informers** for different API types in a controller
- The `Reconciler` **implements the functionality of related resources** and will be invoked when they are created, updated, deleted, etc.

The static `ControllerBuilder.defaultBuilder` method creates a builder instance with several default values, which can be overridden. It accepts the provided **SharedInformerFactory instance as a parameter to validate that an informer for the resource to be watched is registered - in our case the V1Foo class**.

**To configure the resources to be watched**, and the interval **we are using the static `ControllerBuilder.controllerWatchBuilder` method**.

The provided `Reconciler` is set via the `withReconciler` method, and the **`withReadyFunc` defines a pre-flight check** that has to be fulfilled before the controller can run. In this case, whether the shared informer's store has synced.

**For this demo, we reduce the worker thread count to two from the current default of 16**.

After doing all the initialization, we've to **start our registered informer and the controller**. 
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/ControllerConfiguration.java
line: 32
description: Start registered informer and the controller
text: |2

      @Bean
      public CommandLineRunner commandLineRunner(SharedInformerFactory sharedInformerFactory, Controller controller) {
          return args -> Executors.newSingleThreadExecutor().execute(() -> {
              sharedInformerFactory.startAllRegisteredInformers();
              controller.run();
          });
      }
```
```editor:insert-lines-before-line
file: ~/controller/src/main/java/io/spring/controller/ControllerConfiguration.java
line: 11
description: Add imports
text: |2
  import org.springframework.boot.CommandLineRunner;
  import java.util.concurrent.Executors;
```


If we run the tests of our application, we can see that things are broken.
```terminal:execute
command: |
  (cd controller && ./gradlew bootTest)
clear: true
```
```
Parameter 1 of method controller in io.spring.controller.ControllerConfiguration required a bean of type 'io.kubernetes.client.informer.SharedIndexInformer' that could not be found.
```

Let's configure a bean of type SharedIndexInformer<V1Foo> in the following section.
