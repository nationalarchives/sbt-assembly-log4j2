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
  private val testDat = "test.dat"
  private val test2Dat = "test2.dat"

  def createWatchService(): WatchServiceAdapter = {
    val watchService = FileSystems.getDefault.newWatchService()

    val watchServiceAdapter = new WatchServiceAdapter(watchService)
    watchServiceAdapter.register(Paths.get("/tmp"), ENTRY_CREATE, ENTRY_DELETE)
    watchServiceAdapter
  }

  private def library(name: String) = {
    val bytes = Source.fromResource(name).map(_.toByte).toArray
    Library(moduleCoordinate, src, target, () => new ByteArrayInputStream(bytes))
  }

  "log4j2MergeStrategy" should "create and delete temporary files" in {
    val watchServiceAdapter = createWatchService()

    val input = Vector(library(testDat), library(test2Dat))
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
    val lib = library(testDat)
    val input = Vector(lib, lib)
    val response = log4j2MergeStrategy(input)
    val responseVector = response match {
      case Left(err)    => throw new Exception(err)
      case Right(value) => value
    }
    responseVector.size should equal(1)
    responseVector.head.stream().readAllBytes().length should equal(lib.stream().readAllBytes().length)
  }

  "log4j2MergeStrategy" should "merge different dat files into one" in {

    val libraryOne = library(testDat)
    val libraryTwo = library(test2Dat)

    val input = Vector(libraryOne, libraryTwo)
    val response = log4j2MergeStrategy(input)
    val responseVector = response match {
      case Left(err)    => throw new Exception(err)
      case Right(value) => value
    }
    responseVector.size should equal(1)
    val responseFileSize = responseVector.head.stream().readAllBytes().length
    responseFileSize > libraryOne.stream().readAllBytes().length should equal(true)
    responseFileSize > libraryTwo.stream().readAllBytes().length should equal(true)
  }

  "log4j2MergeStrategy" should "return an error if there is an error merging the files" in {
    val input = library("invalid")
    val response = log4j2MergeStrategy(Vector(input))

    response.isLeft should be(true)
  }
}
