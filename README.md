

# Deploy and configure Template on Nifi
[![Maven Central](https://img.shields.io/maven-central/v/com.github.hermannpencole/nifi-deploy-config.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.hermannpencole/nifi-deploy-config)
[![Build Status](https://travis-ci.org/hermannpencole/nifi-config.svg?branch=master)](https://travis-ci.org/hermannpencole/nifi-config/)
[![codecov](https://codecov.io/gh/hermannpencole/nifi-config/branch/master/graph/badge.svg)](https://codecov.io/gh/hermannpencole/nifi-config)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c165156aaa3242bc9a41dc6225e19706)](https://www.codacy.com/app/hermannpencole/nifi-config?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=hermannpencole/nifi-config&amp;utm_campaign=Badge_Grade)

Update, Extract Nifi Configuration

Deploy, undeploy, connect template

## Which version

| version                                  | version NIFI                             |
| ---------------------------------------- | ---------------------------------------- |
| -                                        | 0.X                                      |
| version 1.1.X  [download it from Maven Central](http://central.maven.org/maven2/com/github/hermannpencole/nifi-deploy-config/) | 1.0.X : to test <br/>1.1.X : OK, Build on api nifi  1.1.0 with [nifi-swagger-client](https://github.com/hermannpencole/nifi-swagger-client) 1.1.0.x <br/> 1.2.X : OK<br/> 1.3.X : OK<br/> 1.4.X : OK |
| version 1.5.X  [download it from Maven Central](http://central.maven.org/maven2/com/github/hermannpencole/nifi-deploy-config/) | 1.5.X : OK, Build on api nifi  1.5.0 with [nifi-swagger-client](https://github.com/hermannpencole/nifi-swagger-client) 1.5.0.x|

## How to :

```text
usage: java -jar nifi-deploy-config-1.1.15.jar [OPTIONS]
 -h,--help                 Usage description
 -b,--branch <arg>         Target process group (must begin by root) : root > my group > my sub group (default : root)
 -m,--mode <arg>           mandatory, possible values : updateConfig/extractConfig/deployTemplate/undeploy
 -c,--conf <arg>           mandatory if mode in [updateConfig, extractConfig, deployTemplate]  : configuration file
 -n,--nifi <arg>           mandatory : Nifi URL (ex : http://localhost:8080/nifi-api)
```

*For more options see Chapter [Advanced options](#advanced-options)*

Requirement : *You must have java 8 or higher installed on your machine*

## Step by Step : use in real life

### Prepare your nifi development

1 ) Create a template on nifi :

with this rule : each processor and each controller in a process group **must** have a unique name.

![template](/docs/template.png)

2) Download it

3) Extract a sample configuration with the command

```shell
java -jar nifi-deploy-config-1.1.15.jar \
  -nifi http://ip-nifi-dev:8080/nifi-api \
  -branch "root>My Group>My Subgroup" \
  -conf /tmp/config.json \
  -mode extractConfig
```

### Deploy it on production

1a) undeploy the old version with the command

```shell
java -jar nifi-deploy-config-1.1.15.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>My group>My Subgroup" \
  -m undeploy
```

1b) deploy the template with the command

```shell
java -jar nifi-deploy-config-1.1.15.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>My group>My Subgroup" \
  -conf /tmp/my_template.xml \
  -m deployTemplate
```

2) update the production configuration with the command

```shell
java -jar nifi-deploy-config-1.1.15.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>My group>My Subgroup" \
  -conf /tmp/PROD_config.json \
  -mode updateConfig
```

## What can I configure ?

Everything !

You can find all properties in your extraction file. Now just configure it with the production properties and update your production with the mode updateConfig.

Configuration is based on "name" properties, remember that each processor in a group **must** have a unique name.

It is also possible to create connections between process groups by adding a `connections` JSON element to the configuration. The shortest route between the two process groups will be selected, with output and input ports named according to the connection's name used automatically, and created if they do not already exist. Note that it is only possible to connect to input and output ports, so this functionality is typically best used by including an input or output port already wired in to the template that is named the same as the connection.

sample :
```json
{  
  "processors": [
	{
	  "name": "ExecuteSQL",
	  "config": {
		"properties": {
		  "Database Connection Pooling Service": "85f67694-015d-1000-5071-8cd46e8b2e47",
		  "Max Wait Time": "5 seconds",
		  "dbf-normalize": "false",
		  "dbf-user-logical-types": null,
		  "dbf-default-precision": null,
		  "dbf-default-scale": null
		},
		"schedulingPeriod": "0 sec",
		"schedulingStrategy": "TIMER_DRIVEN",
		"executionNode": "ALL",
		"penaltyDuration": "30 sec",
		"yieldDuration": "1 sec",
		"bulletinLevel": "WARN",
		"runDurationMillis": 0,
		"concurrentlySchedulableTaskCount": 1,
		"comments": "",
		"lossTolerant": false
	  }
	},
	{
	  "name": "PutFile",
	  "config": {
		"properties": {
		  "Directory": "c:\\tmp",
		  "Conflict Resolution Strategy": "fail",
		  "Create Missing Directories": "true"
		},
		"schedulingPeriod": "0 sec",
		"schedulingStrategy": "TIMER_DRIVEN",
		"executionNode": "ALL",
		"penaltyDuration": "30 sec",
		"yieldDuration": "1 sec",
		"bulletinLevel": "WARN",
		"runDurationMillis": 0,
		"concurrentlySchedulableTaskCount": 1,
		"comments": "",
		"lossTolerant": false
	  }
	}
  ],
  "controllerServices": [
    {
      "name": "DBCPConnectionPool",
      "properties": {
        "Database Connection URL": "localhost:8080",
		"Database Driver Class Name": "org.test",
		"Password": "********",
        "Max Total Connections": "3"
      }
    }
  ],
  "connections": [
    {
      "name": "front_end_1",
      "source": "root > front_end > front_end_1",
      "destination": "root > back_end > back_end_1"
    }
  ],
  "name": "testController"
}
```
### Sample usage

#### Sample extract configuration

```shell
java -jar nifi-deploy-config-1.1.15.jar \
  -nifi http://ip-nifi-dev:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/test2.json \
  -mode extractConfig
```

#### Sample update configuration

```shell
java -jar nifi-deploy-config-1.1.15.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/test2.json \
  -mode updateConfig
```

#### Sample deploy Template

```shell
java -jar nifi-deploy-config-1.1.15.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/my_template.xml \
  -m deployTemplate
```

#### Sample undeploy

```shell
java -jar nifi-deploy-config-1.1.15.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -m undeploy
```

force mode actived

```shell
java -jar nifi-deploy-config-1.1.15.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -m undeploy
  -f
  -timeout 600
  -interval 10
```

#### Sample access via username/password

```shell
java -jar nifi-deploy-config-1.1.15.jar \
  -user my_username \
  -password my_password \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/test2.json \
  -m updateConfig
```

#### Sample access via Kerberos ticket exchange / SPNEGO negotiation

```shell
java -Djava.security.krb5.conf=/etc/krb5.conf \ 
     -Djavax.security.auth.useSubjectCredsOy=false \
     -jar nifi-deploy-config-1.1.15.jar \
  -accessFromTicket \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/test2.json \
  -m updateConfig
```

### Advanced Options

#### Pooling

```text
 -timeout <arg>            allows specifying the polling timeout in second (defaut 120 seconds); negative values indicate no timeout
 -interval <arg>           allows specifying the polling interval in second (default 2 seconds)
```

####  Security

 ```text
 -password <arg>           password for access via username/password, then user is mandatory
 -user <arg>               user name for access via username/password, then password is mandatory
 -accessFromTicket         Access via Kerberos ticket exchange / SPNEGO negotiation 
 -noVerifySsl              turn off ssl verification certificat 
 ```

For accessFromTicket option, if you want use access via Kerberos ticket exchange / SPNEGO negotiation ; You must configure system properties java.security.krb5.conf (see https://docs.oracle.com/javase/8/docs/technotes/guides/security/jgss/tutorials/KerberosReq.html) and javax.security.auth.useSubjectCredsOy to false.  [Sample access via Kerberos ticket exchange / SPNEGO negotiation](#sample-access-via-kerberos-ticket-exchange--spnego-negotiation)

####  Timeout Api Client

 ```text
 -connectionTimeout <arg>  configure api client connection timeout (default 10 seconds)
 -readTimeout <arg>        configure api client read timeout (default 10 seconds)
 -writeTimeout <arg>       configure api client write timeout (default 10 seconds)
 ```

####  Position

```text
 -placeWidth <arg>         width of place for installing group (default 1935 : 430 * (4 + 1/2) = 4 pro line)
 -startPosition <arg>      starting position for the place for installing group, format x,y (default : 0,0)
```

####  Other

 ```text
 -f,--force                turn on force mode : empty queue after timeout
 -noStartProcessors        turn off auto start of the processors after update of the config
 -enableDebugMode          turn on debugging mode of the underlying API library
 -keepTemplate             keep template after installation (default false)
 ```
## Note

#### About controller

By default, nifi-config uses the controller declared on the parent group that has the same name, if any then deletes the controller declaration on the child group, otherwise uses the controller of the group.

If you want to use a controller declared on parent group without updating it, just declare the controller with no property on json file : 

    "controllerServices": [
     {
      "name": "DBCPConnectionPool"
      }
# TODO

add version management that undeploys the old version automatically (with a version # in comment?)

All ideas are welcome.

# Troubleshooting

### Proxy

If you are behind a proxy,  try adding these system properties on the command line:

```
-Dhttp.proxyHost=myproxy.mycompany.com -Dhttp.proxyPort=3128
```

See more at [http://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html](http://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html)
