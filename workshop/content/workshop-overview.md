Kubernetes provides a framework to run distributed systems and applications resiliently.

It provides lots of built-in automation mechanisms to deploy, scale, and update workloads; load balance traffic; and much more.

Kubernetes also provides many extension points to implement new features and behaviors when you need more than what's available out of the box.

One of these extension points is the powerful **operator pattern**, which lets you **extend the cluster's behavior** without modifying the code of Kubernetes itself.

In a nutshell, operators are **clients of the Kubernetes API that act as controllers for a Custom Resource.** We're going to see what that means, and what implementing such an operator entails.

**In this interactive workshop, we will learn how to build extensions for Kubernetes with operators, using our beloved application framework Spring Boot.**
