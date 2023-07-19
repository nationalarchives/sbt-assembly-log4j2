import sbtrelease.ReleaseStateTransformations._

lazy val root: Project = project
  .in(file("."))
  .settings(
    sbtPlugin := true,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.15" % "test",
    addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.1" % Provided),
    name := "sbt-assembly-log4j2",
    organization := "uk.gov.nationalarchives",
    useGpgPinentry := true,
    publishTo := sonatypePublishToBundle.value,
    publishMavenStyle := true,

    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommand("publishSigned"),
      releaseStepCommand("sonatypeBundleRelease"),
      setNextVersion,
      commitNextVersion,
      pushChanges
    ),
    resolvers +=
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    version := (ThisBuild / version).value,
    organization := "uk.gov.nationalarchives",
    organizationName := "National Archives",

    scmInfo := Some(
      ScmInfo(
        url("https://github.com/nationalarchives/sbt-assembly-log4j"),
        "git@github.com:nationalarchives/sbt-assembly-log4j.git"
      )
    ),
    developers := List(
      Developer(
        id = "tna-digital-archiving-jenkins",
        name = "TNA Digital Archiving",
        email = "digitalpreservation@nationalarchives.gov.uk",
        url = url("https://github.com/nationalarchives/sbt-assembly-log4j")
      )
    ),
    description := "An sbt plugin used to merge log4j dat files",
    licenses := List("MIT" -> new URL("https://choosealicense.com/licenses/mit/")),
    homepage := Some(url("https://github.com/nationalarchives/sbt-assembly-log4j")),
    console / initialCommands := """import uk.gov.nationalarchives.sbt._""",
    scriptedLaunchOpts ++= Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
  ).enablePlugins(ScriptedPlugin)



