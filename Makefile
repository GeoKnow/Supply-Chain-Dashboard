VIRT_CONT_NAME=gk_virtuoso

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

run-scd-bash:
	docker run -it --rm --link ${VIRT_CONT_NAME}:virtuoso -p 9000:9000 scd bash

run-virtuoso:
	cd virtuoso-data && \
	docker run -d -p 80:80 -p 1111:1111 -v `pwd`:/data --name ${VIRT_CONT_NAME} docker-registry.eccenca.com/openlink-virtuoso-7:v7.2.1-4

rm-virtuoso:
	-docker rm -f ${VIRT_CONT_NAME}
	-rm -f virtuoso-data/*.lck virtuoso-data/*.log

clean-virtuoso:
	-rm virtuoso-data/*.log virtuoso-data/*.trx virtuoso-data/*.pxa virtuoso-data/*.db

virtuoso-load-status:
	docker exec -it ${VIRT_CONT_NAME} /bin/sh -c "/usr/bin/isql exec=\"select * from DB.DBA.load_list;\""

virtuoso-load-data:
	docker exec -it ${VIRT_CONT_NAME} /bin/sh -c "/usr/bin/isql exec=\"delete from DB.DBA.load_list ;\""
	docker exec -it ${VIRT_CONT_NAME} /bin/sh -c "/usr/bin/isql exec=\"ld_dir('/data/import', 'gadm2.fixed.virtuoso.sorted.nt.gz', 'http://linkedgeodata.org/gadm2/');\""
	docker exec -it ${VIRT_CONT_NAME} /bin/sh -c "/usr/bin/isql exec=\"ld_dir('/data/import', 'ncdc-ghcnd-obs.ttl.gz', 'http://www.xybermotive.com/GeoKnowWeather#');\""
	docker exec -it ${VIRT_CONT_NAME} /bin/sh -c "/usr/bin/isql exec=\"ld_dir('/data/import', 'ncdc-stations.ttl.gz', 'http://www.xybermotive.com/GeoKnowWeather#');\""
	docker exec -it ${VIRT_CONT_NAME} /bin/sh -c "/usr/bin/isql exec=\"ld_dir('/data/import', 'news_geoknow_20150910_1016.nt.gz', 'http://www.xybermotive.com/news/');\""
	docker exec -it ${VIRT_CONT_NAME} /bin/sh -c "/usr/bin/isql exec=\"rdf_loader_run();\""
	docker exec -it ${VIRT_CONT_NAME} /bin/sh -c "/usr/bin/isql exec=\"wait_for_children;\""
	docker exec -it ${VIRT_CONT_NAME} /bin/sh -c "/usr/bin/isql exec=\"checkpoint;\""


