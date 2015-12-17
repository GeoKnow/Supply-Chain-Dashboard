FROM ubuntu:14.04

ENV ECC_IMAGE_PREFIX eccenca
ENV ECC_IMAGE_NAME scd

ENV JAVA_OPTS "-Xmx512m -Xms512m -Xss512m"

RUN apt-get update && apt-get install -y openjdk-7-jre-headless && apt-get clean

RUN mkdir -p /scd
WORKDIR /scd

ADD simulator.tgz /scd
ADD dashboard.tgz /scd
ADD data /scd/data

RUN mv /scd/dashboard* /scd/dashboard && cd /scd/dashboard && ln -s /scd/data && cd -
RUN mv /scd/simulator* /scd/simulator && cd /scd/simulator && ln -s /scd/data && cd -

EXPOSE 9000 9001

