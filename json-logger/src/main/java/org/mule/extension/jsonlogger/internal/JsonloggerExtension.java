package org.mule.extension.jsonlogger.internal;

import org.mule.extension.jsonlogger.api.pojos.LoggerConfig;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;

@Xml(prefix = "json-logger")
@Extension(name = "JSON Logger")
@Export(resources = {"modules/JSONLoggerModule.dwl"})
@Configurations(JsonLoggerConfig.class)
public class JsonLoggerExtension {

}
