# JSON Logger

Drop-in replacement for default Mule Logger that outputs a JSON structure based on a predefined JSON schema.

## Why?

Typically we put the burden of generating logs to developers. While doing so we also expect all developers will think alike or align to a particular standard without giving them any tools to enforce that standard.

Small but core best practices like embedding a correlation Id on each log entry are unfortunately not always followed which drastically impacts troubleshooting down the road. 

Furthermore, the rise of Splunk and other log aggregators don't get fully exploited since we tend to aggregate raw log entries with no particular way for these tools to index the contents.

A great way to help tools like Splunk "understand" what data is being aggregated so that it could be properly indexed, is to have a standardized JSON structure. Then, the full potential of these tools can be achieved since it unlocks the ability to build advanced dashboards and reports around the data flowing throughtout their applications.

For this purpose, I created this generic JSON Logger connector (based on devkit) where we can define a JSON schema of the desired output format. Then by providing some additional devkit specific details to that JSON schema, we can dynamically generate a connector that aligns to that standard and which should become the replacement for the out of the box logger component.

## Installation

While the module `json-logger-module` is based on DevKit, as part of the dynamic code generation it uses a maven plugin called `jsonschema2pojo` to generate the required POJO's based on a predefined JSON schema. However, in order to generate a POJO that also includes DevKit specific annotations, I created a custom annotation extension called `jsonschema2pojo-custom-devkitannotation`

Thus, in order to be able to install this connector, you first need to install this extension:

```bash
cd jsonschema2pojo-custom-devkitannotation
mvn clean install
```

Then you can proceed to install the `json-logger-module` as any other DevKit connector.

## Usage

As already described, the idea is that based on a JSON schema (actually 2 JSON schemas) we can generate a connector that takes in those parameters in a standard manner, ideally with most values defined from default expressions (e.g. `correlationId` could have a default expression of `#[message.id]`)

Inside the `json-logger-module` we will find the following JSON schemas:
```markdown
- src/main/resources
  - schemas
    - loggerConfig.json
    - loggerProcessor.json
```

As the names imply, `loggerConfig.json` is the schema that defines the attributes that will be configurable in the Connector's config. At the same time, `loggerProcessor.json` is the schema that defines the final attributed that will form the final JSON output in the log files.

### Supported annotations per schema:

Given both schemas serve different purposes, the supported devkit annotations are also different. Here is a description on the currently supported devkit annotations per schema type.

#### loggerConfig.json

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
