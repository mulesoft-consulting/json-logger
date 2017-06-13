# JSON Logger

Drop-in replacement for default Mule Logger that outputs a JSON structure based on a predefined JSON schema.

Typically we put the burden of generating logs to developers. While doing so we also expect all developers will think alike or align to a particular standard without giving them any tools to enforce that standard.

Small but core best practices like embedding a correlation Id on each log entry are unfortunately not always followed which drastically impacts troubleshooting down the road. 

Furthermore, the rise of Splunk and other log aggregators don't get fully exploited since we tend to aggregate raw log entries with no particular way for these tools to index the contents.

A great way to help tools like Splunk "understand" what data is being aggregated so that it could be properly indexed, is to have a standardized JSON structure. Then, the full potential of these tools can be achieved since it unlocks the option to build advanced reports and dashboards around the data flowing throughtout their applications.

For this purpose, I created this generic JSON Logger connector (based on devkit) where we can define a JSON schema of the desired output format. Then by providing some additional devkit specific details to that JSON schema, we can dynamically generate a connector that aligns to that standard and which should become the replacement for the out of the box logger component.
