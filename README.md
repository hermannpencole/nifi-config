

# Nifi : deploy and configure Template 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.hermannpencole/nifi-deploy-config/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.hermannpencole/nifi-deploy-config)
[![Build Status](https://travis-ci.org/hermannpencole/nifi-swagger-client.svg?branch=master)](https://travis-ci.org/hermannpencole/nifi-deploy-config/)
[![codecov](https://codecov.io/gh/hermannpencole/nifi-config/branch/master/graph/badge.svg)](https://codecov.io/gh/hermannpencole/nifi-config)

Update, Extract Nifi Configuration

Deploy, undeploy template

## Which version

| version                                  | version NIFI    |
| ---------------------------------------- | --------------- |
| version 1.1.0 (last) [download it from Maven Central](http://central.maven.org/maven2/com/github/hermannpencole/nifi-deploy-config/) | for nifi  1.1.0 |

## How to :

```shell
usage: java -jar nifi-deploy-config-1.1.0.jar [OPTIONS]
 -accessFromTicket <arg>   Access via Kerberos ticket exchange / SPNEGO negotiation
 -b,--branch <arg>         branch to begin (must begin by root) : root > my group > my sub group (default root)
 -c,--conf <arg>           adresse configuration file mandatory with mode (updateConfig/extractConfig/deployTemplate)
 -h,--help                 Usage description
 -m,--mode <arg>           mandatory :updateConfig/extractConfig/deployTemplate/undeploy
 -n,--nifi <arg>           mandatory : Nifi http (ex : http://localhost:8080/nifi-api)
 -noVerifySsl <arg>        turn off ssl verification certificat
 -password <arg>           password for access via username/password, then user is mandatory
 -user <arg>               user name for access via username/password, then password is mandatory

```

## Strep by Step

### Prepare your nifi development

Create a template on nifi : 

![template](/docs/template.png)

and export it

Extract a sample configuration with the command

```shell
java -jar nifi-deploy-config-1.1.0.jar \
  -nifi http://ip-nifi-dev:8080/nifi-api \
  -branch "root>My Group>My Subgroup" \
  -conf /tmp/config.json \
  -mode extractConfig
```

### Deploy it on production

undeploy the old version with the command

```shell
java -jar nifi-deploy-config-1.1.0.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>My group>My Subgroup" \
  -m undeploy
```

deploy the template with the command

```shell
java -jar nifi-deploy-config-1.1.0.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>My group>My Subgroup" \
  -conf /tmp/my_template.xml \
  -m deployTemplate
```

update the production configuration with the command

```shell
java -jar nifi-deploy-config-1.1.0.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>My group>My Subgroup" \
  -conf /tmp/PROD_config.json \
  -mode updateConfig
```

## What can I configure

All !

You can find all properties in your extraction. Now configure it with the production properties and update your production.

sample :
```json
{
  "processors": [
    {
      "name": "TheListFile",
      "config": {
        "properties": {
          "Input Directory": "c:\\temp",
          "Recurse Subdirectories": "false",
          "Input Directory Location": "Local",
          "File Filter": "[^\\.].*",
          "Minimum File Age": "0 sec",
          "Minimum File Size": "0 B",
          "Ignore Hidden Files": "true"
        },
        "schedulingPeriod": "0 sec",
        "schedulingStrategy": "TIMER_DRIVEN",
        "executionNode": "ALL",
        "penaltyDuration": "30 sec",
        "yieldDuration": "1 sec",
        "bulletinLevel": "WARN",
        "runDurationMillis": 0,
        "concurrentlySchedulableTaskCount": 1,
        "autoTerminatedRelationships": [],
        "comments": "",
        "lossTolerant": false
      }
    }
  ],
  "groupProcessorsEntity": [
    {
      "processors": [
        {
          "name": "ConnectWebSocket",
          "config": {
            "properties": {},
            "schedulingPeriod": "0 sec",
            "schedulingStrategy": "TIMER_DRIVEN",
            "executionNode": "ALL",
            "penaltyDuration": "30 sec",
            "yieldDuration": "1 sec",
            "bulletinLevel": "WARN",
            "runDurationMillis": 0,
            "concurrentlySchedulableTaskCount": 1,
            "autoTerminatedRelationships": [],
            "comments": "",
            "lossTolerant": false
          }
        }
      ],
      "name": "test"
    }
  ]
}
```
### Sample

#### Sample extract configuration

```shell
java -jar nifi-deploy-config-1.1.0.jar \
  -nifi http://ip-nifi-dev:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/test2.json \
  -mode extractConfig
```

#### Sample update configuration

```shell
java -jar nifi-deploy-config-1.1.0.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/test2.json \
  -mode updateConfig
```

#### Sample deploy Template

```shell
java -jar nifi-deploy-config-1.1.0.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/my_template.xml \
  -m deployTemplate
```

#### Sample undeploy

```shell
java -jar nifi-deploy-config-1.1.0.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -m undeploy
```

#### Sample access via username/password

```shell
java -jar nifi-deploy-config-1.1.0.jar \
  -user my_username \
  -password my_password \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/test2.json \
  -m updateConfig
```

#### Sample access via Kerberos ticket exchange / SPNEGO negotiation

```shell
java -jar nifi-deploy-config-1.1.0.jar \
  -accessFromTicket \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/test2.json \
  -m updateConfig
```

# TODO

add version management that undeploy the old version automatically

All idea are welcome. 

# Troubleshooting

### Proxy

If you are behind a proxy,  try adding these system properties on the command line:

```
-Dhttp.proxyHost=myproxy.mycompany.com -Dhttp.proxyPort=3128
```

See more at [http://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html](http://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html)