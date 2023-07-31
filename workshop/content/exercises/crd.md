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
                  nickname:
                    description: The nickname of your Foo
                    type: string
```
The name of a CRD object must be a valid DNS subdomain name, as in our case `foos.example.com`. 
`spec.group` is the API group of the defined custom resource. The custom resources are served under `/apis/<group>/...` and must match the name of the CRD.
`spec.names` specify the resource and kind names for the custom resource.

CustomResourceDefinitions are available to all namespaces but the resources created from a CRD object can be either namespaced or cluster-scoped, as specified in the CRD's `spec.scope` field.

`spec.versions` is the list of all API versions of the defined custom resource. For each version, the `name` (e.g. "v1" or "v2beta1"), `served` (enable/disable a version), and `storage` (the one version used to persist custom resources to storage, other available versions will be converted to it) fields are required.

The `schema` describes an OpenAPI v3 schema for custom fields used for validation during creation and updates. In our basic example a custom field `spec.name` of a `Foo` resource.


After the creation of our CRD a new namespaced REST API endpoint is created at `/apis/foos.example.com/v1/namespaces/*/foos/...`.
```terminal:execute
command: |
    kubectl create -f foo-crd.yaml
```

With the following commands, you can fetch information about our CRD via the kubectl CLI.
```terminal:execute
command: |
    kubectl get crds foos.example.com
```
```terminal:execute
command: |
    kubectl explain foo.spec
```

After the CustomResourceDefinition object has been created, you can create custom objects.
```editor:append-lines-to-file
file: ~/my-first-foo.yaml
description: First custom resource
text: |2
  apiVersion: example.com/v1
  kind: Foo
  metadata:
    name: my-first-foo
  spec:
    nickname: firsty
```
```terminal:execute
command: |
    kubectl create -f my-first-foo.yaml
```