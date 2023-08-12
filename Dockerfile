FROM ghcr.io/vmware-tanzu-labs/educates-jdk17-environment:2.5.2

USER root

COPY --chown=1001:0 . /home/eduk8s/

RUN mv /home/eduk8s/workshop /opt/workshop

RUN apt-get update && apt-get install -y moreutils

USER 1001

RUN fix-permissions /home/eduk8s