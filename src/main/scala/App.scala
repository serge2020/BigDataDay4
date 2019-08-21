
object App {

  import org.apache.log4j.{Level, Logger}
  import org.apache.spark._
  import org.apache.spark.streaming._
  import org.apache.spark.SparkContext
  import org.apache.spark.sql.SparkSession



  def main(args: Array[String]): Unit = {

    Logger.getLogger("org").setLevel(Level.ERROR)
    println("Hello!")

    val conf = new SparkConf().setMaster("local[*]").setAppName("NetworkWordCount")
    val ssc = new StreamingContext(conf, Seconds(1))

    val lines = ssc.socketTextStream("localhost", 33001 )
    // api version counts
    val words = lines.flatMap(_.split(" "))
    val api = words.filter(_.startsWith("/api"))

    val apiVersion = api.map(x => x.replaceAll("[^\\d.]", ""))
      .filter(x => x.nonEmpty)
    val verCount = apiVersion.map(ver => (ver,1)).reduceByKey(_+_)

   //println("API versions:")
    verCount.print()

    // users with more than 1 IP address

    val idAndip = lines.map(x => x.replaceAll("(?:/api/\\d\\.\\d/\\w*)", ",")).filter(_.nonEmpty)
    val idAndip2 = idAndip.map(x => x.replaceAll("(?:/api/\\d\\.\\d)", ",")).filter(_.nonEmpty)

/*    val wordsStr = idAndip2.toString
    val words1 = wordsStr.substring(0, wordsStr.lastIndexOf(" "))
    val words2 = words1.map { case Array(ip, id) => id -> ip }.toMap
    val uniqueId = words2.groupBy (_._2).mapValues(_.size)
    val id2 = uniqueId.filter(_._2 > 1)
    id2.foreach(println)*/


   val idAndip3 = idAndip2.flatMap(_.split(",")).countByValue().map(x => x._1)
    val uniqueIpId = idAndip3.flatMap(_.split(" "))
    val uniqueId = uniqueIpId.countByValue().filter(_._1.length <7).filter(_._2 > 1)

    //println("Customer ID:")
    uniqueId.print()


    ssc.start()             // Start the computation
    ssc.awaitTermination()

  }
}
