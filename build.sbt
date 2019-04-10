scalaVersion := "2.11.12"

libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.8"
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.8"
val nd4jVersion = "1.0.0-beta3"

//libraryDependencies += "org.jfree" % "jfreechart" % "1.0.14"
//libraryDependencies += "org.nd4j" %% "nd4s" % nd4jVersion // no latest port
libraryDependencies += "org.nd4j" % "nd4j-native-platform" % nd4jVersion
//libraryDependencies += "org.nd4j" % "nd4j-cuda-10.0" % nd4jVersion
libraryDependencies += "org.deeplearning4j" % "deeplearning4j-core" % nd4jVersion
libraryDependencies += "org.vegas-viz" %% "vegas" % "0.3.11"
libraryDependencies ++= Seq("com.twelvemonkeys.imageio" % "imageio-core" % "3.4.1", "com.twelvemonkeys.imageio" % "imageio-tiff" % "3.4.1")

// Required for assembly only
assemblyMergeStrategy in assembly := {
  case PathList("org", "bytedeco", xs @ _*) => MergeStrategy.last
  case PathList("org", "jetbrains", xs @ _*) => MergeStrategy.last
  case PathList("org", "deeplearning4j", xs @ _*) => MergeStrategy.last
  case PathList("org", "nd4j", xs @ _*) => MergeStrategy.last
  case "about.html" => MergeStrategy.rename
  case "META-INF/ECLIPSEF.RSA" => MergeStrategy.last
  case "META-INF/mailcap" => MergeStrategy.last
  case "META-INF/mimetypes.default" => MergeStrategy.last
  case "plugin.properties" => MergeStrategy.last
  case "log4j.properties" => MergeStrategy.last
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
