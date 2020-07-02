package org.examples.pinar.spark

import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext

object MinTemprature {

  def main(args: Array[String]): Unit = {
    // Set the log level to only print errors
    Logger.getLogger("org").setLevel(Level.ERROR)

    // Create a SparkContext using every core of the local machine, named RatingsCounter
    val sc = new SparkContext("local[*]", "MinTemprature")

    val lines = sc.textFile("data/1800.csv")

    def parseLine(line: String): (String, String, Float) = {
      val splitedLine = line.split(",");
      val id = splitedLine(0).toString;
      val tType = splitedLine(2).toString;
      val temprature = splitedLine(3).toFloat * 0.1f * (9.0f / 5.0f) + 32.0f;
      (id, tType, temprature)
    }

    //create map rdd in format (age,friendcount) like (33,285),(33,2),(25,154)
    val rdd = lines.map(x => parseLine(x));

    //filter only min temps
    val filteredRdd = rdd.filter(x => x._2.equals("TMIN"));
    // convert to (id, temperature)
    val idTempMap = filteredRdd.map(x => (x._1, x._3));
    //minimum temprature
    val minTempsByStation = idTempMap.reduceByKey((v1, v2) => Math.min(v1, v2))
    //collect results..
    val results = minTempsByStation.collect();

    for (result <- results.sorted) {
      val station = result._1
      val temp = result._2
      val formattedTemp = f"$temp%.2f F"
      println(s"$station minimum temperature: $formattedTemp")
    }
  }
}
