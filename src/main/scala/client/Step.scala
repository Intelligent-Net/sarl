package client;

//case class StepInt(observation: Integer = 0, reward: Double = 0.0, done: Boolean = false, info: Map[String,Any])

//case class Step(observation: List[Double], reward: Double, done: Boolean, info: Map[String,Any])
case class Step(observation: Any, reward: Double, done: Boolean, info: Map[String,Any])

