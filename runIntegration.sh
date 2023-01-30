#!/bin/sh

$( awk '{print "export", $1}' ./.env )

mvn clean verify -f integration/pom.xml \
  -Dskip.integration.tests=false \
  -Dtest.openid.service.url=${TEST_OPENID_TOKEN_SERVICE_URL} \
  -Dtest.openid.service.auth.username=${TEST_OPENID_TOKEN_SERVICE_AUTH_USERNAME} \
  -Dtest.openid.service.auth.password=${TEST_OPENID_TOKEN_SERVICE_AUTH_PASSWORD}
