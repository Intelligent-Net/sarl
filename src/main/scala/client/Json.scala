package client

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
//import scala.util.parsing.json.JSON

object Json {
  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  /*
  def main(args: Array[String]): Unit = {
    args.foreach(println(_))
    println(toJson("{'hello':'world'}"))
    println(toJson("[1,2,3]"))
    println(toJson(Map("hello" -> "world")))
    println(toJson(Array(1,2,3)))
    val m = toMap("{\"hello\": \"world\", \"nested\":{\"nest1\":\"one\",\"nest2\":2700000000,\"nest3\":27.5,\"nest4\":[1,2,3]}}")
    println(m)
    println(toJson(m))
    println(m.getClass)
    println(m("nested").asInstanceOf[Map[String,Any]]("nest1").getClass)
    println(m("nested").asInstanceOf[Map[String,Any]]("nest2").getClass)
    println(m("nested").asInstanceOf[Map[String,Any]]("nest3").getClass)
    println(m("nested").asInstanceOf[Map[String,Any]]("nest4").getClass)
    println(m("nested").asInstanceOf[Map[String,Any]]("nest4"))
    //val sm = m("nested").asInstanceOf[Map[String,Any]]
    //val sm = m('nested).asInstanceOf[Map[String,Any]]
    val sm = m("nested").asInstanceOf[Map[String,Any]]
    println(sm.getClass)
    println(sm)
    println(sm("nest2"))
  }
  */

  def toJson(value: Any): String = {
    mapper.writeValueAsString(value)
  }

  def toMap(json: String): Map[String,Any] = {
    mapper.readValue(json, classOf[Map[String,Any]])
  }

  def toStep(json: String): Step = {
    mapper.readValue(json, classOf[Step])
  }
}
