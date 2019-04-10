package agent

import scala.collection.mutable.ListBuffer

import org.deeplearning4j.eval.Evaluation
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.{NeuralNetConfiguration, Updater}
import org.deeplearning4j.nn.conf.layers.{DenseLayer, OutputLayer, BatchNormalization, ActivationLayer}
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator
import org.nd4j.linalg.lossfunctions.LossFunctions
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.learning.config.{Nesterovs, Adam, Nadam};
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator
import org.nd4j.linalg.dataset.api.preprocessor.{Normalizer, NormalizerStandardize}
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerSerializer

//import org.nd4s.Implicits._     // Out of date

import client._

case class TrialData(val obs: Array[Float], val acts: Array[Int]) {
  override def toString = s"${obs.toList} 2-> ${acts.toList}"
}

case class ActionData(val obs: Array[Float], val acts: Int) {
  override def toString = s"${obs.toList} 1-> ${acts}"
}

case class INNCartPole(private val games: Int = 200,
                       val goalSteps: Int = 200, // This is limit in gym V0
                       private val seed: Int = 1234,
                       private val hidden: Int = 128) {
  val id = GymJavaHttpClient.createEnv(if (goalSteps <= 200)
                                         "CartPole-v0"
                                       else
                                         "CartPole-v1")
  private val env = GymJavaHttpClient.observationSpace(id)
  var stepCnt = 0
  var gameCnt = 0

  GymJavaHttpClient.resetEnv(id)

  def randomPop(scoreReq: Int = 0): ListBuffer[TrialData] = {
    val trainData = new ListBuffer[TrialData]
    val scores = new ListBuffer[Int]
    val acceptedScores = new ListBuffer[Int]

    for (_ <- 0 until games) {
      var score: Int = 0
      val memory = new ListBuffer[ActionData]
      var prevObs: Array[Float] = Array()
      var done = false

      for (_ <- 0 until goalSteps if ! done) {
          // choose random action (0 or 1)
          val action = if (math.random > 0.5) 1 else 0
          // do it!
          val step = GymJavaHttpClient.stepDiscrete(id, action, false)

          done = step.done

          stepCnt += 1
          
          if (prevObs.length > 0)
            memory += ActionData(prevObs, action)

          prevObs = step.observation.asInstanceOf[List[Double]].map(_.floatValue).toArray
          score += step.reward.intValue
      }

      // Iff our score is higher than threshold then use 
      if (score >= scoreReq) {
        acceptedScores += score

        for (data <- memory) {
          // Use one hot excoding
          val output =
            if (data.acts == 1)
              Array(0,1)
            else
              Array(1,0)
 
          trainData += TrialData(data.obs, output)
        }
      }

      GymJavaHttpClient.resetEnv(id)

      scores += score
      gameCnt += 1
    }
    
    trainData
  }

  def dNN(seed: Int = seed, nin: Int = 4, nout: Int = 2, hidden: Int = hidden, lr: Double = 0.001f, layers: Int = 1): MultiLayerNetwork = {
    var layer = -1
    val incLayer = () => { layer += 1; layer }

    val conf = new NeuralNetConfiguration.Builder()
      .seed(seed)
      .weightInit(WeightInit.XAVIER)
      .updater(new Nesterovs(lr, 0.9))
      .list
      .layer(incLayer(), new DenseLayer.Builder()
              .hasBias(false)
              .activation(Activation.IDENTITY)
              .nIn(nin)
              .nOut(hidden)
              .build())
      .layer(incLayer(), new BatchNormalization.Builder()
              .activation(Activation.RELU)
              //.nIn(hidden)
              .nOut(hidden)
              //.dropOut(0.8)
              .build)
    var hiddenLayers = conf
      for (_ <- 0 until layers) {
        hiddenLayers = hiddenLayers
          .layer(incLayer(), new DenseLayer.Builder()
                  .hasBias(false)
                  .activation(Activation.IDENTITY)
                  .nOut(hidden)
                  .build)
          .layer(incLayer(), new BatchNormalization.Builder()
                  .activation(Activation.RELU)
                  .nOut(hidden)
                  .build)
          //.layer(incLayer(), new ActivationLayer.Builder()
          //        .activation(Activation.RELU)
          //        //.dropOut(0.8)
          //        .build())
      }
    val mlconf = hiddenLayers
//      .layer(incLayer(), new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD)
      .layer(incLayer(), new OutputLayer.Builder(LossFunction.POISSON)
              .activation(Activation.SOFTMAX)
              //.nIn(hidden)
              .nOut(nout)
              .build)
      .backprop(true)
      .pretrain(false)
      .build

    val model = new MultiLayerNetwork(mlconf)

    model.init

    //model.setListeners(new ScoreIterationListener(1)) // not worth doing!

    model
  }

  def trainModel(trainData: ListBuffer[TrialData], model: MultiLayerNetwork): Unit = {
    val l = trainData.length
    //val X = trainData.flatMap(_.obs).toArray.asNDArray(l, 4)
    //val y = trainData.flatMap(_.acts).toArray.asNDArray(l, 2)
    val X = Nd4j.create(trainData.flatMap(_.obs).toArray).reshape(l,4)
    val y = Nd4j.create(trainData.flatMap(_.acts.map(_.floatValue)).toArray).reshape(l,2)

    model.fit(X, y)
  }
}

object INNCartPole {
  def main(args: Array[String]): Unit = {
    var render = false
    var games = 1
    var steps = 500
    var seed = 253    // good for current 
    var seedInc = 0
    var hidden = 16
    var layers = 1
    var iterations = 1
    for (i <- args) // Parse parameters
      i match {
        case para if para.toLowerCase.startsWith("--render=") =>
          render = para.substring("--render=".length).toBoolean
        case para if para.toLowerCase.startsWith("--games=") =>
          games = para.substring("--games=".length).toInt
        case para if para.toLowerCase.startsWith("--steps=") =>
          steps = para.substring("--steps=".length).toInt
        case para if para.toLowerCase.startsWith("--seed=") =>
          seed = para.substring("--seed=".length).toInt
        case para if para.toLowerCase.startsWith("--seed_inc=") =>
          seedInc = para.substring("--seed_inc=".length).toInt
        case para if para.toLowerCase.startsWith("--hidden=") =>
          hidden = para.substring("--hidden=".length).toInt
        case para if para.toLowerCase.startsWith("--layers=") =>
          layers = para.substring("--layers=".length).toInt
        case para if para.toLowerCase.startsWith("--iterations=") =>
          iterations = para.substring("--iterations=".length).toInt
        case unknown => println(s"Unknown parameter: $unknown")
      }

    for (_ <- 0 until iterations) {
      runner(render, games, steps, seed, hidden, layers)

      seed += seedInc
    }
  }

  def runner(render: Boolean, games: Int, steps: Int, seed: Int, hidden: Int, layers: Int): Unit = {
    val cp = INNCartPole(games=games, goalSteps=steps)
    val model = cp.dNN(seed=seed, hidden=hidden, layers=layers)

    for (i <- 0 until 1) {
      val trainData = cp.randomPop()

      cp.trainModel(trainData, model)
    }

    val scores = new ListBuffer[Int]
    val choices = new ListBuffer[Int]

    for (i <- 0 until 2) {
      var score = 0
      val memory = new ListBuffer[ActionData]
      var prevObs: Array[Float] = Array()
      var action = if (math.random > 0.5) 1 else 0
      var done = false

      GymJavaHttpClient.resetEnv(cp.id)
      if (render)
        println(s"New game: $i")

      for (_ <- 0 until cp.goalSteps if ! done) {

        if (prevObs.length > 0) {
          //action = model.output(prevObs.asNDArray(1, 4)).argMax(1)(0).intValue
          //action = model.predict(prevObs.asNDArray(1, 4))(0)
//println(Nd4j(prevObs.asNDArray(1, 4) + " --> " + action)
          action = model.predict(Nd4j.create(prevObs).reshape(1,4))(0)

          choices += action
        }
                
        val step = GymJavaHttpClient.stepDiscrete(cp.id, action, render)

        prevObs = step.observation.asInstanceOf[List[Double]].map(_.floatValue).toArray
        memory += ActionData(prevObs, action)
        score += step.reward.intValue
        done = step.done
      }

      scores += score
    }

    println(s"Score: ${scores.sum / scores.length} trained with ${cp.stepCnt} steps ${cp.gameCnt} games seed=$seed:- ${choices.length} : ${choices.filter(_ == 0).length} 0s and ${choices.filter(_ == 1).length} 1s")

    if (render)
      GymJavaHttpClient.closeMonitor(cp.id)
  }
}
