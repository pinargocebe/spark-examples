package org.examples.pinar.spark

import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext
import org.examples.pinar.spark.MostPopularMovie.loadMovieNames

import scala.io.{Codec, Source}

object SimilarMovies {

  def main(args: Array[String]): Unit = {
    Logger.getLogger("org").setLevel(Level.ERROR)

    val sc = new SparkContext("local[*]", "SimilarMovies")

    val movieNames = sc.broadcast(loadMovieNames());

    val lines = sc.textFile("data/ml-100k/u.data")

    val userMovieRatingMap = lines.map(x => {
      val splittedLine = x.split("\t")
      val userID = splittedLine(0).toInt
      val movieID = splittedLine(1).toInt
      val rating = splittedLine(2).toDouble
      val ratingData: RatingData = (movieID, rating)
      (userID, ratingData)
    })

    val joinedRatings = userMovieRatingMap.join(userMovieRatingMap)

    val filteredRatings = joinedRatings.filter(filterMovies)

    val moviePairs = filteredRatings.map(makePairs)

    val moviePairRatings = moviePairs.groupByKey()

    val moviePairSimilarities = moviePairRatings.mapValues(computeCosineSimilarity).cache()

    // Extract similarities for the movie we care about that are "good".

    if (args.length > 0) {
      val scoreThreshold = 0.97
      val coOccurenceThreshold = 50.0

      val movieID: Int = args(0).toInt

      // Filter for movies with this sim that are "good" as defined by
      // our quality thresholds above

      val filteredResults = moviePairSimilarities.filter(x => {
        val pair = x._1
        val sim = x._2
        (pair._1 == movieID || pair._2 == movieID) && sim._1 > scoreThreshold && sim._2 > coOccurenceThreshold
      }
      )

      // Sort by quality score.
      val results = filteredResults.map(x => (x._2, x._1)).sortByKey(false).take(10)

      println("\nTop 10 similar movies for " + movieNames.value(movieID))
      for (result <- results) {
        val sim = result._1
        val pair = result._2
        // Display the similarity result that isn't the movie we're looking at
        var similarMovieID = pair._1
        if (similarMovieID == movieID) {
          similarMovieID = pair._2
        }
        println(movieNames.value(similarMovieID) + "\tscore: " + sim._1 + "\tstrength: " + sim._2)
      }
    }
  }

  type RatingPair = (Double, Double)
  type RatingPairs = Iterable[RatingPair]

  def computeCosineSimilarity(ratingPairs: RatingPairs): (Double, Int) = {
    var numPairs: Int = 0
    var sum_xx: Double = 0.0
    var sum_yy: Double = 0.0
    var sum_xy: Double = 0.0

    for (pair <- ratingPairs) {
      val ratingX = pair._1
      val ratingY = pair._2

      sum_xx += ratingX * ratingX
      sum_yy += ratingY * ratingY
      sum_xy += ratingX * ratingY
      numPairs += 1
    }

    val numerator: Double = sum_xy
    val denominator = Math.sqrt(sum_xx) * Math.sqrt(sum_yy)

    var score: Double = 0.0
    if (denominator != 0) {
      score = numerator / denominator
    }

    return (score, numPairs)
  }

  private def filterMovies(userRatings: UserRatingPair) = {
    val moviePair = userRatings._2
    val m1Id = moviePair._1._1
    val m2Id = moviePair._2._1
    m1Id < m2Id
  }

  private def makePairs(userRatings: UserRatingPair) = {
    val moviePair = userRatings._2
    val m1Id = moviePair._1._1
    val m2Id = moviePair._2._1
    val m1Rating = moviePair._1._2
    val m2Rating = moviePair._2._2
    ((m1Id, m2Id), (m1Rating, m2Rating))
  }

  type RatingData = (Int, Double)
  type UserRatingPair = (Int, (RatingData, RatingData))

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
}
