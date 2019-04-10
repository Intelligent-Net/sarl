package client

import java.io.{InputStreamReader, BufferedReader, OutputStreamWriter}
import java.net.{HttpURLConnection, URL}
import scala.util.control.NonFatal

object GymJavaHttpClient {

  var baseUrl = "http://127.0.0.1:5000"

  def listEnvs(): Set[String] = {
    val con = connect("/v1/envs/", "GET", null)

    Json.toMap(getJson(con))("all_envs").asInstanceOf[Map[String,Any]].keySet
  }

  def createEnv(envId: String): String = {
    val con = connect("/v1/envs/",
                      "POST",
                      s"""{"env_id":"$envId"}""")

    Json.toMap(getJson(con))("instance_id").asInstanceOf[String]
  }

  def resetEnvInt(id: String): Integer = {
    val con = connect(s"/v1/envs/$id/reset/",
                      "POST",
                      s"""{"instance_id":"$id"}""")

    val ob = Json.toMap(getJson(con))("observation")

    /*
    ob match {
      case _: Integer => List(ob.asInstanceOf[Integer].doubleValue)
      case _: Double => List(ob.asInstanceOf[Double])
      case _ => ob.asInstanceOf[List[Double]]
    }
    */
    ob.asInstanceOf[Integer]
  }

  def resetEnv(id: String): List[Double] = {
    val con = connect(s"/v1/envs/$id/reset/",
                      "POST",
                      s"""{"instance_id":"$id"}""")

    Json.toMap(getJson(con))("observation").asInstanceOf[List[Double]]
  }

  def stepDiscrete(id: String, action: Int, render: Boolean = false): Step = {
    stepInternal(id, action, render)
  }

  def step(id: String, action: Double, render: Boolean = false): Step = {
    stepInternal(id, action, render)
  }

  private def stepInternal(id: String, action: Any, render: Boolean = false): Step = {
    val con = connect(s"/v1/envs/$id/step/",
                      "POST",
                      s"""{"instance_id":"$id", "action":$action, "render":$render}""")

    Json.toStep(getJson(con))
  }

  def actionSpace(id: String): Map[String,Any] = {
    val con = connect(s"/v1/envs/$id/action_space/",
                      "GET",
                      s"""{"instance_id":"$id"}""")

    Json.toMap(getJson(con))("info").asInstanceOf[Map[String,Any]]
  }
  
  def observationSpace(id: String): Map[String,Any] = {
    val con = connect(s"/v1/envs/$id/observation_space/",
                      "GET",
                      s"""{"instance_id":"$id"}""")

    Json.toMap(getJson(con))("info").asInstanceOf[Map[String,Any]]
  }

  def startMonitor(id: String, force: Boolean, resume: Boolean): Unit = {
    val con = connect(s"/v1/envs/$id/monitor/start/",
                      "POST",
                      s"""{"instance_id":"$id", "force":$force, "resume":$resume}""")

    getJson(con)
  }

  def sample(id: String): Int = {
    val con = connect(s"/v1/envs/$id/action_space/sample",
                      "GET",
                      s"""{"instance_id":"$id"}""")

    Json.toMap(getJson(con))("action").asInstanceOf[Int]
  }
  
  def upload(trainingDir: String, apiKey: String, algId: String): Unit = {
    val con = connect("/v1/upload/",
                      "POST",
                      s"""{"training_dir":"$trainingDir","api_key":"$apiKey","algorithm_id":"$algId"}""")

    getJson(con)
  }

  def closeMonitor(id: String): Unit = {
    val con = connect(s"/v1/envs/$id/monitor/close/",
                      "POST",
                      s"""{"instance_id":"$id"}""")

    getJson(con)
  }

  def shutdownServer(): Unit = {
    val con = connect("/v1/shutdown/", "POST", null)

    getJson(con)
  }

  def isActionSpaceDiscrete(jobj: Map[String,Any]): Boolean = {
    jobj("name") == "Discrete"
  }
  
  def actionSpaceSize(jobj: Map[String,Any]): Integer = {
    jobj("n").asInstanceOf[Integer]
  }

  def observationSpaceSize(jobj: Map[String,Any]): Integer = {
    if (jobj.contains("n"))
      jobj("n").asInstanceOf[Integer]
    else
      jobj("high").asInstanceOf[List[Double]].length
  }

  private def getJson(con: HttpURLConnection): String = {
    try {
      Resources.tryWith(new BufferedReader(new InputStreamReader(con.getInputStream())))
      {_.readLine}
    }
    finally {
      con.getResponseCode
      con.getResponseMessage
      //con.disconnect
      try { Thread.sleep(1) } finally {}  // Nasty, but stabilises it
    }
  }

  private def connect(url: String, method: String, args: String): HttpURLConnection = {
    val con: HttpURLConnection = (new URL(baseUrl + url)).openConnection().asInstanceOf[HttpURLConnection]

    con.setRequestMethod(method)

    if (method == "POST") {
      con.setDoOutput(true)
      con.setRequestProperty("Content-Type", "application/json")
      con.setRequestProperty("Accept", "application/json")
      con.setRequestProperty("Keep-Alive", "true")

      Resources.tryWith(new OutputStreamWriter(con.getOutputStream())) {
        os => {
          os.write(args)
          os
        }
      }
    }

    con
  }
}
