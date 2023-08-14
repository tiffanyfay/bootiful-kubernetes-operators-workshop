FROM ghcr.io/vmware-tanzu-labs/educates-jdk17-environment:2.5.2

USER root

COPY --chown=1001:0 . /home/eduk8s/

RUN yum install moreutils wget -y

RUN wget -O /etc/pki/ca-trust/source/anchors/isrgrootx1.pem https://letsencrypt.org/certs/isrgrootx1.pem && update-ca-trust

USER 1001

RUN fix-permissions /home/eduk8s