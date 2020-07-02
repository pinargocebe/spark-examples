package org.examples.pinar.spark

import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext

object AvgFriendsByAge {

  def main(args: Array[String]): Unit = {
    // Set the log level to only print errors
    Logger.getLogger("org").setLevel(Level.ERROR)

    // Create a SparkContext using every core of the local machine, named RatingsCounter
    val sc = new SparkContext("local[*]", "FriendsByAge")

    val lines = sc.textFile("data/fakefriends.csv")

    def parseLine(line: String): (Integer, Integer) = {
      val splitedLine = line.split(",");
      val age = splitedLine(2).toInt;
      val friendsCount = splitedLine(3).toInt;
      (age, friendsCount)
    }

    //create map rdd in format (age,friendcount) like (33,285),(33,2),(25,154)
    val rdd = lines.map(x => parseLine(x));
    //(33,(285,1)), (33,(2,1)), (25,(154,1))
    val ageFriendTupleMap = rdd.mapValues(x => (x, 1));
    //(33,(287,2)), (25,(154,1))
    val ageTotalFriendCountTupleMap = ageFriendTupleMap.reduceByKey((x, y) => (x._1 + y._1, x._2 + y._2))
    // (33, 143.5) (25, 154)
    val ageAvgFriendCount = ageTotalFriendCountTupleMap.mapValues(v => v._1 / v._2)
    //get results
    val result = ageAvgFriendCount.collect();
    //sort and print
    result.sorted.foreach(x => println(x));
  }
}
