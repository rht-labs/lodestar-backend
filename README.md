# Open Management Portal - Backend

> The API for the Open Management Portal.

## Configuration

The following environment variables are available:

| Name | Example Value | Required |
|------|---------------|----------|
| GIT_REPO_URL | git@gitlab.com:my-org/omp-repo.git | True |
| GIT_SECRET_NAME | git-key | True |
| MP_JWT_VERIFY_PUBLICKEY_LOCATION | http://[your-cluster-internal-sso-service-name]:8080/auth/realms/[your-realm-id]/protocol/openid-connect/certs | True |

## Deployment

This project includes an `openshift-applier` inventory. To use it, make sure that you are logged in to the cluster and that you customize the variables in `.applier/inventory/group_vars/all.yml`. 

#### Image Pull Secret

The `BuildConfig` in this project requires an image pull secret with permission to pull from `registry.redhat.io`. By default, the `BuildConfig` expects it to be called `builder-image-pull-secret`. You can create it on the command-line using `oc` like this:

```bash
oc create secret docker-registry builder-image-pull-secret --docker-server=registry.redhat.io --docker-username=[your-username] --docker-password=[your-password] --docker-email=[anything]
```

Alternatively, you can uncomment the image pull secret in `inventory/group_vars/all.yml` and define the registry credentials in this inventory.

#### SSH Private Key

The `DeploymentConfig` in this project requires a secret containing an SSH private key that has read/write access to your git repository. This should have a `type` of `kubernetes.io/ssh-auth`. This SSH key must not have a passphrase, and _must_ be RSA-type in PEM format. You can generate a key of this type using this command:

```bash
ssh-keygen -b 4096 -t rsa -m pem -f git-key
```

The resulting private key should begin with `-----BEGIN RSA PRIVATE KEY-----`, **not** `-----BEGIN OPENSSH PRIVATE KEY-----`. If you receive a runtime error of `invalid privatekey`, double check this.

You can then create the secret in OpenShift using the web UI (easy), or by base64 encoding the private key and putting it into the SSH secret included in this inventory and then uncommenting the ssh secret in `inventory/group_vars/all.yml`.

#### Running OpenShift-Applier

Once properly configured, you can deploy the project with:

```bash
$ cd .applier/

$ ansible-galaxy install -r requirements.yml --roles-path=roles --force

$ ansible-playbook apply.yml -i inventory/
```

If you put a secret into this inventory, _do not push the inventory to a public git repository_.

## Components

This project was built using Quarkus.

## Testing

This project runs unit tests using an alternate application profile, which disables JWT token verification and user role verification. This means that all endpoints are available without returning 401 or 403, but the user context is not available during tests.

## Useful Commands

``` bash

# serve with hot reload at localhost:8080
$ mvn quarkus:dev

# run unit tests
$ mvn test

# build for production
$ mvn quarkus:build

```
