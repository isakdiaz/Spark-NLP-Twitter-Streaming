import java.util.Properties

import org.apache.spark.{SparkConf, SparkContext}
import twitter4j.conf.ConfigurationBuilder
import twitter4j.auth.OAuthAuthorization
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{StreamingContext, Seconds}

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import twitter4j.Status


object TweetStream {
  def main(args: Array[String]): Unit = {

    val appName = "TwitterData"
    //create context
    val conf = new SparkConf().setAppName(appName).setMaster("local[2]")
    val ssc = new StreamingContext(conf, Seconds(10))
//
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

//    //Connection to Twitter API
    val cb = new ConfigurationBuilder
    cb.setDebugEnabled(true).setOAuthConsumerKey(consumerKey).setOAuthConsumerSecret(consumerSecret).setOAuthAccessToken(accessToken).setOAuthAccessTokenSecret(accessTokenSecret)
//
    val auth = new OAuthAuthorization(cb.build)
    val tweets = TwitterUtils.createStream(ssc, Some(auth))
    val englishTweets = tweets.filter(_.getLang() == "en")
    val statuses = englishTweets.map(status => (status.getText(),status.getUser.getName(),status.getUser.getScreenName(),status.getCreatedAt.toString))


    statuses.foreachRDD { (rdd, time) =>

      rdd.foreachPartition { partitionIter =>
        val props = new Properties()
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        props.put("bootstrap.servers", bootstrap_server)
        val producer = new KafkaProducer[String, String](props)
        partitionIter.foreach { elem =>
          val dat = elem.toString()
          val data = new ProducerRecord[String, String]("new_topic", null, dat) // "llamada" is the name of Kafka topic
          producer.send(data)
        }
        producer.flush()
        producer.close()
      }
    }
    ssc.start()
    ssc.awaitTermination()

  }
}
