ThisBuild / tlBaseVersion := "0.15"

ThisBuild / organization     := "dev.scalafreaks"
ThisBuild / organizationName := "ScalaFreaks"
ThisBuild / startYear        := Some(2024)
ThisBuild / licenses         := Seq(License.Apache2)
ThisBuild / developers       := List(tlGitHubDev("aartigao", "Alan Artigao"))

ThisBuild / tlFatalWarnings := true
ThisBuild / tlJdkRelease    := Some(11)

val Scala2 = "2.13.15"
val Scala3 = "3.3.4"

ThisBuild / scalaVersion       := Scala2
ThisBuild / crossScalaVersions := Seq(Scala2, Scala3)

ThisBuild / tlVersionIntroduced := Map("3" -> "0.12.0")

ThisBuild / tlCiReleaseBranches := Seq.empty

ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("11"))

ThisBuild / githubWorkflowAddedJobs += {
  val jobSetup = (ThisBuild / githubWorkflowJobSetup).value.toList
  val coverageAggregate =
    WorkflowStep.Sbt(
      List("coverage", "test", "coverageAggregate"),
      name = Some("Coverage Aggregate")
    )
  val codecovPublish =
    WorkflowStep.Use(
      name = Some("Publish Aggregated Coverage"),
      ref = UseRef.Public("codecov", "codecov-action", "v4"),
      params = Map(
        "token"            -> "${{ secrets.CODECOV_TOKEN }}",
        "file"             -> "./target/scala-2.13/scoverage-report/scoverage.xml",
        "flags"            -> "unittests",
        "codecov_yml_path" -> "./.codecov.yml"
      )
    )
  WorkflowJob(
    "codecov",
    "Codecov Publish",
    jobSetup :+ coverageAggregate :+ codecovPublish,
    cond = Some("github.event_name != 'push'")
  )
}

ThisBuild / githubWorkflowPublishPostamble := Seq(
  WorkflowStep.Sbt(
    List("docs / mdoc"),
    name = Some("Generate repository documentation")
  ),
  WorkflowStep.Run(
    List(
      "git config user.email 'alanartigao@gmail.com'",
      "git config user.name 'aartigao'",
      "git add README.md",
      "git diff --quiet && git diff --staged --quiet || git commit --cleanup=verbatim -m $'Update documentation\\n\\n\\nskip-checks: true'"
    ),
    name = Some("Publish repository documentation")
  )
)

ThisBuild / githubWorkflowTargetBranches := Seq("*")

lazy val versions = new {

  val cats = "2.12.0"

  val catsEffect = "3.5.7"

  val catsMtl = "1.5.0"

  val disruptor = "4.0.0"

  val jsoniter = "2.33.1"

  val log4j = "2.24.3"

  val magnoliaScala2 = "1.1.10"

  val magnoliaScala3 = "1.3.8"

  val perfolation = "1.2.11"

  val scalaCheck = "1.18.1"

  val scalaTest = "3.2.19"

  val scalaTestScalaCheck = "3.2.14.0"

  val scribe = "3.16.0"

  val slf4j = "2.0.16"

  val slf4j1 = "1.7.36"

  val sourcecode = "0.4.2"

  val zio = "1.0.18"

  val zioCats = "13.0.0.2"

}

lazy val scalaTest           = "org.scalatest"     %% "scalatest"       % versions.scalaTest           % Test
lazy val scalaTestScalaCheck = "org.scalatestplus" %% "scalacheck-1-16" % versions.scalaTestScalaCheck % Test

lazy val alleycats = "org.typelevel" %% "alleycats-core" % versions.cats

lazy val cats = List(
  (version: String) => "org.typelevel" %% "cats-core" % version,
  (version: String) => "org.typelevel" %% "cats-laws" % version % Test
).map(_.apply(versions.cats))

lazy val catsEffect        = "org.typelevel" %% "cats-effect"         % versions.catsEffect
lazy val catsEffectStd     = "org.typelevel" %% "cats-effect-std"     % versions.catsEffect
lazy val catsEffectTestkit = "org.typelevel" %% "cats-effect-testkit" % versions.catsEffect % Test

lazy val catsMtl = "org.typelevel" %% "cats-mtl" % versions.catsMtl

lazy val sourcecode = "com.lihaoyi" %% "sourcecode" % versions.sourcecode

lazy val scalaCheck = "org.scalacheck" %% "scalacheck" % versions.scalaCheck % Test

lazy val magnoliaScala2 = "com.softwaremill.magnolia1_2" %% "magnolia" % versions.magnoliaScala2
lazy val magnoliaScala3 = "com.softwaremill.magnolia1_3" %% "magnolia" % versions.magnoliaScala3

lazy val perfolation = "com.outr" %% "perfolation" % versions.perfolation

lazy val slf4j  = "org.slf4j" % "slf4j-api" % versions.slf4j
lazy val slf4j1 = "org.slf4j" % "slf4j-api" % versions.slf4j1

lazy val log4j = ("com.lmax" % "disruptor" % versions.disruptor) :: List(
  "org.apache.logging.log4j" % "log4j-api",
  "org.apache.logging.log4j" % "log4j-core"
).map(_ % versions.log4j)

lazy val scribe = List(
  "com.outr" %% "scribe"      % versions.scribe,
  "com.outr" %% "scribe-file" % versions.scribe
)

lazy val jsoniter = List(
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core"   % versions.jsoniter,
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % versions.jsoniter % "compile-internal"
)

lazy val sharedSettings = Seq(
  libraryDependencies ++= scalaTestScalaCheck :: scalaCheck :: scalaTest :: Nil
)

lazy val `odin-core` = (project in file("core"))
  .settings(sharedSettings)
  .settings(
    libraryDependencies ++= (catsEffect % Test) :: catsEffectTestkit :: catsMtl :: sourcecode :: perfolation :: catsEffectStd :: alleycats :: cats
  )

lazy val `odin-json` = (project in file("json"))
  .settings(sharedSettings)
  .settings(
    libraryDependencies ++= jsoniter
  )
  .dependsOn(`odin-core` % "compile->compile;test->test")

lazy val `odin-zio` = (project in file("zio"))
  .settings(sharedSettings)
  .settings(
    libraryDependencies ++= Seq(
      catsEffect,
      "dev.zio" %% "zio"              % versions.zio,
      "dev.zio" %% "zio-interop-cats" % versions.zioCats
    )
  )
  .dependsOn(`odin-core` % "compile->compile;test->test")

lazy val `odin-slf4j` = (project in file("slf4j"))
  .settings(sharedSettings)
  .settings(
    libraryDependencies += slf4j
  )
  .dependsOn(`odin-core` % "compile->compile;test->test")

lazy val `odin-slf4j-provider` = (project in file("slf4j-provider"))
  .settings(sharedSettings)
  .settings(
    libraryDependencies += slf4j
  )
  .dependsOn(`odin-core` % "compile->compile;test->test")

lazy val `odin-slf4j1-provider` = (project in file("slf4j1-provider"))
  .settings(sharedSettings)
  .settings(
    libraryDependencies += slf4j1
  )
  .dependsOn(`odin-core` % "compile->compile;test->test")

lazy val `odin-extras` = (project in file("extras"))
  .settings(sharedSettings)
  .settings(
    libraryDependencies ++= {
      if (tlIsScala3.value) List(magnoliaScala3)
      else
        List(
          magnoliaScala2,
          // only in provided scope so that users of extras not relying on magnolia don't get it on their classpaths
          // see extras section In Readme
          "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided"
        )
    }
  )
  .dependsOn(`odin-core` % "compile->compile;test->test")

lazy val benchmarks = (project in file("benchmarks"))
  .enablePlugins(JmhPlugin, NoPublishPlugin)
  .settings(sharedSettings)
  .settings(
    javacOptions         -= "-Xlint:all",
    libraryDependencies ++= catsEffect :: scribe ::: log4j
  )
  .dependsOn(`odin-core`, `odin-json`)

lazy val docs = (project in file("odin-docs"))
  .enablePlugins(MdocPlugin, NoPublishPlugin)
  .dependsOn(`odin-core`, `odin-json`, `odin-zio`, `odin-slf4j`, `odin-extras`)
  .settings(sharedSettings)
  .settings(
    mdocVariables       := Map("VERSION" -> version.value),
    mdocOut             := file("."),
    libraryDependencies += catsEffect
  )

lazy val examples = (project in file("examples"))
  .enablePlugins(NoPublishPlugin)
  .dependsOn(`odin-core` % "compile->compile;test->test", `odin-zio`)
  .settings(sharedSettings)
  .settings(
    coverageExcludedPackages := "io.odin.examples.*",
    libraryDependencies      += catsEffect
  )

lazy val odin =
  tlCrossRootProject.aggregate(
    `odin-core`,
    `odin-json`,
    `odin-zio`,
    `odin-slf4j`,
    `odin-slf4j-provider`,
    `odin-slf4j1-provider`,
    `odin-extras`,
    benchmarks,
    examples
  )
