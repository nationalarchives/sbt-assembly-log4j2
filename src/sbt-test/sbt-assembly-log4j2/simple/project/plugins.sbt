{
  val pluginVersion = System.getProperty("plugin.version")
  if(pluginVersion == null)
    throw new RuntimeException("""|The system property 'plugin.version' is not defined.
                                  |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
  else {
    addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.1")
    addSbtPlugin("uk.gov.nationalarchives" % """sbt-assembly-log4j2""" % pluginVersion)
  }
}
