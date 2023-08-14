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

The Spring Initializr will get us most of the way (it does a lot of code generation) but we need to add two dependencies not available at start.spring.io. The official Java client for Kubernetes, and integration with the JSON mapping library Jackson.

```editor:insert-lines-before-line
file: controller/build.gradle
line: 20
text: |1
     implementation 'io.kubernetes:client-java-spring-integration:18.0.1'
     implementation 'org.springframework.boot:spring-boot-starter-json'
```

#### Generating Java Classes for the CRD
In order to work with the Kubernetes CRD, we would need to get Java class representations of the CRD.

To generate the class files from a custom resource definition, there is a [containerized utility](https://github.com/kubernetes-client/java/blob/master/docs/generate-model-from-third-party-resources.md#remote-generate-via-github-action) available.

As this utility will create a [kind](https://kind.sigs.k8s.io/) Kubernetes cluster to fetch the OpenApi specification of the custom resource from it, we'll use a more lightweight approach in this workshop and download already generated Java classes by a [GitHub Action using this containerized utility](https://github.com/kubernetes-client/java/blob/master/docs/generate-model-from-third-party-resources.md#remote-generate-via-github-action). 
```terminal:execute
command: |
  wget https://github.com/tiffanyfay/bootiful-kubernetes-operators-workshop/releases/download/2023-08-13/generated-crd-classes.zip && unzip generated-crd-classes.zip && cp -r src/main/java/io/spring/controller/ controller/src/main/java/io/spring && rm -rf src && rm generated-crd-classes.zip
description: Download generated Java classes and move them to controller project
clear: true
```
```editor:open-file
file: controller/src/main/java/io/spring/controller/models/V1Foo.java
```

Now we have the Java class representations of the CRD and can start with the implementation of the controller itself.

#### Implementing the Controller

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
          var builder = ControllerBuilder
                  .defaultBuilder(sharedInformerFactory)
                  .watch(q -> ControllerBuilder
                          .controllerWatchBuilder(V1Foo.class, q)
                          .withResyncPeriod(Duration.ofSeconds(30))
                          .build())
                  .withWorkerCount(2);
          return builder
                  .withReconciler(reconciler)
                  .withReadyFunc(informer::hasSynced)
                  .withName("fooController")
                  .build();
      }
  }
```

If we run the tests of our applications, we can see that things are broken.
```terminal:execute
command: |
  (cd controller && ./gradlew test)
clear: true
```
```
No qualifying bean of type 'io.kubernetes.client.informer.SharedIndexInformer<io.spring.controller.models.V1Foo>' available
``

Let's configure a bean of type SharedIndexInformer<V1Foo> in the following section.