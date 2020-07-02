package org.examples.pinar.spark

import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext

import scala.io.{Codec, Source}

object MostPopularMovie {

  /** Load up a Map of movie IDs to movie names. */
  def loadMovieNames(): Map[Int, String] = {

    // Handle character encoding issues:
    implicit val codec = Codec("ISO-8859-1") // This is the current encoding of u.item, not UTF-8.

    // Create a Map of Ints to Strings, and populate it from u.item.
    var movieNames: Map[Int, String] = Map()

    val lines = Source.fromFile("data/ml-100k/u.item").getLines()
    for (line <- lines) {
      var fields = line.split('|')
      if (fields.length > 1) {
        movieNames += (fields(0).toInt -> fields(1))
      }
    }

    return movieNames
  }

  def main(args: Array[String]): Unit = {
    // Set the log level to only print errors
    Logger.getLogger("org").setLevel(Level.ERROR)

    // Create a SparkContext using every core of the local machine, named RatingsCounter
    val sc = new SparkContext("local[*]", "MostPopularMovie")

    var movieNames = sc.broadcast(loadMovieNames());

    val lines = sc.textFile("data/ml-100k/u.data")

    val watchedMovies = lines.map(x => (x.split("\t")(1).toInt, 1))

    val reducedWatchedMovies = watchedMovies.reduceByKey((v1, v2) => v1 + v2);

    val flippedWatchedMovies = reducedWatchedMovies.map(x => (x._2, x._1));

    val result = flippedWatchedMovies.sortByKey().collect();

    println(movieNames.value(result.max._2))
  }
}
