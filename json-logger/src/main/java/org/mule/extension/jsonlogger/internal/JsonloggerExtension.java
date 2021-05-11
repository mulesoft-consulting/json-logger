package org.mule.extension.jsonlogger.internal;

import org.mule.extension.jsonlogger.internal.destinations.AMQDestination;
import org.mule.extension.jsonlogger.internal.destinations.AMQPDestination;
import org.mule.extension.jsonlogger.internal.destinations.Destination;
import org.mule.extension.jsonlogger.internal.destinations.JMSDestination;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;

/**
 * This is the main class of an extension, is the entry point from which configurations, connection providers, operations
 * and sources are going to be declared.
 */
@Xml(prefix = "json-logger")
@Extension(name = "JSON Logger")
@Export(resources = {"modules/JSONLoggerModule.dwl"})
@Configurations(JsonloggerConfiguration.class)
@SubTypeMapping(baseType = Destination.class,
        subTypes = {JMSDestination.class, AMQDestination.class, AMQPDestination.class})
public class JsonloggerExtension {

}
