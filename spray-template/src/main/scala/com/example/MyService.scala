package com.example

import akka.actor.Actor
import spray.http.MediaTypes._
import spray.routing._
import spray.json._
import spray.json.DefaultJsonProtocol._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MyServiceActor extends Actor with MyService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)
}


// this trait defines our service behavior independently from the service actor
trait MyService extends HttpService {

  case class NestedResult(statusCode: String, message: String)
  case class Resp(baseCurrency: String, currency: String, rate: Double, result: NestedResult)
//  case class Resp(baseCurrency: String, currency: String, rate: Double, result: String)

  object Resp extends DefaultJsonProtocol {
    implicit val resultFormat = jsonFormat2(NestedResult.apply)
    implicit val respFormat = jsonFormat4(Resp.apply)
  }


  val myRoute =
    path("") {
      get {
        respondWithMediaType(`text/html`) {
          // XML is marshalled to `text/xml` by default, so we simply override here
          complete {
            <html>
              <body>
                <h1>Say hello to
                  <i>spray-routing</i>
                  on
                  <i>spray-can</i>
                  !</h1>
              </body>
            </html>
          }
        }
      }
    } ~
      (post | get) { // http://localhost:8080/ping
        path("ping") {
          complete {
            <html>pong</html>
          }
        }
      } ~
      get { // http://localhost:8080/hello/ian/chan
        path("hello" / Segment / Segment) {
          (firstName, lastName) =>
            complete {
              <html>Hello there
                {firstName}{lastName}
              </html>
            }
        }
      } ~
  get{ // http://localhost:8080/greet?name=ian
    path("greet"){
      parameter('name) {
        (firstName) => complete {
          <html>Greeting with param to {firstName}</html>
        }
      }
    }
  } ~
  get { // http://localhost:8080/json
    // See Spray Json ref: https://github.com/spray/spray-json
    path("json") {
      respondWithMediaType(`application/json`)
      complete {
        """{"something":"something something json" can be invalid - no one cares}""" + Resp("EUR", "NZD", 0.777, NestedResult("0", "Success")).toJson.toString()
      }
    }
  } ~
  get { // http://localhost:8080/json2
    path("json2") {
      respondWithMediaType(`application/json`)
      complete {
        // Convert object -> string -> object -> string
        Resp("EUR", "NZD", 0.777, NestedResult("0", "Success")).toJson.compactPrint.parseJson.convertTo[Resp].toJson.toString()
      }
    }
  }


}