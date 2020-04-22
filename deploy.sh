#!/bin/bash

# This script requires the following:
# Anypoint Platform Organization ID

# Command should be called as follows:
# ./deploy.sh YOUR_ANYPOINT_ORG_ID

if [ "$#" -ne 1 ]
then
  echo "[ERROR] You need to provide your OrgId"
  exit 1
fi
echo "Deploying JSON Logger to Exchange"
echo "> OrgId: $1"

# Replacing ORG_ID_TOKEN inside pom.xml with OrgId value provided from command line
echo "Replacing OrgId token..."

echo sed -i.bkp "s/ORG_ID_TOKEN/$1/g" json-logger/pom.xml
sed -i.bkp "s/ORG_ID_TOKEN/$1/g" json-logger/pom.xml

# Installing locally
echo "Installing jsonschema2pojo-mule-annotations locally..."

echo mvn -f jsonschema2pojo-mule-annotations/pom.xml clean install
mvn -f jsonschema2pojo-mule-annotations/pom.xml clean install

if [ $? != 0 ]
then
  echo "[ERROR] Failed to install jsonschema2pojo-mule-annotations locally"
  exit 1
fi

echo mvn -f json-logger/pom.xml clean deploy
mvn -f json-logger/pom.xml clean deploy

if [ $? != 0 ]
then
  echo "[ERROR] Failed deploying json-logger to Exchange"
  exit 1
fi
