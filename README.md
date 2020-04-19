![Build Container](https://github.com/rht-labs/open-management-portal-backend/workflows/Build%20Container/badge.svg)

# Open Management Portal - Backend

> The API for the Open Management Portal.

## Configuration

The following environment variables are available:

| Name | Example Value | Required |
|------|---------------|----------|
| JWT_PUBKICKEY_LOCATION | http://[your-cluster-internal-sso-service-name]:8080/auth/realms/[your-realm-id]/protocol/openid-connect/certs | True |
| JWT_ISSUER | http://[your-cluster-internal-sso-service-name] | True |
| JWT_ENABLE | True | False |
| MONGODB_USER | monguser | True |
| MONGODB_PASSWORD | mongopassword | True |
| DATABASE_SERVICE_NAME | omp-backend-mongodb | True |
| MONGODB_DATABASE | engagements | True |
| CONFIG_REPOSITORY_ID |  1234             |  True        |
| CONFIG_REPOSITORY_PATH | schema/config.yml | True |
| OMP_GITLAB_API_URL   | http://omp-git-api:8080 | True |

## Development

See [the development README](development/README.md) for details on how to spin up a deployment for developing on OpenShift.

## Deployment

This project includes an `openshift-applier` inventory. To use it, make sure that you are logged in to the cluster and that you customize the variables in `.applier/inventory/group_vars/all.yml`. Once these are configured, you can deploy the project with:

```bash
cd .applier/
ansible-galaxy install -r requirements.yml --roles-path=roles --force
ansible-playbook apply.yml -i inventory/
```

:heavy_exclamation_mark: The applier will not create a route or ingress and if this is required has to be done seperately

## Components

This project was built using Quarkus.

## Testing

This project runs unit tests using an alternate application profile, which disables JWT token verification and user role verification. This means that all endpoints are available without returning 401 or 403, but the user context is not available during tests.

## Useful Commands

``` bash
# serve with hot reload at localhost:8080
mvn quarkus:dev
# run unit tests
mvn test
# build for production
mvn quarkus:build
```

### OpenShift Applier

This project includes an `openshift-applier` inventory. To use it, make sure that you are logged in to the cluster and that you customize the variables in `.applier/inventory/group_vars/all.yml` - namely make sure that `deploy_vars` uses the correct endpoints. Once these are configured, you can deploy the project with:

```bash
cd .applier/
ansible-galaxy install -r requirements.yml --roles-path=roles --force
ansible-playbook apply.yml -i inventory/
```


## Pipeline

The deployment pipeline is running through a Jenkinsfile located in the root folder of the project. This Jenksinfile is written in groovy.
The pipeline expects the nexus is available at nexus:8080. Make sure that nexus is available and accessible to Jenkins.

#### Prepare environment for [ENVIRONMENT] deploy

The first stage is going to set environment vars based on the branch selected to build:

```groovy
master - env.PROJECT_NAMESPACE = "${NAMESPACE_PREFIX}-test"
         env.NODE_ENV = "test"
         env.QUARKUS_PROFILE = "openshift-test"
         env.RELEASE = true

develop.* or feature.* - env.PROJECT_NAMESPACE = "${NAMESPACE_PREFIX}-dev"
                         env.NODE_ENV = "dev"
                         env.QUARKUS_PROFILE = "openshift-dev"
```

#### Ansible

Jenkins will spin up an Ansible agent that will run a playbook called OpenShift Applier (https://github.com/redhat-cop/openshift-applier). The openshift-applier is used to apply OpenShift objects to an OpenShift Cluster. 

This stage is going to download the playbook dependencies using Ansible Galaxy and apply the playbook using **build** as a *filter_tag*. This is going to create the necessary resources for our application build in an OpenShift cluster. 

#### Test/Maven Build/Nexus/OpenShift Build

Jenkins will spin up a Maven agent to test, Maven build, upload to Nexus and start the OpenShift build.

##### Test

```
mvn clean test
```

##### Maven Build

```
mvn clean install
```

##### Static Code Analysis

```
mvn checkstyle:checkstyle
mvn org.jacoco:jacoco-maven-plugin:prepare-agent install -Dmaven.test.failure.ignore=true
```

##### Nexus

```
mvn deploy
```

##### OpenShift Build

The OpenShift build is going to start after the Nexus deployment is complet and successful.

###### OpenShift Atomic Registry

If you're pushing from the master branch the build will create a container image and push it to the Openshift internal registry.

```
oc project ${PIPELINES_NAMESPACE}
oc patch bc ${APP_NAME} -p "{\\"spec\\":{\\"output\\":{\\"to\\":{\\"kind\\":\\"ImageStreamTag\\",\\"name\\":\\"${APP_NAME}:${JENKINS_TAG}\\"}}}}"
oc start-build ${APP_NAME} --from-file=target/${ARTIFACTID}-${VERSION}-runner.jar --follow
```

###### Quay

If you're pushing from a release tag the build will create a container image and push it to Quay.

```
oc project ${PIPELINES_NAMESPACE} # probs not needed
oc patch bc ${APP_NAME} -p "{\\"spec\\":{\\"output\\":{\\"to\\":{\\"kind\\":\\"DockerImage\\",\\"name\\":\\"quay.io/rht-labs/${APP_NAME}:${JENKINS_TAG}\\"}}}}"
oc start-build ${APP_NAME} --from-file=target/${ARTIFACTID}-${VERSION}-runner.jar --follow
```
