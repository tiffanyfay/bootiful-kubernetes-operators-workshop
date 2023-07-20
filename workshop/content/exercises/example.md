You will create a custom resource called Foo. Foo will create a Deployment using an NGINX image and a ConfigMap. This ConfigMap will be used to change the text that shows on the webpage. Let’s walk through what actually happens.

![I want a Foo (that creates N managed nginx Pods)](../images/example.png)

First off, when you communicate with Kubernetes everything talks through the API server.

The API server then saves the request to the control plane database which is typically etcd. And this is why Josh is saying Kubernetes is just a database without any action behind it – you need something to ask the API server about actually doing something with the resources and are if there any requests related to them.

For instance, here want to have a custom resource of kind Foo. This then gets added to the database. If there’s no controller, this is where things end. The way we will be creating Foo is that Foo will create a Deployment which uses the Deployment controller. And this gets added to the database (which typically is etcd). The deployment controller then will inquire with the API server and know there was a request for a new Deployment and then that continues down through the ReplicaSet controller. In our specific case, we will be using the NGINX image. These controllers compare the actual state to the desired state.

Then the scheduler, which is another type of controller, assigns nodes for these pods. Just a bit of controllers galore.