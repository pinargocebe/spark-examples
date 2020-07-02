package org.examples.pinar.spark

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql._
import org.apache.spark.sql.functions._

object AmountSpentByCustomerDS {

  final case class CustomerOrder(customerID: Int, itemID: Int, spent: Double)

  def main(args: Array[String]): Unit = {
    // Set the log level to only print errors
    Logger.getLogger("org").setLevel(Level.ERROR)

    // Use new SparkSession interface in Spark 2.0
    val spark = SparkSession
      .builder
      .appName("AmountSpentByCustomer")
      .master("local[*]")
      .getOrCreate()

    val lines = spark.sparkContext.textFile("data/customer-orders.csv").map(x => {
      val split = x.split(",")
      CustomerOrder(split(0).toInt, split(1).toInt, split(2).toDouble)
    })

    // Convert to a DataSet
    import spark.implicits._
    val customerOrder = lines.toDS()

    customerOrder.groupBy("customerID").sum("spent").orderBy(desc("sum(spent)")).show()

    spark.stop()
  }
}
