The Spring Boot Gradle plugin automatically configures AOT tasks when the GraalVM Native Image plugin is applied. You should check that your Gradle build contains a plugins block that includes org.graalvm.buildtools.native.

Let's add the `org.graalvm.buildtools.native` plugin, so the `bootBuildImage task will generate a native image rather than a JVM one.
```editor:insert-lines-before-line
file: controller/build.gradle
line: 5
text: |1
     id 'org.graalvm.buildtools.native' version '0.9.23'
```

```terminal:execute
command: (cd ~/controller/ && ./gradlew bootBuildImage --imageName={{ registry_host }}/foo-controller)
clear: true
```



**TMP**

> TODO text here and check where this file saves

```terminal:execute
command: |
    ./gradlew nativeCompile
```

> [name=tiffany jernigan 🐙]
> Figure out exactly what resources need removing
>

Delete the existing resources so you can see that it's creating new ones (otherwise you can see updates):
```terminal:execute
command: |
    kubectl delete deploy deployment-demo
    kubectl delete configmap configmap-demo
```

Re-apply:
```terminal:execute
command: |
    kubectl apply -f bin/test.yaml
```

Run the binary listed in the run output. For instance this could be `./build/native/nativeCompile/controllers`.

> TODO deal with this
Port-forward the deployment so we can look at in the browser without needing to create a service:
```terminal:execute
kubectl port-forward deployment/deployment-demo 8080:80
```

Go to `localhost:8080` in a browser window.

Run `ctrl-C` for both the port-forward and your run.

Congrats, you did it!