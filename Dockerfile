FROM docker-registry.eccenca.com/eccenca-java8:v1.0.0

RUN mkdir -p /data
WORKDIR /data

ADD . /data/

RUN ./sbt "project simulator" compile
RUN ./sbt "project dashboard" compile

EXPOSE 9000 9001

