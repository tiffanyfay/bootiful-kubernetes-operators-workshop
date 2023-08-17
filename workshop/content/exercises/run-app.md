#### Build container image for the controller and push it to registry

```terminal:execute
command: (cd ~/controller/ && ./gradlew bootBuildImage --imageName={{ registry_host }}/foo-controller)
clear: true
```

```terminal:execute
command: docker push {{ registry_host }}/foo-controller
clear: true
```

#### Deploy controller

```editor:append-lines-to-file
file: ~/controller-deployment.yaml
description: Create controller deployment resources
text: |2
  apiVersion: rbac.authorization.k8s.io/v1
  kind: ClusterRole
  metadata:
    name: foo-controller-cr
  rules:
    - apiGroups: [spring.io]
      resources: [foos]
      verbs: [get, list, watch]
    - apiGroups: [""]
      resources: [configmaps]
      verbs: [get, list, create, update, delete]  
    - apiGroups: [apps]
      resources: [deployments]
      verbs: [get, list, create, update, delete]  
  ---
  apiVersion: rbac.authorization.k8s.io/v1
  kind: ClusterRoleBinding
  metadata:
    name: foo-controller-crb
  subjects:
    - kind: ServiceAccount
      name: default
      namespace: default
  roleRef:
    apiGroup: rbac.authorization.k8s.io
    kind: ClusterRole
    name: foo-controller-cr
  ---
  apiVersion: apps/v1
  kind: Deployment
  metadata:
    name: foo-controller
    namespace: default
  spec:
    replicas: 1
    selector:
      matchLabels:
        app: foo-controller
    template:
      metadata:
        labels:
          app: foo-controller
      spec:
        containers:
        - image: {{ registry_host }}/foo-controller
          name: foo-controller
```
```terminal:execute
command: |
    kubectl create -f controller-deployment.yaml
```


#### Discover our first Foo
```terminal:execute
command: kubectl get foos,configmap,deployment | grep my-first-foo
clear: true
```

```terminal:execute
command: kubectl get foos,configmap,deployment | grep my-first-foo
clear: true
```

```execute-2
kubectl port-forward my-first-foo-9b46fc456-nkdtm 8080:8080
```

```terminal:execute
command: kubectl get foos,configmap,deployment | grep my-first-foo
clear: true
```


```terminal:interrupt
session: 2
```