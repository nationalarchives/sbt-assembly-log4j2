package uk.gov.nationalarchives.sbt

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.*
import sbt.io.WatchService.WatchServiceAdapter
import sbtassembly.Assembly.{Library, ModuleCoordinate}
import uk.gov.nationalarchives.sbt.Log4j2MergePlugin.log4j2MergeStrategy

import java.io.ByteArrayInputStream
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchEvent.Kind
import java.nio.file.{FileSystems, Path, Paths, WatchEvent}
import scala.io.Source

class Log4j2MergePluginSpec extends AnyFlatSpec {

  val moduleCoordinate: ModuleCoordinate = ModuleCoordinate("test.org", "name", "version")
  val src = "/tmp"
  val target = "/tmp"

  def createWatchService(): WatchServiceAdapter = {
    val watchService = FileSystems.getDefault.newWatchService()

    val watchServiceAdapter = new WatchServiceAdapter(watchService)
    watchServiceAdapter.register(Paths.get("/tmp"), ENTRY_CREATE, ENTRY_DELETE)
    watchServiceAdapter
  }

  "log4j2MergeStrategy" should "create and delete temporary files" in {
    val watchServiceAdapter = createWatchService()

    val bytes = Source.fromResource("test.dat").map(_.toByte).toArray
    val library = Library(moduleCoordinate, src, target, () => new ByteArrayInputStream(bytes))

    val input = Vector(library, library)
    log4j2MergeStrategy(input)

    val events = watchServiceAdapter.pollEvents()
    val eventList: Seq[WatchEvent[Path]] = events.head._2

    def countEvents(prefix: String, kind: Kind[Path]) =
      eventList.count(ev => ev.context().toString.startsWith(prefix) && ev.kind() == kind)
    countEvents("Log4j2Plugins", ENTRY_CREATE) should equal(2)
    countEvents("Log4j2Plugins", ENTRY_DELETE) should equal(2)
    countEvents("merged", ENTRY_CREATE) should equal(1)
    countEvents("merged", ENTRY_DELETE) should equal(1)
  }

  "log4j2MergeStrategy" should "merge two of the same dat file into one" in {
    val bytes = Source.fromResource("test.dat").map(_.toByte).toArray
    val library = Library(moduleCoordinate, src, target, () => new ByteArrayInputStream(bytes))

    val input = Vector(library, library)
    val response = log4j2MergeStrategy(input)
    val responseVector = response match {
      case Left(err)    => throw new Exception(err)
      case Right(value) => value
    }
    responseVector.size should equal(1)
    responseVector.head.stream().readAllBytes().length should equal(bytes.length)
  }

  "log4j2MergeStrategy" should "merge different dat files into one" in {
    def library(name: String) = {
      val bytes = Source.fromResource(name).map(_.toByte).toArray
      Library(moduleCoordinate, src, target, () => new ByteArrayInputStream(bytes))
    }
    val libraryOne = library("test.dat")
    val libraryTwo = library("test2.dat")

    val input = Vector(libraryOne, libraryTwo)
    val response = log4j2MergeStrategy(input)
    val responseVector = response match {
      case Left(err)    => throw new Exception(err)
      case Right(value) => value
    }
    responseVector.size should equal(1)
    responseVector.head.stream().readAllBytes().length > libraryOne.stream().readAllBytes().length
    responseVector.head.stream().readAllBytes().length > libraryTwo.stream().readAllBytes().length
  }
}
