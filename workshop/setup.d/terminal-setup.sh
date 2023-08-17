#!/bin/bash
set -x
set +e

kubectl create secret generic regcred --from-file=.dockerconfigjson=$REGISTRY_AUTH_FILE --type=kubernetes.io/dockerconfigjson
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Secret
type: kubernetes.io/service-account-token
metadata:
  name: default-sa-token
  namespace: default
  annotations:
    kubernetes.io/service-account.name: "default"
EOF

kubectl patch serviceaccount default -p '{"secrets": [{"name": "default-sa-token"}], "imagePullSecrets": [{"name": "regcred"}]}'
mv samples/foo-crd.yaml .