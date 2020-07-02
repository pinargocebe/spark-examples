package org.examples.pinar.spark

import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext

object AmountSpentByCustomer {
  def main(args: Array[String]): Unit = {
    // Set the log level to only print errors
    Logger.getLogger("org").setLevel(Level.ERROR)

    // Create a SparkContext using every core of the local machine, named RatingsCounter
    val sc = new SparkContext("local[*]", "AmountSpentByCustomer")

    val lines = sc.textFile("data/customer-orders.csv")

    def parseLine(line: String): (Int, Double) = {
      val splitedLine = line.split(",");
      val customerId = splitedLine(0).toInt;
      val itemId = splitedLine(1).toInt;
      val spent = splitedLine(2).toDouble;
      (customerId, spent)
    }

    //create map rdd in format (customerId,spentAmount) like (33,285.2)
    val rdd = lines.map(x => parseLine(x));

    val totalSpentByCustomer = rdd.reduceByKey((v1, v2) => v1 + v2);

    val totalSpentByCustomerFlipped = totalSpentByCustomer.map(x => (x._2, x._1))

    val results = totalSpentByCustomerFlipped.collect();

    for (elem <- results.sorted) {
      println(s"Customer ${elem._2} spent ${elem._1} in total.")
    }
  }
}
