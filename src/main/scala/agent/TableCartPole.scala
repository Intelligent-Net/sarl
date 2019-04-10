package agent

import scala.collection.mutable.ListBuffer

import client._

case class TableCartPole(private val buckets: Array[Int] = Array(1, 1, 6, 12),
                         private val nEpisodes: Int = 1000,
                         private val nWinTicks: Int = 195,
                         private val minAlpha: Double = 0.1,
                         private val minEpsilon: Double = 0.1,
                         private val gamma: Double = 1.0,
                         private val adaDivisor: Int = 25,
                         private val quiet: Boolean = false) {

  GymJavaHttpClient.baseUrl = "http://127.0.0.1:5000"

  private val id = GymJavaHttpClient.createEnv("CartPole-v0")
  private val actionSpace = GymJavaHttpClient.actionSpace(id)
  private val numActions = GymJavaHttpClient.actionSpaceSize(actionSpace)
  private val obsEnv = GymJavaHttpClient.observationSpace(id)
  private val numObs = GymJavaHttpClient.observationSpaceSize(obsEnv)

  private val Q = scala.collection.mutable.Map[(Int,Int,Int,Int), Array[Double]]().withDefault(_ => Array(0.0,0.0))

  private val high = obsEnv("high").asInstanceOf[List[Double]]
  private val low = obsEnv("low").asInstanceOf[List[Double]]
  private val ubs = List(high(0), 0.5, high(2), math.toRadians(50))
  private val lbs = List(low(0), -0.5, low(2), math.toRadians(-50))

  private var stepCnt = 0

  def discretize(obs: List[Double]): (Int,Int,Int,Int) = {
    val ratios = (obs,lbs,ubs).zipped
                              .map{case(o,l,u) => (o + math.abs(l)) / (u - l)}
    val acts = ratios.zip(buckets).map{case(r,b) => (math.round((b - 1) * r),b)}
                                  .map{case(o,b) => math.min(b - 1, math.max(0, o))}
    acts match {
      case List(a,b,c,d) => (a.intValue,b.intValue,c.intValue,d.intValue)
    }
  }

  def chooseAction(state: (Int,Int,Int,Int), eps: Double): Int = {
    if (math.random <= eps)
      // GymJavaHttpClient.sample(id)
      if (math.random > 0.5) 1 else 0 // Faster!
    else {
      Q(state).indexOf(Q(state).max)
    }
  }

  def updateQ(stateOld: (Int,Int,Int,Int), action: Int, reward: Double, stateNew: (Int,Int,Int,Int), alpha: Double): Unit = {
    val qi = Q(stateOld)

    qi(action) += alpha * (reward + gamma * Q(stateNew).max - qi(action))

    Q(stateOld) = qi
  }

  def getEpsilon(t: Double): Double =
    math.max(minEpsilon, math.min(1.0, 1.0 - math.log10((t + 1) / adaDivisor)))

  def getAlpha(t: Double): Double =
    math.max(minAlpha, math.min(1.0, 1.0 - math.log10((t + 1) / adaDivisor)))

  def run: Unit = {
    val scores = new ListBuffer[Int]

    for (e <- 0 until nEpisodes) {
      var currentState = discretize(GymJavaHttpClient.resetEnv(id))

      val alpha = getAlpha(e)
      val epsilon = getEpsilon(e)
      var done = false
      var i = 0

      while (! done) {
        val action = chooseAction(currentState, epsilon)
        val step = GymJavaHttpClient.stepDiscrete(id, action, false)
        val newState = discretize(step.observation.asInstanceOf[List[Double]])

        stepCnt += 1

        updateQ(currentState, action, step.reward, newState, alpha)

        currentState = newState

        done = step.done

        i += 1
      }

      scores += i

      val meanScore = scores.takeRight(100).sum / scores.takeRight(100).length

      if (meanScore >= nWinTicks && e >= 100) {
        if (! quiet) {
          println(s"Ran $e episodes. Solved after ${e - 100} trials - ${Q.size}, Steps: $stepCnt")

          return
        }
      }

      if (e % 100 == 0 && ! quiet)
        println(s"[Episode $e] - Mean survival time over last 100 episodes was $meanScore, Steps: $stepCnt")
    }

    if (! quiet)
      println(s"Did not solve after $nEpisodes episodes")
  }
}

object TableCartPole {
  def main(args: Array[String]): Unit = {
    TableCartPole(Array(1, 1, 6, 12)).run
  }
}
