D2RQ_PATH=./tools/d2rq
SPARQL=~/Development/apache-jena/bin/sparql

SUPPL_BASE_URI=http://www.xybermotive.com/supplier/

CWD=$(shell pwd)

# .PHONY: build

default: help

## downloads and prepare tools
download-tools:
	cd tools; wget http://download.d2rq.org/d2rq-r2rml-preview-v4.tar.gz; tar -zxf d2rq-r2rml-preview-v4.tar.gz
	cd tools; wget https://github.com/downloads/d2rq/d2rq/d2rq-0.8.1.tar.gz; tar -zxf d2rq-0.8.1.tar.gz

# generate a (d2rml) mapping for the suppliers
#mapping-supplier-d2rq:
#	${D2RQ_PATH}/generate-mapping -d org.postgresql.Driver -u postgres -p postgres jdbc:postgresql://dockerhost:5432/supplier > ${D2RQ_SUPPL_GEN_MAPPING_FILE}

## run the transformation for the suppliers
transform-supplier-schnellecke:
	${D2RQ_PATH}/dump-rdf -f n-triples -b "${SUPPL_BASE_URI}" mappings/supplier_schnellecke.mapping.d2rq.ttl > supplier_schnellecke.nt
	${D2RQ_PATH}/dump-rdf -f n-triples -b "${SUPPL_BASE_URI}" mappings/supplier_schnellecke.mapping.address.d2rq.ttl >> supplier_schnellecke.nt
	${D2RQ_PATH}/dump-rdf -f n-triples -b "${SUPPL_BASE_URI}" mappings/supplier_schnellecke.mapping.location.d2rq.ttl >> supplier_schnellecke.nt

## transform dbpedia-inds to RDF
transform-supplier-dbpedia:
	${D2RQ_PATH}/dump-rdf -f n-triples -b "${SUPPL_BASE_URI}" mappings/supplier_dbpedia.mapping.d2rq.ttl > supplier_dbpedia.nt

## enrich supplier RDF by geocoding the addresses
build-tools:
	cd tools/d2rq && ant jar
	cp tools/csvjdbc* tools/d2rq/lib/db-drivers/
	cd tools/enrichment; \
	rm -rf build/libs/*; \
	gradle clean; \
	gradle build; \
	gradle jar; \
	gradle bootRepackage; \
	cd -

## Enrich the RDF data by geocoding the addresses
enrich-supplier:
	-rm -f supplier_schnellecke.enriched.ttl
	echo "Enriching Supplier RDF data..."
	java -jar tools/enrichment/build/libs/enrichment.jar --enrichment.inputFile=supplier_schnellecke.nt --enrichment.outputFile=supplier_schnellecke.enriched.ttl
	echo "Done."
	${SPARQL} --data supplier_schnellecke.enriched.ttl --query queries/construct-location-geo.rq >> supplier_schnellecke.enriched.ttl
	rapper -i turtle -o ntriples supplier_schnellecke.enriched.ttl > supplier_schnellecke.enriched.nt
	mv supplier_schnellecke.enriched.nt supplier_schnellecke.nt

combine-supplier-files:
	cat supplier_dbpedia.nt supplier_schnellecke.nt > supplier_all.nt


generate-bom:
	python generate-bom.py > products_all.nt

count-supplier:
	${SPARQL} --data=supplier_all.nt --query=queries/count_supplier.rq

count-manufacturer:
	${SPARQL} --data=products_all.nt --query=queries/count_manufacturer.rq

## reformat the supplier.enriched.ttl file
reformat-supplier-as-ttl: combine-supplier-files
	rapper -i ntriples -o turtle supplier_all.nt \
  -f 'xmlns:schema="http://schema.org/"' \
  -f 'xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"' \
  -f 'xmlns:geo="http://www.w3.org/2003/01/geo/wgs84_pos#"' \
  -f 'xmlns:suppl="http://www.xybermotive.com/supplier/"' \
  -f 'xmlns:owl="http://www.w3.org/2002/07/owl#"' \
  -f 'xmlns:ogc="http://www.opengis.net/ont/geosparql#"' \
  -f 'xmlns:dbpedia="http://dbpedia.org/resource/"' \
  -f 'xmlns:xsd="http://www.w3.org/2001/XMLSchema#"' \
  -f 'xmlns:map="http://www.xybermotive.com/supplier/#"' \
  -f 'xmlns:sc="http://www.xybermotive.com/ontology/"' \
  -f 'xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"' \
  > supplier_all.ttl

## Show this help screen
help:
	@printf "Available targets\n\n"
	@awk '/^[a-zA-Z\-\_0-9]+:/ { \
		helpMessage = match(lastLine, /^## (.*)/); \
		if (helpMessage) { \
			helpCommand = substr($$1, 0, index($$1, ":")-1); \
			helpMessage = substr(lastLine, RSTART + 3, RLENGTH); \
			printf "%-15s %s\n", helpCommand, helpMessage; \
		} \
	} \
	{ lastLine = $$0 }' $(MAKEFILE_LIST)
