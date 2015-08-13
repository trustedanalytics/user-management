User management
==========

The primary responsibility of this service is the management of users in the platform. There are two ways in which you can become a user:

* Global SysOp invitation. Admin user can send a email with invitation to create an organization and Org SysOp account;
* Org SysOp invitation. Org SysOp can create new accounts for org users and send email with instructions how to log in.

The service allows for SysOps not only invite new users, but also remove them and give them a role inside organization or a specific space.
All of these operations are performed against UAA and Cloud Controller.

Required services
-----------------
User-management requires following service to function properly:

* **SMTP** - to send invitation emails;
* **Redis DB** - for storing security codes.
* **SSO** - a collection of URLs for services like UAA, Cloud Controller, Login server, etc ...

Required libraries
-----------------
Following libraries are necessary to successfully build user-management:

* **cf-client** - separate library to communicate with cloud foundry layer.

Security
--------
The RESTful endpoints provided by this service are protected by OAuth2. User management is a Resource Server and requires valid Acces Token for communication.
There are actually two tokens used:

* **user AT** - obtained from HTTP header from request a user comes with. It's used for majority of the communications with Cloud Controller.
* **client AT** - required to create new accounts in UAA. This token is obtained from UAA by means of client grant type.

How to build
------------
It's a Spring Boot application build by maven. All that's needed is a single command to compile, run tests and build a jar:

```
$ mvn verify
```

How to run locally
------------------
To run the service locally or in Cloud Foundry, the following environment variables need to be defined:

* `VCAP_SERVICES_SSO_CREDENTIALS_APIENDPOINT` - a Cloud Foundry API endpoint;
* `VCAP_SERVICES_SSO_CREDENTIALS_UAAURI` - an UAA service address;
* `VCAP_SERVICES_SSO_CREDENTIALS_AUTHORIZATIONURI` - an OAuth authorization endpoint;
* `VCAP_SERVICES_SSO_CREDENTIALS_TOKENKEY` - an UAA endpoint for verifying token signatures;
* `VCAP_SERVICES_SSO_CREDENTIALS_CLIENTID` - a client ID used for OAuth authorization;
* `VCAP_SERVICES_SSO_CREDENTIALS_CLIENTSECRET` - a client secret used for OAuth authorization;
* `VCAP_SERVICES_SMTP_CREDENTIALS_HOSTNAME` - a SMTP host name;
* `VCAP_SERVICES_SMTP_CREDENTIALS_USERNAME` - a user name for authorization to SMTP server;
* `VCAP_SERVICES_SMTP_CREDENTIALS_PASSWORD` - a password for authorization to SMTP server;
* `VCAP_SERVICES_SMTP_CREDENTIALS_PORT` - a SMTP server port;
* `VCAP_SERVICES_SMTP_CREDENTIALS_PORT_SSL` - a SMTP server SSL port;

There are meaningful configuration values provided that allow for local testing. The server can be run by maven spring boot plugin:

```
$ mvn spring-boot:run -Dspring.cloud.propertiesFile=spring-cloud.properties
```

After server has been started an ordinary curl command can be used to test the functionality, i.e.:

```
$ curl http://localhost:9998/rest/orgs/69e8563a-f182-4c1a-9b9d-9a475297cb41/users -v -H "Authorization: `cf oauth-token|grep bearer`"
```
