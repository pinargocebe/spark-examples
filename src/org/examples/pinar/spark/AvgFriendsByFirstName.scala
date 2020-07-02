package org.examples.pinar.spark

import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext

object AvgFriendsByFirstName {

  def main(args: Array[String]): Unit = {
    // Set the log level to only print errors
    Logger.getLogger("org").setLevel(Level.ERROR)

    // Create a SparkContext using every core of the local machine, named RatingsCounter
    val sc = new SparkContext("local[*]", "FriendsByAge")

    val lines = sc.textFile("data/fakefriends.csv")

    def parseLine(line: String): (String, Integer) = {
      val splitedLine = line.split(",");
      val name = splitedLine(1).toString;
      val friendsCount = splitedLine(3).toInt;
      (name, friendsCount)
    }

    //create map rdd in format (age,friendcount) like (a,285),(a,2),(b,154)
    val rdd = lines.map(x => parseLine(x));
    //(a,(285,1)), (a,(2,1)), (b,(154,1))
    val nameFriendTupleMap = rdd.mapValues(x => (x, 1));
    //(a,(287,2)), (b,(154,1))
    val nameTotalFriendCountTupleMap = nameFriendTupleMap.reduceByKey((x, y) => (x._1 + y._1, x._2 + y._2))
    // (a, 143.5) (b, 154)
    val nameAvgFriendCount = nameTotalFriendCountTupleMap.mapValues(v => v._1 / v._2)
    //get results
    val result = nameAvgFriendCount.collect();
    //sort and print
    result.sorted.foreach(x => println(x));
  }
}
