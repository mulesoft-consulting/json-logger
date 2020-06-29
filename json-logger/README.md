# Json-logger Extension

## 2.0.1 version - Release notes

Bug fixes:
* Added support for large payloads

## 2.0.0 version - Release notes

New features:
* External Destinations
* Data masking

Improvements:
* Field ordering

More details in the coming blog post (stay tuned!)

## 1.1.0 version - Release notes

New features:
* Scoped loggers to capture "scope bound elapsed time". Great for performance tracking of specific components (e.g. outbound calls)
* Added "Parse content fields in json output" flag so that content fields can become part of final JSON output rather than a "stringified version" of the content

Improvements:
* Removed Guava and caching in general with a more efficient handling of timers (for elapsed time)
* Optimized generation of JSON output
* Code optimizations
* Minimized dependency footprint (down from ~23MB to ~13MB)
* Optimized parsing of TypedValue content fields

