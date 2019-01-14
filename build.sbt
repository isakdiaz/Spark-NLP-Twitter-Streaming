

name := "TwitterSentiment"

version := "0.1"

scalaVersion := "2.11.8"

val sparkVersion = "2.4.0"

// https://mvnrepository.com/artifact/org.apache.spark/spark-streaming
libraryDependencies += "org.apache.spark" %% "spark-streaming" % sparkVersion


// https://mvnrepository.com/artifact/org.apache.spark/spark-streaming-kafka-0-10
libraryDependencies += "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion

// https://mvnrepository.com/artifact/org.apache.bahir/spark-streaming-twitter
libraryDependencies += "org.apache.bahir" %% "spark-streaming-twitter" % "2.3.2"

// https://mvnrepository.com/artifact/org.apache.spark/spark-core
libraryDependencies += "org.apache.spark" %% "spark-core" % sparkVersion

//Sparl MLlib
libraryDependencies += "org.apache.spark" %% "spark-mllib" % sparkVersion

//Spark-NLP (req scala 2.11)
libraryDependencies += "com.johnsnowlabs.nlp" %% "spark-nlp" % "1.8.0"

//CoreNLP Portion
libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % "3.5.1"
libraryDependencies += "edu.stanford.nlp" % "stanford-corenlp" % "3.5.1" classifier "models"




resolvers ++= Seq(
  "Akka Repository" at "http://repo.akka.io/releases/",
  "Ooyala Bintray" at "http://dl.bintray.com/ooyala/maven"
)
