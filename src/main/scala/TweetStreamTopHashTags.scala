import java.util.Properties
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import twitter4j.auth.OAuthAuthorization
import twitter4j.conf.ConfigurationBuilder
//import com.johnsnowlabs.nlp.pretrained.pipelines.en.SentimentPipeline


object TweetStreamTopHashTags {

  def main(args: Array[String]): Unit = {

    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)
    Logger.getLogger("twitter4j").setLevel(Level.OFF)


    // values of Twitter API.
    //load properties from file
    var (bootstrap_server, consumerKey, consumerSecret, accessToken, accessTokenSecret) =
    try{
      val properties = new Properties()
      properties.load(getClass.getResourceAsStream("application.properties"))
      (
        properties.getProperty("bootstrap_server"),
        properties.getProperty("twitter4j.oauth.consumerKey"),
        properties.getProperty("twitter4j.oauth.consumerSecret"),
        properties.getProperty("twitter4j.oauth.accessToken"),
        properties.getProperty("twitter4j.oauth.accessTokenSecret")
      )
    }catch {case e: Exception =>
      e.printStackTrace()
      sys.exit(1)
    }


    val appName = "TwitterData"

    val spark  = SparkSession
      .builder()
      .appName(appName)
      .master("local[*]")
      .getOrCreate()
    val sc = spark.sparkContext
    val ssc = new StreamingContext(sc, Seconds(1))

    //Count number of hash tags
    var accum = sc.longAccumulator("Running Total")

    //    //Connection to Twitter API
    val cb = new ConfigurationBuilder
    cb.setDebugEnabled(true).setOAuthConsumerKey(consumerKey).setOAuthConsumerSecret(consumerSecret).setOAuthAccessToken(accessToken).setOAuthAccessTokenSecret(accessTokenSecret)
    //
    val auth = new OAuthAuthorization(cb.build)
    val tweets = TwitterUtils.createStream(ssc, Some(auth))
    val englishTweets = tweets.filter(_.getLang == "en")
    val tweetWords = englishTweets.flatMap(_.getText.split(" "))
    val statuses = tweetWords.filter(_.contains("#")).map(hash =>
      (hash, accum.value)
    )



    statuses.foreachRDD { (rdd, time) =>

      rdd.foreachPartition { partitionIter =>

        val props = new Properties()
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        props.put("bootstrap.servers",  bootstrap_server)
        val producer = new KafkaProducer[String, String](props)
        partitionIter.foreach { elem =>
          accum.add(1)
          val dat = elem.toString()
          val data = new ProducerRecord[String, String]("new_topic", null, dat)
          producer.send(data)
//          println(dat)

        }


        producer.flush()
        producer.close()
      }
    }
    ssc.start()
    ssc.awaitTermination()

  }
}
