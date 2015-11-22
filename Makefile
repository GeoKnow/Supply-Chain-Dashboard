build-docker-image:
	docker build -t scd .

clean-virtuoso:
	docker stop gk_virtuoso; cd virtuoso-data; rm -f virtuoso-temp.db virtuoso.db virtuoso.lck virtuoso.log virtuoso.pxa virtuoso.trx; cd -; docker start gk_virtuoso

start-simulator:
	./sbt "project simulator" compile "run 9000"

start-dashboard:
	./sbt "project dashboard" compile "run 9001"

test-scd-container:
	docker run -d --name gk-test-virtuoso docker-registry.eccenca.com/openlink-virtuoso-7:v7.2.1-4
	docker run -it --rm --link gk-test-virtuoso:docker.local -p 9000:9000 scd bash