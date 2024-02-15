FROM jupyter/minimal-notebook:notebook-6.4.12
# Inspect the Dockerfile at:
# https://github.com/jupyter/docker-stacks/tree/HEAD/minimal-notebook/Dockerfile

USER root
RUN chown -R jovyan /home/jovyan

RUN apt update && apt -y install pkg-config libcairo2-dev gcc libgirepository1.0-dev git clang \
    llvm graphviz openjdk-21-jdk vim

# Install iJava kernel for juypter
RUN mkdir /tmp/ijava
RUN wget https://github.com/SpencerPark/IJava/releases/download/v1.3.0/ijava-1.3.0.zip -P /tmp/ijava
RUN unzip /tmp/ijava/ijava-1.3.0.zip -d /tmp/ijava
RUN python /tmp/ijava/install.py --classpath "/home/jovyan/mll/lib/*.jar:/home/jovyan/mll/bin/"
RUN rm -rf /tmp/ijava

USER jovyan
EXPOSE 8888
