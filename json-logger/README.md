# Json-logger Extension

## 1.1.0 version - Release notes

Improvements:
* Removed Guava and caching in general with a more efficient handling of timers (for elapsed time)
* Optimized generation of JSON output
* Code optimizations
* Minimized dependency footprint (down from ~23MB to ~3MB)
* Optimized parsing of TypedValue content fields

New features:
* Scoped loggers to capture "scope bound elapsed time". Great for performance tracking of specific components (e.g. outbound calls)
* Added "Parse content fields in json output" flag so that content fields can become part of final JSON output rather than a "stringified version" of the content

Add this dependency to your application pom.xml

```
<groupId>YOUR_ORG_ID</groupId>
<artifactId>json-logger</artifactId>
<version>1.1.0-rc1</version>
```
