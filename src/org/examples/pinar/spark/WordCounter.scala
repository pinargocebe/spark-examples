package org.examples.pinar.spark

import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext

object WordCounter {

  def main(args: Array[String]): Unit = {
    // Set the log level to only print errors
    Logger.getLogger("org").setLevel(Level.ERROR)

    // Create a SparkContext using every core of the local machine, named RatingsCounter
    val sc = new SparkContext("local[*]", "WordCounter")

    val lines = sc.textFile("data/book.txt")

    val rdd = lines.flatMap(x => x.split("\\W"));

    // to lowercase
    val lowercaseWords = rdd.map(x => x.toLowerCase())

    // count
    val wordCounts = lowercaseWords.map(x => (x, 1)).reduceByKey((x, y) => x + y)

    // Flip (word, count) tuples to (count, word) and then sort by key (the counts)
    val wordCountsSorted = wordCounts.map(x => (x._2, x._1)).sortByKey()

    val results = wordCountsSorted.collect();
    for (sortedRes <- results) {
      println(s"${sortedRes._1} times '${sortedRes._2}'")
    }
  }
}
