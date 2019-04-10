package agent

import scala.util.Random

import scala.collection.mutable.ListBuffer

import vegas._
import vegas.render.WindowRenderer._

import client._

object RandomCartPole {
  def main(args: Array[String]): Unit = {
    GymJavaHttpClient.baseUrl = "http://127.0.0.1:5000"

    val id = GymJavaHttpClient.createEnv("CartPole-v0")
    val actionSpace = GymJavaHttpClient.actionSpace(id)
    val numActions = GymJavaHttpClient.actionSpaceSize(actionSpace)
    val seed = 1234
    val rng = new Random(seed)
    val games = 500
    val all = new ListBuffer[Int]

    for (game <- 0 until games) {

      var state = GymJavaHttpClient.resetEnv(id)
      var done = false
      var steps = 0
      
      while (! done) {
        steps += 1

        //val actionSpace = GymJavaHttpClient.actionSpace(id)
        val action = rng.nextInt(numActions)
        val step = GymJavaHttpClient.stepDiscrete(id, action, false)

        done = step.done

        if (done) {
          all += steps
          println(s"Game $game lasted $steps steps")
        }
      }
    }

    println(s"Average number of steps: ${all.sum / games} in $games games with total of ${all.sum} steps")
    plotRewards(all.toList)
    plotRunningAvg(all.toList)

    GymJavaHttpClient.closeMonitor(id)
  }

  def plotRewards(rs: List[Int]): Unit = {
    plot(rs, "Rewards")
  }

  def plotRunningAvg(rs: List[Int]): Unit = {
    val avg = new ListBuffer[Int]
    for (i <- 0 until rs.length) 
      avg += rs.slice(math.max(0, i-100), i + 1).sum / rs.length

    plot(avg.toList, "Running Average")
  }

  private def plot(rs: List[Int], title: String): Unit = {
    val plot = Vegas(title).
      withData(rs.zipWithIndex.map(i => Map("episode" -> i._1, "iteration" -> i._2))).
      encodeX("iteration", Quant).
      encodeY("episode", Quant).
      mark(Line)
      .encodeSize(value=401L)

    plot.show
  }
}
