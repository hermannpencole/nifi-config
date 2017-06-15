

# Deploy and configure Template on Nifi 
[![Maven Central](https://img.shields.io/maven-central/v/com.github.hermannpencole/nifi-deploy-config.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.hermannpencole/nifi-deploy-config)
[![Build Status](https://travis-ci.org/hermannpencole/nifi-config.svg?branch=master)](https://travis-ci.org/hermannpencole/nifi-config/)
[![codecov](https://codecov.io/gh/hermannpencole/nifi-config/branch/master/graph/badge.svg)](https://codecov.io/gh/hermannpencole/nifi-config)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c165156aaa3242bc9a41dc6225e19706)](https://www.codacy.com/app/hermannpencole/nifi-config?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=hermannpencole/nifi-config&amp;utm_campaign=Badge_Grade)

Update, Extract Nifi Configuration

Deploy, undeploy template

## Which version

| version                                  | version NIFI                             |
| ---------------------------------------- | ---------------------------------------- |
| -                                        | 0.X                                      |
| version 1.1.X  [download it from Maven Central](http://central.maven.org/maven2/com/github/hermannpencole/nifi-deploy-config/) | 1.0.X : to test <br/>1.1.X : OK, Build on api nifi  1.1.0 with [nifi-swagger-client](https://github.com/hermannpencole/nifi-swagger-client) 1.1.0.x <br/> 1.2.X : OK<br/> 1.3.X : OK |

## How to :

```shell
usage: java -jar nifi-deploy-config-1.1.3.jar [OPTIONS]
 -b,--branch <arg>         process group to begin (must begin by root) : root > my group > my sub group (default root)
 -c,--conf <arg>           adresse configuration file mandatory with mode (updateConfig/extractConfig/deployTemplate)
 -h,--help                 Usage description
 -m,--mode <arg>           mandatory :updateConfig/extractConfig/deployTemplate/undeploy
 -n,--nifi <arg>           mandatory : Nifi http (ex : http://localhost:8080/nifi-api)
 -password <arg>           password for access via username/password, then user is mandatory
 -user <arg>               user name for access via username/password, then password is mandatory
 -accessFromTicket         Access via Kerberos ticket exchange / SPNEGO negotiation
 -noVerifySsl              turn off ssl verification certificat
```

Requirement : *You must have java 8 or higher installed on your machine*

## Step by Step : use in real live

### Prepare your nifi development

1 ) Create a template on nifi : 

with this rule : each processor in a process group **must** have a unique name

![template](/docs/template.png)

2) and download it

3) Extract a sample configuration with the command

```shell
java -jar nifi-deploy-config-1.1.3.jar \
  -nifi http://ip-nifi-dev:8080/nifi-api \
  -branch "root>My Group>My Subgroup" \
  -conf /tmp/config.json \
  -mode extractConfig
```

### Deploy it on production

1a) undeploy the old version with the command

```shell
java -jar nifi-deploy-config-1.1.3.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>My group>My Subgroup" \
  -m undeploy
```

1b) deploy the template with the command

```shell
java -jar nifi-deploy-config-1.1.3.jar \
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
### Sample usage

#### Sample extract configuration

```shell
java -jar nifi-deploy-config-1.1.3.jar \
  -nifi http://ip-nifi-dev:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/test2.json \
  -mode extractConfig
```

#### Sample update configuration

```shell
java -jar nifi-deploy-config-1.1.3.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/test2.json \
  -mode updateConfig
```

#### Sample deploy Template

```shell
java -jar nifi-deploy-config-1.1.3.jar \
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

#### Sample access via username/password

```shell
java -jar nifi-deploy-config-1.1.3.jar \
  -user my_username \
  -password my_password \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/test2.json \
  -m updateConfig
```

#### Sample access via Kerberos ticket exchange / SPNEGO negotiation

```shell
java -jar nifi-deploy-config-1.1.3.jar \
  -accessFromTicket \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/test2.json \
  -m updateConfig
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