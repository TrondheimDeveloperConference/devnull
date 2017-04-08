#!/bin/bash

cd ..
sbt appmgr:packageBin
cp -R target/appmgr docker/devnull
docker build -t trondheimdc/devnull docker
rm -rf docker/devnull
docker push trondheimdc/devnull