# Log4j2 merge plugin for sbt assembly

Log4j 2 uses a cache to store information about the plugins it discovers during initialization.

The cache file has the `.dat` extension and contains serialized data related to the plugin classes found in the classpath. This cache file helps Log4j 2 avoid re-scanning the classpath every time the application starts up, resulting in faster startup times.

When building a jar file, these `.dat` files need to be merged using the log4j-core library. Merging them using the sbt-assembly doesn't work.

This plugin implements a `CustomMergeStrategy` which uses the log4j2 libraries to merge multiple `.dat` files.
## Usage
In `project/plugins.sbt` add 

```
addSbtPlugin("uk.gov.nationalarchives" % "sbt-assembly-log4j2" % "x.x.x")
```

Add the merge strategy to your assembly merge strategy in `build.sbt` 
```scala
import uk.gov.nationalarchives.sbt.Log4j2MergePlugin

(assembly / assemblyMergeStrategy) := {
  case PathList(ps@_*) if ps.last == "Log4j2Plugins.dat" => Log4j2MergePlugin.log4j2MergeStrategy
  case _ => MergeStrategy.first
}
```

### Testing

Run `sbt test` for the unit tests.

Run `sbt scripted` for [sbt script tests](http://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html).

### CI
The plugin is deployed to Maven central using GitHub actions.

