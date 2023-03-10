ARG BASE_IMAGE
ARG JAVA_VERSION=11
FROM $BASE_IMAGE$JAVA_VERSION-build AS installer-env

COPY ./target /target

RUN mkdir -p /home/site/wwwroot && \
    cd ./target/azure-functions/ && \
    cd $(ls -d */|head -n 1) && \
    cp -a . /home/site/wwwroot

FROM $BASE_IMAGE$JAVA_VERSION

ENV AzureWebJobsScriptRoot=/home/site/wwwroot \
    AzureFunctionsJobHost__Logging__Console__IsEnabled=true

ARG SERVICE_VERSION
ENV API_VERSION=${SERVICE_VERSION}

COPY --from=installer-env ["/home/site/wwwroot", "/home/site/wwwroot"]
