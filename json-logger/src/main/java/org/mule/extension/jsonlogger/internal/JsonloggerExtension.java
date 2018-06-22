package org.mule.extension.jsonlogger.internal;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;


/**
 * This is the main class of an extension, is the entry point from which configurations, connection providers, operations
 * and sources are going to be declared.
 */
@Xml(prefix = "json-logger")
@Extension(name = "JSON Logger")
@Configurations(JsonloggerConfiguration.class)
public class JsonloggerExtension {

}
