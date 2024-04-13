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

internal fun createProjectXMLString(
  moduleName: String,
  rootDir: String,
  srcs: List<Path>,
  resources: List<Path>,
  androidManifest: Path?,
  classpathJars: List<Path>,
  classpathAars: List<Path>,
  classpathExtractedAarDirectories: List<Pair<Path, Path>>,
  customLintChecks: List<Path>,
): String {
  val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()

  val projectElement = document.createElement("project")
  document.appendChild(projectElement)

  document.createElement("root").also {
    it.setAttribute("dir", rootDir)
    projectElement.appendChild(it)
  }

  val moduleElement = document.createElement("module").also {
    it.setAttribute("name", moduleName)
    it.setAttribute("android", if (androidManifest != null) "true" else "false")
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
    moduleElement.appendChild(element)
  }

  classpathExtractedAarDirectories.forEach { (aar, unzippedDir) ->
    val element = document.createElement("aar")
    element.setAttribute("file", aar.absolutePathString())
    element.setAttribute("extracted", unzippedDir.absolutePathString())
    moduleElement.appendChild(element)
  }

  return StringWriter().apply {
    val transformer = TransformerFactory.newInstance().newTransformer()
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
    transformer.transform(DOMSource(document), StreamResult(this))
  }.buffer.toString()
}
