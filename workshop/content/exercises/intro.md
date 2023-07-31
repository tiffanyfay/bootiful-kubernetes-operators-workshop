In robotics and automation, a control loop is a non-terminating loop that regulates the state of a system. When you for example set the temperature of a thermostat, that's the desired state. The actual room temperature is the current state. The thermostat acts to bring the current state closer to the desired state, by turning equipment on or off.

In Kubernetes, controllers are control loops that watch the state of your cluster, then make or request changes where needed. Each controller tries to move the current cluster state closer to the desired state.

#### The Controller pattern

A controller tracks at least one Kubernetes resource type. These objects have a spec field that represents the desired state. The controller(s) for that resource are responsible for making the current state come closer to that desired state.

#### The Operator Pattern

Kubernetes has emerged as the de facto standard for container orchestration, revolutionizing the way applications are deployed and managed in modern cloud-native environments. While Kubernetes offers powerful abstractions for managing containers and resources, there are **scenarios where the built-in functionality may not be sufficient**. This is where the **Operator Pattern** comes into play, providing a powerful and extensible way **to enhance Kubernetes capabilities** for managing complex applications.

The Operator Pattern is a concept for extending Kubernetes to manage custom resources and applications with a higher level of abstraction by utilizing **Custom Resource Definitions** and **custom controllers**.
- **Custom Resource Definitions (CRDs)**: Operators use Custom Resource Definitions to define new types of resources and their desired states. This enables Kubernetes to understand and manage these custom resources alongside its native objects like pods, services, and deployments.
- **Custom Controllers**: Operators consist of custom controllers that continuously monitor the state of the custom resources. When a custom resource is created, updated, or deleted, the corresponding controller takes the appropriate actions to reconcile the actual state with the desired state.

##### How to write your own Operator?

If there isn't an operator available that implements the behavior you want, you can write one yourself.

Operators (that is, a Controller) can be written using any language / runtime that can act as a client for the Kubernetes API.
The Kubernetes Controller is built with Spring Boot, but I could have implemented it easily with other programming languages/frameworks, where a Kubernetes client library is available for. You

**We will use the official Java Kubernetes client library for the controller we'll build with Spring Boot.**

[Several Operator frameworks](https://kubernetes.io/docs/concepts/extend-kubernetes/operator/#writing-operator), such as the Operator SDK and Kubebuilder, have been developed to streamline the creation of Operators. These frameworks provide templates, libraries, and tools to simplify the development process and promote best practices.



