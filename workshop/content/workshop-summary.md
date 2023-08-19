This interactive workshop aimed to teach you how to build extensions for Kubernetes with operators using Spring Boot.

**You can download the sample application you created via the following commands.**

```terminal:execute
description: Archive sample code for download
command: |
  mkdir /tmp/bootiful-kubernetes-operators-workshop
  cp -a controller /tmp/bootiful-kubernetes-operators-workshop/
  cp controller-deployment.yaml /tmp/bootiful-kubernetes-operators-workshop/
  cp my-first-foo.yaml /tmp/bootiful-kubernetes-operators-workshop/
  cp foo-crd.yaml /tmp/bootiful-kubernetes-operators-workshop/
  zip -r bootiful-kubernetes-operators-workshop.zip /tmp/bootiful-kubernetes-operators-workshop/
clear: true
```
```files:download-file
path: bootiful-kubernetes-operators-workshop.zip
```

Here are more resources to help you implement your first operator. 
- We found [this post](https://lairdnelson.wordpress.com/2018/01/07/understanding-kubernetes-tools-cache-package-part-3/) supremely useful for navigating this nightmare world
- [Generating models from CRD YAML definitions for fun and profit](https://github.com/kubernetes-client/java/blob/master/docs/generate-model-from-third-party-resources.md)
