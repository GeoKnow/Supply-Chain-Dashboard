
clean-virtuoso:
	docker stop gk_virtuoso; cd virtuoso-data; rm -f virtuoso-temp.db virtuoso.db virtuoso.lck virtuoso.log virtuoso.pxa virtuoso.trx; cd -; docker start gk_virtuoso

start-simulator:
	sbt "project simulator" compile "run 9000"

start-dashboard:
	sbt "project dashboard" compile "run 9001"