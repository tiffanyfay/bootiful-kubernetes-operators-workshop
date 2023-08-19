#!/bin/bash
set -x
set +e

kubectl create secret generic regcred --from-file=.dockerconfigjson=$REGISTRY_AUTH_FILE --type=kubernetes.io/dockerconfigjson

kubectl patch serviceaccount default -p '{"secrets": [{"name": "default-sa-token"}], "imagePullSecrets": [{"name": "regcred"}]}'
mv samples/foo-crd.yaml .

kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# JVM image
docker pull docker.io/paketobuildpacks/builder:base
docker pull docker.io/paketobuildpacks/run:base-cnb

## Native image
docker pull docker.io/paketobuildpacks/builder:tiny
docker pull docker.io/paketobuildpacks/run:tiny-cnb