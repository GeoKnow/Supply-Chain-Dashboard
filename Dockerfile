FROM docker-registry.eccenca.com/eccenca-java8:v1.0.0

# provide basic image information
MAINTAINER Rene Pietzsch <rene.pietzsch@eccenca.com>

# prefix name: eccenca, elds, virtuoso, ...
ENV ECC_IMAGE_PREFIX eccenca
# local image name in the prefix
ENV ECC_IMAGE_NAME scd

RUN mkdir -p /data
WORKDIR /data

ADD . /data/

RUN ./sbt "project simulator" compile
RUN ./sbt "project dashboard" compile

EXPOSE 9000 9001

