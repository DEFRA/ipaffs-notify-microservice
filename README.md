# Notify Azure Function

## Introduction

Java Azure Function to notify drivers/hauliers by email/text when a notification has been selected for 
inspection.

## Secret scanning
Secret scanning is setup using [truffleHog](https://github.com/trufflesecurity/truffleHog).
It is used as a pre-push hook and will scan any local commits being pushed

### Pre-push hook setup
1. Install [truffleHog](https://github.com/trufflesecurity/truffleHog)
    - `brew install trufflesecurity/trufflehog/trufflehog`
2. Set DEFRA_WORKSPACE env var (`export DEFRA_WORKSPACE=/path/to/workspace`)
3. Potentially there's an older version of Trufflehog located at: `/usr/local/bin/trufflehog`. If so, remove this.

### Git hook setup

1. Run `mvn install` to configure hooks from service folder.

## Set up

Ensure that you have the necessary configuration to resolve dependencies from Artifactory.

Copy the .env file from Sharepoint into the root of this project.

Install the [Azure Functions Core Tools](https://docs.microsoft.com/en-us/azure/azure-functions/functions-run-local?tabs=macos%2Cjava%2Cportal%2Cbash%2Ckeda#install-the-azure-functions-core-tools):

```shell
brew tap azure/functions
brew install azure-functions-core-tools@3
```

## How To Run

### With the Maven plugin

To run locally using the azure-functions Maven plugin, Azure Storage emulation must be running. 
The simplest way of achieving this is via the Azurite Docker image:

```shell
docker pull mcr.microsoft.com/azure-storage/azurite
docker run -d -p 10000:10000 -p 10001:10001 mcr.microsoft.com/azure-storage/azurite
```

Set the required environment variables, for required variables see the 
[environment variables section](#environment-variables).

From the service directory run:

```shell
mvn package
mvn azure-functions:run
```

**Note**: If updating the `local.settings.json` file (for example changing `AzureWebJobsStorage` to
point to a remote Azure Storage instance) then you must run `mvn package` before running the
function so that the settings will be updated in the target directory.

```shell
. ./setup-local-env.sh
docker build --build-arg BASE_IMAGE=$BASE_IMAGE --tag $DOCKER_IMAGE .
docker run --env-file=.env $DOCKER_IMAGE
```

If a storage key has been obtained, add this to the `setup-local-env.sh`.

`docker run --env AzureWebJobsStorage=$AZURE_WEB_JOBS_STORAGE $DOCKER_IMAGE`

### With IntelliJ

Within the test sources folder is a class `uk.gov.defra.tracesx.notify.LocalFunctionRunner`
which can be used to run the function. Run this as per any other main class and set the relevant
environment variables defined in the [environment variables section](#environment-variables).

## How To Test

### Unit tests

From the service directory, run with:

    mvn test

The coverage report can be created with:

    mvn clean test jacoco:report

The coverage report can then be viewed by opening the `target/site/jacoco/index.html` file in your
browser.

### Integration tests

## Additional Information

### Environment Variables

When the function is initiated the runtime environment is validated to ensure that all required
variables have been set.

| Environment variable | Description |
| -------------------- | ----------- |
| `PROTOCOL` | The HTTP protocol used when connecting to IPAFFS components |
| `ENV_DOMAIN` | The IPAFFS deployment domain (e.g. pool number or RTL environment) |
| `NOTIFY_QUEUE_NAME` | The name of the notify service bus queue |
| `ENABLE_EMAIL_NOTIFICATION` | Flag to enable or disable email notify functionality |
| `ENABLE_TEXT_NOTIFICATION` | Flag to enable or disable text notify functionality |
| `TRADE_PLATFORM_AUTH_URL` | Trade Auth URL for token generation |
| `TRADE_PLATFORM_NOTIFY_URL` | Trade Notify URL for email/Text Notification |
| `TRADE_PLATFORM_CLIENT_ID` | Trade Platform client ID |
| `TRADE_PLATFORM_CLIENT_SECRET` | Trade Platform client Secret |
| `TRADE_PLATFORM_SCOPE` | Trade Platform Scope |
| `TRADE_PLATFORM_SYSTEM_NAME` | Trade Platform System Name |
| `TRADE_PLATFORM_SYSTEM_UNIQUE_ID` | Trade Platform System Unique ID |
| `TRADE_PLATFORM_SUBSCRIPTION_KEY` | Trade Platform Subscription Key |
