# Json-logger Extension for Mule 4 runtime

Json logger makes uses of [Mule Java SDK](https://docs.mulesoft.com/mule-sdk/1.1/getting-started) 
and contains a custom deserializer for masking fields in the JSON.

> Data masking is not possible in any other formats at this time e.g. xml, csv, etc.

## Serialization/Deserialization performance penalty
When the payload is passed into the json logger component as `application/json`, it is first deserialized 
and masking if specified is applied in place. The serialization takes place when the log is being written out. 
Therefore, at the very minimum, it is a two-step process. The transformation done before or after the json logger 
to convert payload to `application/java` from `application/json` and vice versa will degrade performance.

## Operations:
There are two operations available:
1. logger: Use this for regular logging 
2. loggerScope: Use this to measure response times for external calls

## Logging structure:
```json
 {
  "correlationId" : "cbfb9c1f-f904-40f2-bad9-2eac6bc05e84",
  "message" : "",
  "content": {
    "dateOfBirth": "**-**-****"
  },
  "priority" : "INFO",
  "tracePoint" : "START",
  "locationInfo" : {
    "fileName" : "api-main-router.xml",
    "rootContainer" : "api-main",
    "lineNumber" : "25"
  },
  "applicationName" : "proc-checkin-api-v1",
  "applicationVersion" : "1.2.7",
  "environment" : "local"
}
```