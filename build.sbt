import Dependencies._
import ReleaseTransformations._
import sbt.librarymanagement.MavenRepository

organization := "fr.maif"

name := "functional-json"

scalaVersion := "2.12.12"

lazy val root = (project in file("."))

usePgpKeyHex("ACB29F776DF78DC275FD53D701A8C4DED9143455")

sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
sonatypeCredentialHost := "s01.oss.sonatype.org"
resolvers ++= Seq(
  "jitpack.io" at "https://jitpack.io"
)

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
    releaseCrossBuild := false,
    crossPaths := false
  )
)
