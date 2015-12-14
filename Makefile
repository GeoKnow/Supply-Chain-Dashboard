## build the image based on docker file and latest repository
build: build-simulator-tgz build-dashboards-tgz
	cd docker && make build tag

dist-clean:
	-rm -f docker/*.tgz
	-rm -rf docker/data
	./sbt clean

####################

build-simulator-tgz:
	./sbt "project simulator" universal:packageZipTarball
	mv simulator/target/universal/simulator*.tgz docker/simulator.tgz
	mkdir -p docker/data
	cp -r data/conf docker/data/

build-dashboards-tgz:
	./sbt "project dashboard" universal:packageZipTarball
	mv dashboard/target/universal/dashboard*.tgz docker/dashboard.tgz
	mkdir -p docker/data
	cp -r data/conf docker/data/

start-simulator:
	./sbt "project simulator" compile "run 9000"

start-dashboard:
	./sbt "project dashboard" compile "run 9001"

test-scd-container:
	docker run -d --name gk-test-virtuoso docker-registry.eccenca.com/openlink-virtuoso-7:v7.2.1-4
	docker run -it --rm --link gk-test-virtuoso:virtuoso -p 9000:9000 scd bash

run-virtuoso:
	cd virtuoso-data && \
	docker run -d -p 80:80 -p 1111:1111 -v `pwd`:/data --name gk_virtuoso docker-registry.eccenca.com/openlink-virtuoso-7:v7.2.1-4

rm-virtuoso:
	-docker rm -f gk_virtuoso
	-rm -f virtuoso-data/*.lck virtuoso-data/*.log virtuoso-data/*.trx virtuoso-data/*.pxa virtuoso-data/*.db