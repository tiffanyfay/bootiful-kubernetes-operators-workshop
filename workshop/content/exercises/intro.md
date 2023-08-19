In robotics and automation, a control loop is a non-terminating loop (i.e., a loop that runs forever) that regulates the state of a system and tries to match the **current state** of the system with a **desired state**.

For example, when we set the temperature of a thermostat, that's the desired state. The actual room temperature is the current state. The thermostat acts to bring the current state closer to the desired state, by turning equipment (heaters, AC, heat pumps...) on or off.

In Kubernetes, controllers are control loops that watch the state of your cluster, then make or request changes where needed. For example, when scaling a deployment, we set the desired state (the number of replicas), and a controller will create or delete pods accordingly so that the current state (the number of running pods) matches the desired state.

#### The Controller Pattern

A controller tracks at least one Kubernetes resource type.

By convention, most resources have a "spec" field that represents the desired state. The controller(s) for that resource are responsible for making the current state come closer to that desired state.

In the control plane of a freshly installed Kubernetes cluster, there is a component called the **controller manager**, which runs a few dozens of controllers managing the default Kubernetes resources (there are more than 50 of them as of Kubernetes 1.27). But it is also possible to implement and run our own custom controllers to extend these resources, or implement entirely new resources.

#### Emergent scenarios

Kubernetes has emerged as the de facto standard for container orchestration, revolutionizing the way applications are deployed and managed in modern cloud-native environments. While Kubernetes offers powerful abstractions for managing containers and resources, there are **scenarios where the built-in functionality may not be sufficient**. This is where the **Operator Pattern** comes into play, providing a powerful and extensible way **to enhance Kubernetes capabilities** for managing complex applications.

#### The Operator Pattern

The Operator Pattern is a concept for extending Kubernetes with a higher level of abstraction. It combines **Custom Resource Definitions** and **custom controllers**.

- **Custom Resource Definitions (CRDs)**: Kubernetes lets us define new resource types on the fly to extend its basic types. For instance, a database operator could define a PostgresqlCluster resource.
Just like creating a Deployment would eventually create one or multiple Pods for that Deployment, creating a PostgresqlCluster would create Pods and Services and stateful resources to run a persistent, replicated database cluster.

- **Custom Controllers**: the CRDs themselves are merely data structures. We also need code to back these data structures. Operators consist of custom controllers that continuously monitor the state of their custom resources. When a custom resource is created, updated, or deleted, the corresponding controller takes the appropriate actions to reconcile the actual state with the desired state.

##### How to write your own Operator?

If there isn't an operator available that implements the behavior you want, you can write one yourself.

Operators (specifically, the controller part of the operator) can be written using any language / runtime that can act as a client for the Kubernetes API.

[Several Operator frameworks](https://kubernetes.io/docs/concepts/extend-kubernetes/operator/#writing-operator), such as the Operator SDK and Kubebuilder, have been developed to streamline the creation of Operators. These frameworks provide templates, libraries, and tools to simplify the development process and promote best practices.

**In this workshop, we will build a custom controller with Spring Boot, and use the official Java Kubernetes client library.**

