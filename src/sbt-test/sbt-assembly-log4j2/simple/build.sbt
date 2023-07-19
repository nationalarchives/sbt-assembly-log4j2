import java.util.jar.JarFile
import uk.gov.nationalarchives.sbt.Log4j2MergePlugin

version := "0.1"

lazy val log4jVersion = "2.20.0"
lazy val log4jCore = "log4j-core"
lazy val log4jTemplateJson = "log4j-layout-template-json"
lazy val root = (project in file("."))
  .settings(
    libraryDependencies ++= Seq(
      "org.apache.logging.log4j" % log4jCore % log4jVersion,
      "org.apache.logging.log4j" % log4jTemplateJson % log4jVersion
    ),
    addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.1"),
    TaskKey[Unit]("check") := {
      def getJar(name: String) = new JarFile((Compile / dependencyClasspath).value.map(_.data)
        .filter(_.getAbsolutePath.contains(name)).head)
      val assembledJarFile = new JarFile((assembly / assemblyOutputPath).value)
      val metaInfLocation = "META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat"
      val coreDatFile = getJar(log4jCore).getJarEntry(metaInfLocation)
      val templateJsonDatFile = getJar(log4jTemplateJson).getJarEntry(metaInfLocation)
      val datFile = assembledJarFile.getJarEntry(metaInfLocation)

      if(datFile == null) {
        sys.error("Missing Log4j2Plugins.dat")
      }
      if(datFile.getSize <= coreDatFile.getSize) {
        sys.error("Assembled dat file size is less than log4j core dat file")
      }
      if (datFile.getSize <= templateJsonDatFile.getSize) {
        sys.error("Assembled dat file size is less than log4j template json dat file")
      }
      if (datFile.getSize >= templateJsonDatFile.getSize + coreDatFile.getSize) {
        sys.error("Assembled dat file is larger than the sum of the original dat files")
      }
    },
    (assembly / assemblyMergeStrategy) := {
      case PathList(ps@_*) if ps.last == "Log4j2Plugins.dat" => Log4j2MergePlugin.log4j2MergeStrategy
      case _ => MergeStrategy.first
    },
    (assembly / assemblyJarName) := "test.jar"
  )
