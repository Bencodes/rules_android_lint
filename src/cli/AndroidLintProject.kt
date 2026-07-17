package com.rules.android.lint.cli

import java.io.StringWriter
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.io.path.absolutePathString
import kotlin.io.path.pathString

/**
 * A first-party dependency module contributing partial analysis results to the report phase.
 *
 * In the report (`--report-only`) phase lint merges these modules' partial results into the main
 * module's verdict without re-analyzing their sources. Each is registered as a library module
 * carrying its own `partial-results-dir` and linked from the main module via a `<dep>` element.
 */
internal data class LintDependencyModule(
  val name: String,
  val partialResultsDir: Path,
  val isAndroid: Boolean = false,
)

internal fun createProjectXMLString(
  moduleName: String,
  rootDir: String,
  srcs: List<Path>,
  resources: List<Path>,
  androidManifest: Path?,
  isAndroid: Boolean = androidManifest != null,
  classpathJars: List<Path>,
  classpathAars: List<Path>,
  classpathExtractedAarDirectories: List<Pair<Path, Path>>,
  customLintChecks: List<Path>,
  partialResultsDir: Path? = null,
  dependencyModules: List<LintDependencyModule> = emptyList(),
  // Scratch partial-results directory assigned to AAR dependency projects during partial
  // analysis. These projects are not analyzed, but lint's partial-analysis detectors dereference
  // every project's partial-results-dir (e.g. JoinEffectDetector), so it must be non-null.
  aarPartialResultsScratchDir: Path? = null,
): String {
  val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()

  val projectElement = document.createElement("project")
  document.appendChild(projectElement)

  document.createElement("root").also {
    it.setAttribute("dir", rootDir)
    projectElement.appendChild(it)
  }

  val moduleElement =
    document.createElement("module").also {
      it.setAttribute("name", moduleName)
      it.setAttribute("android", isAndroid.toString())
      // The partial-results-dir is where lint writes results in `--analyze-only` and reads them
      // back in `--report-only`. Absent in the legacy single-shot mode.
      if (partialResultsDir != null) {
        it.setAttribute("partial-results-dir", partialResultsDir.absolutePathString())
      }
      // it.setAttribute("library", "false")
      // it.setAttribute("compile-sdk-version", "get-actual-value-here")
      projectElement.appendChild(it)
    }

  customLintChecks.forEach { jar ->
    document.createElement("lint-checks").also {
      it.setAttribute("jar", jar.absolutePathString())
      projectElement.appendChild(it)
    }
  }

  srcs.forEach { src ->
    val element = document.createElement("src")
    element.setAttribute("file", src.pathString)
    moduleElement.appendChild(element)
  }

  resources.forEach { res ->
    val element = document.createElement("resource")
    element.setAttribute("file", res.pathString)
    moduleElement.appendChild(element)
  }

  if (androidManifest != null) {
    val element = document.createElement("manifest")
    element.setAttribute("file", androidManifest.pathString)
    moduleElement.appendChild(element)
  }

  classpathJars.forEach { jar ->
    val element = document.createElement("classpath")
    element.setAttribute("jar", jar.absolutePathString())
    moduleElement.appendChild(element)
  }

  classpathAars.forEach { aar ->
    val element = document.createElement("aar")
    element.setAttribute("file", aar.absolutePathString())
    if (aarPartialResultsScratchDir != null) {
      element.setAttribute("partial-results-dir", aarPartialResultsScratchDir.absolutePathString())
    }
    moduleElement.appendChild(element)
  }

  classpathExtractedAarDirectories.forEach { (aar, unzippedDir) ->
    val element = document.createElement("aar")
    element.setAttribute("file", aar.absolutePathString())
    element.setAttribute("extracted", unzippedDir.absolutePathString())
    if (aarPartialResultsScratchDir != null) {
      element.setAttribute("partial-results-dir", aarPartialResultsScratchDir.absolutePathString())
    }
    moduleElement.appendChild(element)
  }

  // Link the main module to each first-party dependency that contributed partial results, then
  // register those dependencies as library modules carrying their own partial-results-dir.
  dependencyModules.forEach { dependency ->
    document.createElement("dep").also {
      it.setAttribute("module", dependency.name)
      moduleElement.appendChild(it)
    }
  }

  dependencyModules.forEach { dependency ->
    document.createElement("module").also {
      it.setAttribute("name", dependency.name)
      it.setAttribute("android", dependency.isAndroid.toString())
      it.setAttribute("library", "true")
      it.setAttribute("partial-results-dir", dependency.partialResultsDir.absolutePathString())
      projectElement.appendChild(it)
    }
  }

  return StringWriter()
    .apply {
      val transformer = TransformerFactory.newInstance().newTransformer()
      transformer.setOutputProperty(OutputKeys.INDENT, "yes")
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
      transformer.transform(DOMSource(document), StreamResult(this))
    }.buffer
    .toString()
}
