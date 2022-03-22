package org.mule.extension.jsonlogger;

import org.mule.extension.jsonlogger.config.JsonLoggerConfig;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;

@Xml(prefix = "json-logger")
@Extension(name = "JSON Logger")
@Configurations(JsonLoggerConfig.class)
public class JsonLoggerExtension {

}
