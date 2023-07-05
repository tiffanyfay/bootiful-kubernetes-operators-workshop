## Stage

We copied the contents of `bin` from the source code at https://github.com/kubernetes-native-java/controllers-101 to the new project generated from the Spring Initializr.


#### Deploy the CRD to Kubernetes

Create CRD:

```terminal:execute
command: |
    kubectl apply -f samples/bin/foo.yaml
```

We should be able to do:

```terminal:execute
command: |
    kubectl get crds foos.spring.io
```
And see the newly minted CRD.

Let's take a look at `bin/foo.yaml`

```editor:open-file
file: samples/bin/foo.yaml
line: 1
```

TODO: explain what is happening in the file

#### Run the Code Generator

We'll need a little script to help us code-generate the Java code for our CRD.

```terminal:execute
command: |
    ./bin/regen_crds.sh
```

#### And then a Miracle Happens 

There are several main concepts we need to understand before the code we're about to create makes sense.