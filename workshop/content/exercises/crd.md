The CustomResourceDefinition API resource allows you to define custom resources. Defining a CRD object creates a new custom resource with a name and schema that you specify. The Kubernetes API serves and handles the storage of your custom resource. 

Run the following command to generate the Foo the custom resource definition.
```editor:append-lines-to-file
file: ~/foo-crd.yaml
description: Generate Foo custom resource definition
text: |2
  apiVersion: apiextensions.k8s.io/v1
  kind: CustomResourceDefinition
  metadata:
    name: foos.example.com
  spec:
    group: example.com
    names:
      kind: Foo
      plural: foos
      singular: foo
    scope: Namespaced
    versions:
      - name: v1
        # Each version can be enabled/disabled by Served flag.
        served: true
        # One and only one version must be marked as the storage version.
        storage: true
        schema:
          openAPIV3Schema:
            type: object
            properties:
              spec:
                type: object
                properties:
                  name:
                    description: The name of your Foo
                    type: string
```
The name of a CRD object must be a valid DNS subdomain name, as in our case `foos.example.com`. 

CustomResourceDefinitions are available to all namespaces but the resources created from a CRD object can be either namespaced or cluster-scoped, as specified in the CRD's `spec.scope` field.






```terminal:execute
command: |
    kubectl apply -f foo-crd.yaml
```

We should be able to do:
```terminal:execute
command: |
    kubectl get crds foos.example.com
```
```terminal:execute
command: |
    kubectl explain foo.spec
```

After the CustomResourceDefinition object has been created, you can create custom objects.