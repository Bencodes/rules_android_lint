package com.rules.android.lint.cli

import com.google.gson.Gson
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
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
 * module's verdict without re-analyzing their sources. Each is registered with the same module
 * identity and inputs used during analysis, carries its own `partial-results-dir`, and is linked
 * from the main module via a `<dep>` element.
 */
internal data class LintDependencyModule(
  val name: String,
  val partialResultsDir: Path,
  val isAndroid: Boolean = false,
  val isLibrary: Boolean = false,
  val srcs: List<Path> = emptyList(),
  val resources: List<Path> = emptyList(),
  val androidManifest: Path? = null,
  val classpathJars: List<Path> = emptyList(),
  val classpathAars: List<Path> = emptyList(),
  val classpathExtractedAarDirectories: List<Pair<Path, Path>> = emptyList(),
)

private data class SerializedLintDependencyModule(
  val name: String?,
  val partialResultsDir: String?,
  val isAndroid: Boolean,
  val isLibrary: Boolean,
  val srcs: List<String>?,
  val resources: List<String>?,
  val androidManifest: String?,
  val classpathJars: List<String>?,
  val classpathAars: List<String>?,
  val classpathExtractedAarDirectories: List<SerializedExtractedAar>?,
)

private data class SerializedExtractedAar(
  val aar: String?,
  val extracted: String?,
)

internal fun readLintDependencyModule(model: Path): LintDependencyModule {
  val serialized =
    Gson().fromJson(Files.readString(model), SerializedLintDependencyModule::class.java)
  return LintDependencyModule(
    name = requireNotNull(serialized.name) { "Dependency model $model has no name" },
    partialResultsDir =
      Paths.get(
        requireNotNull(serialized.partialResultsDir) {
          "Dependency model $model has no partialResultsDir"
        },
      ),
    isAndroid = serialized.isAndroid,
    isLibrary = serialized.isLibrary,
    srcs = serialized.srcs.orEmpty().map(Paths::get),
    resources = serialized.resources.orEmpty().map(Paths::get),
    androidManifest = serialized.androidManifest?.let(Paths::get),
    classpathJars = serialized.classpathJars.orEmpty().map(Paths::get),
    classpathAars = serialized.classpathAars.orEmpty().map(Paths::get),
    classpathExtractedAarDirectories =
      serialized.classpathExtractedAarDirectories.orEmpty().map { aar ->
        Paths.get(requireNotNull(aar.aar) { "Dependency model $model has an AAR with no path" }) to
          Paths.get(
            requireNotNull(aar.extracted) {
              "Dependency model $model has an AAR with no extracted directory"
            },
          )
      },
  )
}

internal fun createProjectXMLString(
  moduleName: String,
  rootDir: String,
  srcs: List<Path>,
  resources: List<Path>,
  androidManifest: Path?,
  isAndroid: Boolean = androidManifest != null,
  isLibrary: Boolean = false,
  isTestSources: Boolean = false,
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
      it.setAttribute("library", isLibrary.toString())
      // The partial-results-dir is where lint writes results in `--analyze-only` and reads them
      // back in `--report-only`. Absent in the legacy single-shot mode.
      if (partialResultsDir != null) {
        it.setAttribute("partial-results-dir", partialResultsDir.absolutePathString())
      }
      // it.setAttribute("compile-sdk-version", "get-actual-value-here")
      projectElement.appendChild(it)
    }

  customLintChecks.forEach { jar ->
    document.createElement("lint-checks").also {
      it.setAttribute("jar", jar.absolutePathString())
      projectElement.appendChild(it)
    }
  }

  appendModuleContents(
    document = document,
    moduleElement = moduleElement,
    srcs = srcs,
    isTestSources = isTestSources,
    resources = resources,
    androidManifest = androidManifest,
    classpathJars = classpathJars,
    classpathAars = classpathAars,
    classpathExtractedAarDirectories = classpathExtractedAarDirectories,
    aarPartialResultsScratchDir = aarPartialResultsScratchDir,
  )

  // Link the main module to each first-party dependency that contributed partial results, then
  // register those dependencies as library modules carrying their own partial-results-dir.
  dependencyModules.forEach { dependency ->
    document.createElement("dep").also {
      it.setAttribute("module", dependency.name)
      moduleElement.appendChild(it)
    }
  }

  dependencyModules.forEach { dependency ->
    val dependencyElement =
      document.createElement("module").also {
        it.setAttribute("name", dependency.name)
        it.setAttribute("android", dependency.isAndroid.toString())
        it.setAttribute("library", dependency.isLibrary.toString())
        it.setAttribute("partial-results-dir", dependency.partialResultsDir.absolutePathString())
        projectElement.appendChild(it)
      }
    appendModuleContents(
      document = document,
      moduleElement = dependencyElement,
      srcs = dependency.srcs,
      isTestSources = false,
      resources = dependency.resources,
      androidManifest = dependency.androidManifest,
      classpathJars = dependency.classpathJars,
      classpathAars = dependency.classpathAars,
      classpathExtractedAarDirectories = dependency.classpathExtractedAarDirectories,
      aarPartialResultsScratchDir = aarPartialResultsScratchDir,
    )
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

private fun appendModuleContents(
  document: org.w3c.dom.Document,
  moduleElement: org.w3c.dom.Element,
  srcs: List<Path>,
  isTestSources: Boolean,
  resources: List<Path>,
  androidManifest: Path?,
  classpathJars: List<Path>,
  classpathAars: List<Path>,
  classpathExtractedAarDirectories: List<Pair<Path, Path>>,
  aarPartialResultsScratchDir: Path?,
) {
  srcs.forEach { src ->
    document.createElement("src").also {
      it.setAttribute("file", src.pathString)
      if (isTestSources) {
        it.setAttribute("test", "true")
      }
      moduleElement.appendChild(it)
    }
  }

  resources.forEach { resource ->
    document.createElement("resource").also {
      it.setAttribute("file", resource.pathString)
      moduleElement.appendChild(it)
    }
  }

  if (androidManifest != null) {
    document.createElement("manifest").also {
      it.setAttribute("file", androidManifest.pathString)
      moduleElement.appendChild(it)
    }
  }

  classpathJars.forEach { jar ->
    document.createElement("classpath").also {
      it.setAttribute("jar", jar.absolutePathString())
      moduleElement.appendChild(it)
    }
  }

  classpathAars.forEach { aar ->
    document.createElement("aar").also {
      it.setAttribute("file", aar.absolutePathString())
      if (aarPartialResultsScratchDir != null) {
        it.setAttribute(
          "partial-results-dir",
          aarPartialResultsScratchDir.absolutePathString(),
        )
      }
      moduleElement.appendChild(it)
    }
  }

  classpathExtractedAarDirectories.forEach { (aar, unzippedDir) ->
    document.createElement("aar").also {
      it.setAttribute("file", aar.absolutePathString())
      it.setAttribute("extracted", unzippedDir.absolutePathString())
      if (aarPartialResultsScratchDir != null) {
        it.setAttribute(
          "partial-results-dir",
          aarPartialResultsScratchDir.absolutePathString(),
        )
      }
      moduleElement.appendChild(it)
    }
  }
}
