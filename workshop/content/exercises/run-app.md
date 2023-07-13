## Deploy an Instance of the `foo` Object


```terminal:execute
command: kubectl apply -f samples/bin/test.yaml
```

Now you can run the following:

```terminal:execute
command: kubectl get foo -o yaml
```

## Run the Program

```terminal:execute
command: ./samples/gradlew bootRun
```

You can see the created resources:
```terminal:execute
command: |
    kubectl get deploy deployment-demo -o yaml
    kubectl get configmap configmap-demo -o yaml
```

Port-forward the deployment so we can look at in the browser without needing to create a service:
```terminal:execute
kubectl port-forward deployment/deployment-demo 8080:80
```

> TODO: change to svc in code or see if there's a way to have a browser. Or curl -- but two terminals needed.
Go to `localhost:8080` in a browser window.

Run `ctrl-C` for both the port-forward and your run.

If you want to play around with it, you can change the text in the ConfigMap which will create a new Deployment and you can run the port-forward again to see the change.