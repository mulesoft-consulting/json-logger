# JSON Logger

Drop-in replacement for default Mule Logger that outputs a JSON structure based on a predefined JSON schema.

## Why?

- Logs are as good as the logging practices applied by developers
- Not all developers think alike on what needs to be logged
- No tools to enforce proper logging standards
- Simple but essential logging best practices (e.g. tie multiple log entries to a single transaction by using a correlation id) are inconsistent and drastically impact troubleshooting capabilities
- The rise of Splunk and other log aggregation platforms is due to their ability to aggregate, understand and exploit data
- These platforms typically understand key=value pairs or JSON data structures. Only then, the full potential of these tools can be unlocked in the form of advanced dashboards and reports

For these reasons and based on previous customer experiences, I created this generic DevKit-based JSON Logger Module.

In a nutshell, by defining the output JSON schema as well as providing some additional DevKit specific details (e.g. default values, default expressions, etc.), we can dynamically generate a module that aligns to that schema.

## Installation from Exchange

Starting with version `1.1.0`, the module has been published to our own MuleSoft Services exchange repository. Thus, the easiest way to install it (and use it as-is) would be through Exchange in Anypoint Studio.

The following guest user has been provisioned so that anyone can access and download the module from Exchange:

```
user: guest-mule
pass: Mulesoft1
```

**NOTE:** Assuming you are following best practices and using Mavenized Mule projects, for this module to work with you application build you will need to add those credentials to your Maven settings.xml as described in the [prerequisites](#prerequisites) section.

#### Configuration Options

Currently there are two configurations available for the JSON Logger Module.

- `Logger Configuration`: Provides basic configuration for local generation of logs
- `Logger with Anypoint MQ Configuration`: On to of the basic it also lets you configure an Anypoint MQ destination. This enables the connector to not only log the entries locally but also send them to Anypoint MQ asynchronously (if you must know, this feature uses [OkHttp3](https://github.com/square/okhttp) under the covers)

#### Default JSON structure

The following is a brief explanation of the properties configured in the default JSON Logger out-of-the-box:
```yaml
applicationName:
  - Description: Current application name
  - Default expression: ${json.logger.application.name}
applicationVersion:
  - Description: Current application version
  - Default expression: ${json.logger.application.version}
environment:
  - Description: Current environment
  - Default expression: ${mule.env}
timestamp:
  - Description: Current timestamp with a configurable time zone and date format (thus the crazy weird expression)
  - Default expression: #[new org.joda.time.DateTime().withZone(org.joda.time.DateTimeZone.forID(\"${json.logger.timezone}\")).toString(\"${json.logger.dateformat}\")]
    - Additional dependencies:
        - ${json.logger.timezone}: Time zone (e.g. US/Eastern)
        - ${json.logger.dateformat}: Date format (e.g. yyyy-MM-dd HH:mm:ss.SSS)
rootCorrelationId:
  - Description: Global cross-application correlation Id to trace transaction end-to-end. By default it assumes the x-root-correlation-id header will be received but if not, it will default to the current #[message.id]
  - Default expression: #[(message.inboundProperties.'x-root-correlation-id' != null)?message.inboundProperties.'x-root-correlation-id':message.id]
correlationId:
  - Description: Local correlation Id to trace a transaction within the current application
  - Default expression: #[message.id]
threadName:
  - Description: Current executing Thread
  - Default value: Calculated internally
  - Mandatory: true
elapsed:
  - Description: Elapsed time
  - Default value: Calculated internally
  - Mandatory: true
priority:
  - Description: Defines the priority for the log entry
  - Default enum: DEBUG, INFO (default), WARN, ERROR
  - Mandatory: true
tracePoint:
  - Description: Identifies the different checkpoints within a flow
  - Default enum: START, BEFORE TRANSFORM, AFTER TRANSFORM, BEFORE API OR BACKEND, AFTER API OR BACKEND, END, EXCEPTION
message:
  - Description: Message that developer wants to log
  - Default expression: none (could be hardcoded value or MEL expression)
```
**NOTE:** There are some properties that currently are mandatory. Removing them will break the module's code.

## Customization

Of course, the very purpose of the JSON Logger is to facilitate the customization of the generated JSON output. In order to do so you need to do the following:

**NOTE:** Before doing anything, please make sure you completed these [prerequisites](#prerequisites).

#### 1. Clone the repo
```git
git clone git@github.com:mulesoft-consulting/json-logger.git
```

#### 2. Repository structure
- `json-logger-module`: Main module code
- `json-logger-test-app`: Test Mule application
- `jsonschema2pojo-custom-devkitannotation`: Custom jsonschema2pojo extension to support DevKit annotations (provided in case you want to add additional DevKit annotations that may not be covered already)
- `maven-settings`: Provides settings.xml template for Maven
- `properties`: Contains two template property files
  - `mule-app.properties`: Defines common properties for JSON Logger
  - `mule.dev.properties`: Defines properties that should be dynamically inferred from Maven's pom.xml (e.g. application name = Maven artifactId, application version = Maven artifactVersion) [[how to](#dynamicMaven)]

#### 3. Import the DevKit project
Import the `json-logger-module` as:
```
Import... > Anypoint Connector Project from External Location > Select json-logger-module folder cloned
```
**NOTE:** This assumes you already installed DevKit in your Anypoint Studio

#### 4. Test it before you break it!

Feel free to import the `json-logger-test-app` in Anypoint Studio as
```
Import... > Maven-based Mule Project from pom.xml > pom.xml
```
in order to see how it works out-of-the-box.

#### 5. Customize the output JSON log
The main objective of this module is to allow customization of the output JSON logs by configuring JSON Schemas rather than coding those changes.

Inside the `json-logger-module` we will find the following JSON schemas:
```markdown
- src/main/resources
  - schemas
    - loggerConfig.json
    - loggerProcessor.json
```

- `loggerConfig.json`: Defines which properties are gonna be exposed in the JSON Logger global configuration
- `loggerProcessor.json`: This could be considered the **main schema** as it defines what the final output JSON should look like

##### Supported DevKit annotations per schema:

Given both schemas serve different purposes, the supported DevKit annotations are also different. Here is a description on the currently supported DevKit annotations per schema type.

##### `loggerConfig.json`

```json
"prettyPrint":{
  "type":"boolean",
  "devkit": {
    "description": "Currently this field is mandatory so DON'T REMOVE",
    "placement":"JSON Output",
    "default": "true",
    "isConfig": true
  }
}
```

With the exception of `"description"` all the attributes within devkit will map to an specific devkit annotation:  

- `placement`: Indicates the placement of the attribute ([@Placement](https://docs.mulesoft.com/anypoint-connector-devkit/v/3.8/defining-connector-attributes#placement-field-order-grouping-and-tabs))
- `default`: String value or MEL expression assigned by default ([@Default](https://docs.mulesoft.com/anypoint-connector-devkit/v/3.8/defining-connector-attributes#default-annotation))
- `isConfig`: Boolean that indicates if this attribute should be part of the configurable attributes ([@Configurable](https://docs.mulesoft.com/anypoint-connector-devkit/v/3.8/defining-connector-attributes#configurable-annotation))

##### `loggerProcessor.json`

```json
"correlationId":{
  "type":"string",
  "devkit": {
    "default": "#[message.id]",
    "isHidden": false
  }
}
```

- `default`: String value or MEL expression assigned by default ([@Default](https://docs.mulesoft.com/anypoint-connector-devkit/v/3.8/defining-connector-attributes#default-annotation))
- `isHidden`: Boolean value that will ignore this attribute at the message processor level but will keep it as part of the output JSON structure. This means that the vaule needs to be setup either programatically or by using a default expression defined at the config level (see [passing global expressions as processor values](#passing-global-expressions-as-processor-values)) ([@Ignore](https://docs.mulesoft.com/anypoint-connector-devkit/v/3.7/annotation-reference#ignan))

##### Passing global expressions as message processor values

In most situations, we may want a particular value in the output JSON structure (e.g. timestamp) but we don't want the developer to be able to tinker with the default value at the message processor level every time they drag and drop a Logger component. Rather we want this field to be auto-calculated or defined globally (e.g. at the module's config level). In order to keep the module code changes minimal (or preferably non-existent) the base module code can override the values in the Logger message processor with either values or MEL expressions defined in the module's config. All that we need is to have the exact same attribute name in the `loggerConfig.json` as defined in the `loggerProcessor.json`. Also, because we are overriding the value of the attribute in the JSON message processor, we should mark it as `"isHidden": true`

For example:

From `loggerConfig.json`:

```json
"timestamp":{
  "type":"string",
  "devkit": {
    "placement":"Default expressions",
    "default": "#[new org.joda.time.DateTime().withZone(org.joda.time.DateTimeZone.forID(\"${json.logger.timezone}\")).toString(\"${json.logger.dateformat}\")]",
    "isConfig": true
  }
}
```

From `loggerProcessor.json`:

```json
"timestamp":{
  "type":"string",
  "devkit": {
    "default": "should be defined on the module config schema",
    "isHidden": true
  }
}
```

In this example we are telling the module that the value for the "timeStamp" property in the output JSON will be defined by the expression "#[new org.joda.time.DateTime().withZone(org.joda.time.DateTimeZone.forID(\"${json.logger.timezone}\")).toString(\"${json.logger.dateformat}\")]" defined in the connector's config.

**NOTE:** MEL expressions (`#[]`) will be resolved at runtime while properties (`${}`) are resolved at startup time.

#### 5. Rebuild
Once you finished changing the JSON schemas, all that is left to do is rebuild the DevKit connector and decide your deployment and distribution model.

## Publish to Anypoint Exchange

After the awesome Anypoint Platform [Crowd release](https://videos.mulesoft.com/watch/nwMkVULZBxL3wTAu7fZsZZ), Exchange 2.0 became our go-to central repository for all things Mule. In this guide I'll explain how we can deploy the connector to your very own Anypoint Exchange repository (although the same steps should work to deploy to your own Maven Repository).

#### 1. Obtain your Anypoint Organization Id
Follow this [guide](https://docs.mulesoft.com/anypoint-exchange/ex2-maven#vieworgid)

#### 2. Publish
In the project `json-logger-module` there is a `pom.xml` file. It is pre-configured so that the only thing you need to provide is your Anypoint Platform Organization Id (obtained in the previous step)

```xml
<anypoint.org.id>YOUR_ORG_ID</anypoint.org.id>
```

Last but not least, open a terminal window and navigate to your `json-logger-module` location and run the following command:

```
mvn clean deploy
```

Which in turn should trigger the build, package and publishing to your Anypoint Exchange!!

## Assumptions (or tech debt?)

#### Elapsed time calculation

A popular requirement among our customers is to be able to calculate elapsed times throughout the mule application. In order to do so, the module uses a `timer` flow variable. By default it is assigned to `#[flowVars['timerVariable']]`.

Everytime the logger is used, the module will look for a default `timerVariable flowVar` (this can be changed per processor) and if it doesn't find the specified flowVar it will create one with the current timestamp value. When the module finds the variable (e.g. the second time it's used), instead of setting the current timestamp it will calculate the elapsed time between the original timestamp value and the current timestamp at that point.

For better or worse, the current version of JSON Logger assumes the "elapsed" property is mandatory. An enhancement would be to make it dynamic but currently run out of time :)

#### JSON pretty print

Currently the module config requires an attribute called `prettyPrint` that tells the module if the output should be a formated JSON or inline.

#### Flat JSON data structure

Unfortunately DevKit processors are not able to traverse nested objects, which means the current implementation of this module can only handle flat JSON data structures (at least if we want to keep it zero-code)

## <a name="prequisites"></a>Prerequisites
#### 1. Install Java 8 JDK
#### 2. Install and configure Maven (Mac OS instructions...)
I'm not gonna go into details on how to do this but the very high level steps are:
- Download [maven](https://maven.apache.org/download.cgi)
- Explode zip/tar.gz into your file system (use a location with no spaces. e.g. `/apps/maven/<exploded_archive>`)
- Create M2_HOME environment variable pointing to the new maven directory:
  - e.g. `export M2_HOME=/apps/maven/apache-maven-3.5.0`
- Add M2_HOME/bin to the default PATH environment variable:
  - e.g. `export PATH=$PATH:$M2_HOME/bin`
- Open a new terminal and test:
  - `mvn --version`
- Assuming all went well so far, maven will create a hidden folder name .m2 under your user home (e.g. `/Users/andresramirez/.m2`) that will be used to locally cache the different Maven dependencies your apps need
- Then copy the default `settings.xml` file provided under `maven-settings` folder
- Finally, open the settings.xml and edit the following:
Assuming you are a beloved customer, support should already have provided Mule EE Nexus repo credentials that need to be replaced here:
```xml
<server>
    <id>MuleRepository</id>
	<!-- Provided by Support -->
	<username>CUSTOMER_EE_REPO_USER</username>
	<password>CUSTOMER_EE_REPO_PASS</password>
</server>
```
Lastly, replace your Anypoint Platform credentials here:
```xml
<!-- Customer Exchange Repository -->
<server>
	<id>Exchange2</id>
	<!-- NOTE: In order to be able to publish assets to exchange, this user will need the Exchange Contributor Role -->
	<username>ANYPOINT_PLATFORM_USER</username>
	<password>ANYPOINT_PLATFORM_PASS</password>
</server>
```

## <a name="dynamicMaven"></a>Access Maven values from property files
#### 1. Configure Maven pom.xml for resource filtering
```xml
<build>
  <resources>
    <resource>
      <directory>src/main/resources</directory>
      <filtering>true</filtering>
    </resource>
  </resources>
</build>
```
For the love of all things that are good and pure I was not able to tell Maven to replace the values on src/main/app (so that we could define these properties in the mule-app.properties) so this would be a nice enhacement request for you Maven pro's out there.

#### 2. Map Maven variables to your own property variables
```properties
json.logger.application.name=${project.artifactId}
json.logger.application.version=${project.version}
```

## Implementation details
- `json-logger-module`: Core module was build using DevKit 3.9.0
- `jsonschema2pojo`: This public Maven plugin allows for dynamic POJO generation based on a JSON Schema
- `jsonschema2pojo-custom-devkitannotation`: In order to add DevKit specific annotations to the POJO generation, I created an extension of the jsonschema2pojo plugin. This extension is deployed to MuleSoft Services exchange repository so you no longer need to install it locally to build the module

## Author

* **Andres Ramirez** [Slack: @andres.ramirez / Email: andres.ramirez@mulesoft.com]
