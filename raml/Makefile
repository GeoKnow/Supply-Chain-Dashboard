
## build the markdown document from the raml file
build: check
	node_modules/raml2md/bin/raml2md -t template/main.nunjucks simulator.raml > simulator.md
	node_modules/raml2html/bin/raml2html simulator.raml > simulator.html


## install needed node utils via npm
install: package.json
	npm install

## check syntax of the raml files
check: install
	node_modules/raml-cop/src/raml-cop.js simulator.raml
	# node_modules/raml-cop/src/raml-cop.js dashboard.raml

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