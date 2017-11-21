

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

## How to :

```shell
usage: java -jar nifi-deploy-config-1.1.5.jar [OPTIONS]
 -h,--help                 Usage description
 -b,--branch <arg>         process group to begin (must begin by root) : root > my group > my sub group (default root)
 -c,--conf <arg>           adresse configuration file mandatory with mode (updateConfig/extractConfig/deployTemplate)
 -m,--mode <arg>           mandatory :updateConfig/extractConfig/deployTemplate/undeploy
 -n,--nifi <arg>           mandatory : Nifi http (ex : http://localhost:8080/nifi-api)
```

*For more options see Chapiter [Advanced options](#advanced-options)*

Requirement : *You must have java 8 or higher installed on your machine*

## Step by Step : use in real live

### Prepare your nifi development

1 ) Create a template on nifi :

with this rule : each processor and each controller in a process group **must** have a unique name.

![template](/docs/template.png)

2) and download it

3) Extract a sample configuration with the command

```shell
java -jar nifi-deploy-config-1.1.5.jar \
  -nifi http://ip-nifi-dev:8080/nifi-api \
  -branch "root>My Group>My Subgroup" \
  -conf /tmp/config.json \
  -mode extractConfig
```

### Deploy it on production

1a) undeploy the old version with the command

```shell
java -jar nifi-deploy-config-1.1.5.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>My group>My Subgroup" \
  -m undeploy
```

1b) deploy the template with the command

```shell
java -jar nifi-deploy-config-1.1.5.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>My group>My Subgroup" \
  -conf /tmp/my_template.xml \
  -m deployTemplate
```

2) update the production configuration with the command

```shell
java -jar nifi-deploy-config-1.1.3.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>My group>My Subgroup" \
  -conf /tmp/PROD_config.json \
  -mode updateConfig
```

## What can I configure

All !

You can find all properties in your extraction file. Now just configure it with the production properties and update your production with the mode updateConfig.

Configuration work with the name, remember that each processor in a group **must** have a unique name.

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
java -jar nifi-deploy-config-1.1.5.jar \
  -nifi http://ip-nifi-dev:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/test2.json \
  -mode extractConfig
```

#### Sample update configuration

```shell
java -jar nifi-deploy-config-1.1.5.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/test2.json \
  -mode updateConfig
```

#### Sample deploy Template

```shell
java -jar nifi-deploy-config-1.1.5.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/my_template.xml \
  -m deployTemplate
```

#### Sample undeploy

```shell
java -jar nifi-deploy-config-1.1.3.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -m undeploy
```

force mode actived

```shell
java -jar nifi-deploy-config-1.1.3.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -m undeploy
  -f
  -timeout 600
  -interval 10
```

#### Sample access via username/password

```shell
java -jar nifi-deploy-config-1.1.5.jar \
  -user my_username \
  -password my_password \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/test2.json \
  -m updateConfig
```

#### Sample access via Kerberos ticket exchange / SPNEGO negotiation

```shell
java -jar nifi-deploy-config-1.1.5.jar \
  -accessFromTicket \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/test2.json \
  -m updateConfig
```

### Advanced Options

Pooling
```shell
 -timeout <arg>            allows specifying the polling timeout in second (defaut 120 seconds); negative values indicate no timeout
 -interval <arg>           allows specifying the polling interval in second (default 2 seconds)
```

 Security
 ```shell
 -password <arg>           password for access via username/password, then user is mandatory
 -user <arg>               user name for access via username/password, then password is mandatory
 -accessFromTicket         Access via Kerberos ticket exchange / SPNEGO negotiation
 -noVerifySsl              turn off ssl verification certificat
 
 ```

 Timeout Api Client
 ```shell
 -connectionTimeout <arg>  configure api client connection timeout (default 10 seconds)
 -readTimeout <arg>        configure api client read timeout (default 10 seconds)
 -writeTimeout <arg>       configure api client write timeout (default 10 seconds)
 ```

 Other
 ```shell
 -f,--force                turn on force mode : empty queue after timeout
 -noStartProcessors        turn off auto start of the processors after update of the config
 -enableDebugMode          turn on debugging mode of the underlying API library
 ```
# TODO

add version management that undeploy the old version automatically (with a version # in comment?)

All idea are welcome.

# Troubleshooting

### Proxy

If you are behind a proxy,  try adding these system properties on the command line:

```
-Dhttp.proxyHost=myproxy.mycompany.com -Dhttp.proxyPort=3128
```

See more at [http://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html](http://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html)
