import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {
  val appName = "camelcode"
  val appVersion = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "org.apache.camel" % "camel-core" % "2.9.1",
    "org.apache.camel" % "camel-csv" % "2.9.1",
    "org.apache.camel" % "camel-bindy" % "2.9.1",
    "org.apache.camel" % "camel-jackson" % "2.9.1",
    "org.apache.camel" % "camel-http" % "2.9.1",

    "xml-apis" % "xml-apis-xerces" % "2.7.1" from "http://repo.opengeo.org/xml-apis/xml-apis-xerces/2.7.1/xml-apis-xerces-2.7.1.jar",
    "jgridshift" % "jgridshift" % "1.0" from "http://download.osgeo.org/webdav/geotools/jgridshift/jgridshift/1.0/jgridshift-1.0.jar",

    "org.geotools" % "gt-main" % "8-SNAPSHOT" excludeAll (
      ExclusionRule(organization = "javax.media")
      ),

    "org.geotools" % "gt-epsg-hsql" % "8-SNAPSHOT" excludeAll (
      ExclusionRule(organization = "javax.media")
      ),

    // Metrics
    "com.yammer.metrics" % "metrics-core" % "2.1.0",

    // Guice
    "com.google.inject" % "guice" % "3.0",
    "com.google.inject.extensions" % "guice-assistedinject" % "3.0",
    "com.google.inject.extensions" % "guice-multibindings" % "3.0",
    "com.google.inject.extensions" % "guice-throwingproviders" % "3.0",

    // Morphia
    "com.google.code.morphia" % "morphia" % "0.99.1-SNAPSHOT", // checkout Morphia manually and execute 'mvn install'
    "com.google.code.morphia" % "morphia-logging-slf4j" % "0.99"
  )

  val pdist = TaskKey[File]("pdist", "Build the standalone application package including codepoint data")

  val pdistTask = (baseDirectory, playPackageEverything, dependencyClasspath in Runtime, target, normalizedName, version) map { (root, packaged, dependencies, target, id, version) =>

    import sbt.NameFilter._

    val dist = root / "dist"
    val packageName = id + "-" + version
    val zip = dist / (packageName + ".zip")

    IO.delete(dist)
    IO.createDirectory(dist)

    val libs = {
      dependencies.filter(_.data.ext == "jar").map { dependency =>
        dependency.data -> (packageName + "/lib/" + (dependency.metadata.get(AttributeKey[ModuleID]("module-id")).map { module =>
          module.organization + "." + module.name + "-" + module.revision + ".jar"
        }.getOrElse(dependency.data.getName)))
      } ++ packaged.map(jar => jar -> (packageName + "/lib/" + jar.getName))
    }

    val start = target / "start"

    val config = Option(System.getProperty("config.file"))

    IO.write(start,
      """#!/usr/bin/env sh

exec java $* -cp "`dirname $0`/lib/*" """ + config.map(_ => "-Dconfig.file=`dirname $0`/application.conf ").getOrElse("") + """play.core.server.NettyServer `dirname $0`
""" /* */ )
    val scripts = Seq(start -> (packageName + "/start"))

    val other = Seq((root / "README") -> (packageName + "/README"))

    val datadir = root / "codepointopen"
    val datafiles = datadir.list
    val data = datafiles.filter(_ != "done").map { location =>
      IO.copyFile(datadir / location, target / "codepointopen" / location)
      target / "codepointopen" / location -> (packageName + "/codepointopen/" +  location)
    }

    val productionConfig = target / "application.conf"

    val prodApplicationConf = config.map { location =>

      IO.copyFile(new File(location), productionConfig)
      Seq(productionConfig -> (packageName + "/application.conf"))
    }.getOrElse(Nil)

    IO.zip(libs ++ scripts ++ other ++ data ++ prodApplicationConf, zip)
    IO.delete(start)
    IO.delete(productionConfig)

    println()
    println("Your application is ready in " + zip.getCanonicalPath)
    println()

    zip
  }

  val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(
    pdist <<= pdistTask,
    lessEntryPoints <<= baseDirectory(_ ** "camelcode.less"),

    resolvers += "Local Maven Repository" at "file://" + Path.userHome + "/.m2/repository",
    resolvers += "Codehaus Repository" at "http://repository.codehaus.org/",
    resolvers += "Morphia Repository" at "http://morphia.googlecode.com/svn/mavenrepo/",
    resolvers += "Ibiblio" at "http://www.ibiblio.org/maven2/",
    resolvers += "Java.NET" at "http://download.java.net/maven/2",
    resolvers += "Open Source Geospatial Foundation Repository" at "http://download.osgeo.org/webdav/geotools/",
    resolvers += "OpenGeo Maven Repository" at "http://repo.opengeo.org"

    //SbtIdeaPlugin.defaultClassifierPolicy := true
  )
}