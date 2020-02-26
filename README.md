# JSON Logger - Mule 4

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

## Installation

Please check this blogpost for more details: https://blogs.mulesoft.com/dev/anypoint-platform-dev/json-logging-in-mule-4-getting-the-most-out-of-your-logs/

## Sample Usage

Two configs should be created per project to do both encrypted logging as well as plain-text
logging.

### JSON Logger Config for Encrypted Logging

The encryption mechanism uses AES256.

```
<json-logger:config
		name="JSON_Logger_Config_Encrypt" doc:name="JSON Logger Config"
		disabledFields="${json.logger.application.disabledFields}"
		applicationName="${json.logger.application.name}"
		applicationVersion="${json.logger.application.version}"
		environment="${mule.env}"
		encryptPayload="true"
		secretKey="${json.logger.application.secretKey}"
		encryptionSalt="${json.logger.application.encryptionSalt}" />
```

OR

```
<json-logger:config
		name="JSON_Logger_Config_Encrypt" doc:name="JSON Logger Config"
		disabledFields="${json.logger.application.disabledFields}"
		applicationName="${json.logger.application.name}"
		applicationVersion="${json.logger.application.version}"
		environment="${mule.env}"
		encryptPayload="true"
		secretKey="${secure::json.logger.application.secretKey}"
		encryptionSalt="${secure::json.logger.application.encryptionSalt}" />
```

### JSON Logger Config for Plain-Text Logging

```
<json-logger:config
		name="JSON_Logger_Config_PlainTextt" doc:name="JSON Logger Config"
		disabledFields="${json.logger.application.disabledFields}"
		applicationName="${json.logger.application.name}"
		applicationVersion="${json.logger.application.version}"
		environment="${mule.env}"
		encryptPayload="false"
		secretKey="${json.logger.application.secretKey}"
		encryptionSalt="${json.logger.application.encryptionSalt}" />
```

OR

```
<json-logger:config
		name="JSON_Logger_Config_PlainTextt" doc:name="JSON Logger Config"
		disabledFields="${json.logger.application.disabledFields}"
		applicationName="${json.logger.application.name}"
		applicationVersion="${json.logger.application.version}"
		environment="${mule.env}"
		encryptPayload="false"
		secretKey="${secure::json.logger.application.secretKey}"
		encryptionSalt="${secure::json.logger.application.encryptionSalt}" />
```

## Author

* **Andres Ramirez** [Slack: @andres.ramirez / Email: andres.ramirez@mulesoft.com]
