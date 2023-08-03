package uk.gov.nationalarchives.sbt

import org.apache.logging.log4j.core.config.plugins.processor.PluginCache
import org.apache.logging.log4j.core.config.plugins.processor.PluginProcessor.PLUGIN_CACHE_FILE
import sbt.IO
import sbt.io.Using
import sbtassembly.AssemblyPlugin.autoImport.JarEntry
import sbtassembly.{Assembly, CustomMergeStrategy, MergeStrategy}

import java.io.FileInputStream
import java.net.URL
import scala.jdk.CollectionConverters.*
import scala.util.Try

object Log4j2MergePlugin {
  private val suffix = "dat"
  private val pluginsPrefix = "Log4j2Plugins"
  private val mergedPrefix = "merged"

  val log4j2MergeStrategy: MergeStrategy = CustomMergeStrategy("log4j2-merge-strategy") {
    conflicts: Vector[Assembly.Dependency] =>
      Try {
        val uris: Vector[URL] = conflicts.map { c =>
          IO.withTemporaryFile(pluginsPrefix, suffix, keepFile = true) { file =>
            IO.transfer(c.stream(), file)
            file.toURI.toURL
          }
        }
        IO.withTemporaryFile(mergedPrefix, suffix) { merged =>
          Using.fileOutputStream()(merged) { fos =>
            val aggregator = new PluginCache()
            aggregator.loadCacheFiles(uris.toIterator.asJavaEnumeration)
            aggregator.writeCache(fos)
            val is = new FileInputStream(merged)
            IO.delete(uris.map(IO.toFile))
            Vector(JarEntry.apply(PLUGIN_CACHE_FILE, () => is))
          }
        }
      }.toEither.left.map(_.getMessage)
  }
}
