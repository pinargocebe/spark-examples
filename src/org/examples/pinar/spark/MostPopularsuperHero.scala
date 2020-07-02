package org.examples.pinar.spark

import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext
import org.examples.pinar.spark.MostPopularMovie.loadMovieNames

import scala.io.{Codec, Source}

object MostPopularsuperHero {

  def loadSuperHeroNames(): Map[Int, String] = {

    implicit val codec = Codec("ISO-8859-1")

    var superHeroNames: Map[Int, String] = Map()

    val lines = Source.fromFile("data/Marvel-names.txt").getLines()
    for (line <- lines) {
      val fields = line.split("\"")
      if (fields.length > 1) {
        superHeroNames += (fields(0).trim.toInt -> fields(1))
      }
    }

    return superHeroNames
  }

  def main(args: Array[String]): Unit = {
    // Set the log level to only print errors
    Logger.getLogger("org").setLevel(Level.ERROR)

    // Create a SparkContext using every core of the local machine, named RatingsCounter
    val sc = new SparkContext("local[*]", "MostPopularSuperHero")

    val superHeroNames = sc.broadcast(loadSuperHeroNames());

    val lines = sc.textFile("data/Marvel-graph.txt")
    val idFriendsMap = lines.map(x => {
      val splittedLine = x.split(" ")
      val id = splittedLine(0).toInt
      val friends = splittedLine.size - 1
      (id, friends)
    })

    val totalFriends = idFriendsMap.reduceByKey((v1, v2) => v1 + v2)

    val flippedFriends = totalFriends.map(x => (x._2, x._1))
    val max = flippedFriends.max()
    println((superHeroNames.value(max._2), max._1))

  }
}
