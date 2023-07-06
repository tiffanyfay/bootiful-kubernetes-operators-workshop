We’ll need a new project from the [Spring Initializr](https://start.spring.io/) with Spring Boot 3.0 or later. Maven or Gradle-Groovy can be used; we are using Gradle-Groovy for this workshop. We also need GraalVM Native Support and Lombok dependencies.

If you're later creating this from the actual tool then you also need to set these:

- Group: io.spring
- Artifact: controllers
- Package: io.spring

By default it would have put everything in the package io.spring.controllers, but to keep things simpler, we’ve moved everything up one package.

#### Customize the build file
The Spring Initializr will get us most of the way (it does a lot of code generation) but we need to add an extra dependency – the official Java client for Kubernetes with AOT.

```editor:open-file
file: samples/build.gradle
line: 1
```

```editor:insert-lines-before-line
file: samples/build.gradle
line: 27
text: "    implementation 'io.kubernetes:client-java-spring-aot-integration:17.0.0'"
```
