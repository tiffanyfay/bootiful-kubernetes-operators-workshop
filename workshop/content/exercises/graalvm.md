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