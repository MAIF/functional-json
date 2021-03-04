import Dependencies._
import ReleaseTransformations._
import sbt.librarymanagement.MavenRepository

organization := "fr.maif"

name := "functional-json"

scalaVersion := "2.12.12"

lazy val root = (project in file("."))

val res = Seq(
  "jitpack.io" at "https://jitpack.io"
)

/* Début code temporaire lié au changement d'url sonatype */
val sonatypeStaging = MavenRepository(
  "sonatype-staging",
  "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2"
)
val sonatypeSnapshots = MavenRepository(
  "sonatype-snapshots",
  "https://s01.oss.sonatype.org/content/repositories/snapshots"
)

sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypePublishToBundle := {
  if (version.value.endsWith("-SNAPSHOT")) {
    Some(sonatypeSnapshots)
  } else {
    Some(Resolver.file("sonatype-local-bundle", sonatypeBundleDirectory.value))
  }
}

sonatypeDefaultResolver := {
  val profileM = sonatypeTargetRepositoryProfile.?.value
  val staged = profileM.map { stagingRepoProfile =>
    "releases" at stagingRepoProfile.deployUrl
  }
  staged.getOrElse(if (version.value.endsWith("-SNAPSHOT")) {
    sonatypeSnapshots
  } else {
    sonatypeStaging
  })
}
/* Fin code temporaire lié au changement d'url sonatype */

resolvers ++= res

libraryDependencies ++= Seq(
  "io.vavr" % "vavr" % vavrVersion,
  "io.vavr" % "vavr-jackson" % vavrVersion,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion,
  "org.projectlombok" % "lombok" % "1.18.4",
  "com.novocode" % "junit-interface" % "0.11" % Test,
  "org.assertj" % "assertj-core" % "3.10.0" % Test,
  "com.github.everit-org.json-schema" % "org.everit.json.schema" % "1.12.1" % Test
)

val javaVersion = "8"

javacOptions ++= Seq("-source", javaVersion, "-target", javaVersion)
javacOptions in (Compile, compile) ++= Seq(
  "-target",
  javaVersion,
  "-Xlint:unchecked"
)
// Skip the javadoc for the moment
sources in (Compile, doc) := Seq.empty

testFrameworks := Seq(TestFrameworks.JUnit)
testOptions += Tests.Argument(TestFrameworks.JUnit, "-v")

(parallelExecution in Test) := false

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion,
  pushChanges
)

lazy val githubRepo = "maif/functional-json"

inThisBuild(
  List(
    organization := "fr.maif",
    homepage := Some(url(s"https://github.com/$githubRepo")),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        "alexandre.delegue",
        "Alexandre Delègue",
        "",
        url(s"https://github.com/larousso")
      ),
      Developer(
        "benjamin.cavy",
        "Benjamin Cavy",
        "",
        url(s"https://github.com/ptitFicus")
      ),
      Developer(
        "gregory.bevan",
        "Grégory Bévan",
        "",
        url(s"https://github.com/GregoryBevan")
      ),
      Developer(
        "georges.ginon",
        "Georges Ginon",
        "",
        url(s"https://github.com/ftoumHub")
      )
    ),
    publishMavenStyle := true,
    releaseCrossBuild := true
  )
)
