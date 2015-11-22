export ECC_IMAGE_PREFIX=$(shell cat ./Dockerfile | egrep "^ENV ECC_IMAGE_PREFIX " | cut -d ' ' -f 3)
export ECC_IMAGE_NAME=$(shell cat ./Dockerfile | egrep "^ENV ECC_IMAGE_NAME " | cut -d ' ' -f 3)
export CONTAINER_NAME=${ECC_IMAGE_PREFIX}-${ECC_IMAGE_NAME}
export KILLTIMEOUT=10

# git investigation
export GITBRANCH=$(shell git rev-parse --abbrev-ref HEAD)
export GITDESCRIBE=$(shell git describe --always --dirty)

# our private registry
export REGISTRY_HOST=docker-registry.eccenca.com

# create the main tag (latest / develop / unknown)
export TAG_SUFFIX=unknown
ifeq ($(GITBRANCH), master)
	export TAG_SUFFIX=latest
endif
ifeq ($(GITBRANCH), develop)
	export TAG_SUFFIX=develop
endif
export IMAGE_NAME=${CONTAINER_NAME}:${TAG_SUFFIX}
export TAG=${REGISTRY_HOST}/${IMAGE_NAME}

# add an additional tag based on git versioning
export TAG2=${REGISTRY_HOST}/${CONTAINER_NAME}:${GITDESCRIBE}

export DOCKER_CMD=docker
ifneq ($(ECC_HOST),)
	export DOCKER_CERT_PATH=$(HOME)/.config/ssl/docker-eccenca
	export DOCKER_HOST=https://${ECC_HOST}.eccenca.com:2375
	export DOCKER_TLS_VERIFY=true
	export DOCKER_CMD=docker --tlsverify --tlscacert=${DOCKER_CERT_PATH}/ca.pem --tlscert=${DOCKER_CERT_PATH}/client-cert.pem --tlskey=${DOCKER_CERT_PATH}/client-key.pem -H=$(ECC_HOST).eccenca.com:2375
endif

## build the image based on docker file and latest repository
build:
	$(DOCKER_CMD) build -t ${IMAGE_NAME} .

## build the image based on docker file and latest repository and ignore cache
clean-build:
	$(DOCKER_CMD) build --pull=true --no-cache -t ${IMAGE_NAME} .

clean-virtuoso:
	docker stop gk_virtuoso; cd virtuoso-data; rm -f virtuoso-temp.db virtuoso.db virtuoso.lck virtuoso.log virtuoso.pxa virtuoso.trx; cd -; docker start gk_virtuoso

## save a local image to a tar file
save:
	$(DOCKER_CMD) save -o ${CONTAINER_NAME}-${GITDESCRIBE}.tar ${IMAGE_NAME}

## tag the local image with a registry tag
tag:
	$(DOCKER_CMD) tag -f ${IMAGE_NAME} ${TAG}
	$(DOCKER_CMD) tag -f ${IMAGE_NAME} ${TAG2}

## push the local image to the registry
push: tag
	$(DOCKER_CMD) push ${TAG}
	$(DOCKER_CMD) push ${TAG2}

## pull the image from the registry and tag it in order to use other targets
pull:
	$(DOCKER_CMD) pull ${TAG2}
	$(DOCKER_CMD) tag -f ${TAG2} ${IMAGE_NAME}
	$(DOCKER_CMD) tag -f ${TAG2} ${TAG}

####################
####################
####################

start-simulator:
	./sbt "project simulator" compile "run 9000"

start-dashboard:
	./sbt "project dashboard" compile "run 9001"

test-scd-container:
	docker run -d --name gk-test-virtuoso docker-registry.eccenca.com/openlink-virtuoso-7:v7.2.1-4
	docker run -it --rm --link gk-test-virtuoso:virtuoso -p 9000:9000 scd bash