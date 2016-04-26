package com.example

import akka.actor.Actor
import spray.http.HttpRequest
import spray.http.MediaTypes._
import spray.json._
import spray.routing._
import spray.can.Http
import spray.http._
import spray.client.pipelining._
import scala.concurrent.Future

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

  var eRates = <gesmes:Envelope xmlns:gesmes="http://www.gesmes.org/xml/2002-08-01" xmlns="http://www.ecb.int/vocabulary/2002-08-01/eurofxref">
    <gesmes:subject>Reference rates</gesmes:subject>
    <gesmes:Sender>
      <gesmes:name>European Central Bank</gesmes:name>
    </gesmes:Sender>
    <Cube>
      <Cube time="2016-04-26">
        <Cube currency="USD" rate="1.1287"/>
        <Cube currency="JPY" rate="125.45"/>
        <Cube currency="BGN" rate="1.9558"/>
        <Cube currency="CZK" rate="27.027"/>
        <Cube currency="DKK" rate="7.4418"/>
        <Cube currency="GBP" rate="0.77483"/>
        <Cube currency="HUF" rate="312.20"/>
        <Cube currency="PLN" rate="4.3799"/>
        <Cube currency="RON" rate="4.4747"/>
        <Cube currency="SEK" rate="9.1545"/>
        <Cube currency="CHF" rate="1.1000"/>
        <Cube currency="NOK" rate="9.2278"/>
        <Cube currency="HRK" rate="7.4785"/>
        <Cube currency="RUB" rate="74.8948"/>
        <Cube currency="TRY" rate="3.1951"/>
        <Cube currency="AUD" rate="1.4600"/>
        <Cube currency="BRL" rate="3.9943"/>
        <Cube currency="CAD" rate="1.4276"/>
        <Cube currency="CNY" rate="7.3345"/>
        <Cube currency="HKD" rate="8.7545"/>
        <Cube currency="IDR" rate="14917.56"/>
        <Cube currency="ILS" rate="4.2484"/>
        <Cube currency="INR" rate="75.1373"/>
        <Cube currency="KRW" rate="1299.14"/>
        <Cube currency="MXN" rate="19.7889"/>
        <Cube currency="MYR" rate="4.4279"/>
        <Cube currency="NZD" rate="1.6417"/>
        <Cube currency="PHP" rate="52.864"/>
        <Cube currency="SGD" rate="1.5280"/>
        <Cube currency="THB" rate="39.708"/>
        <Cube currency="ZAR" rate="16.3492"/>
      </Cube>
    </Cube>
  </gesmes:Envelope>

//  val pipeline: HttpRequest = sendReceive
//  val response: Future[HttpResponse] = pipeline(Get("http://spray.io/"))


  val cubeArray = eRates \ "Cube" \ "Cube"
  var currencyMap = (for {
    eachCube <- cubeArray \ "Cube"
    currency <- eachCube \ "@currency"
    rate <- eachCube \ "@rate"
  } yield (currency.toString -> rate.toString.toDouble)).toMap

  currencyMap += ("EUR" -> 1.0)

  def convertRate(target: String, base: String, curMap: Map[String,Double]): Double = {
    if (!(curMap.isDefinedAt(base)) || !(curMap.isDefinedAt(target))) {
      0.0 // Invalid base / or target rate. TODO: handle this proeprly
    } else {
      (curMap(base) / curMap(target))
    }
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
      (post | get) {
        // http://localhost:8080/ping
        path("ping") {
          complete {
            <html>pong</html>
          }
        }
      } ~
      get {
        // http://localhost:8080/hello/ian/chan
        path("hello" / Segment / Segment) {
          (firstName, lastName) =>
            complete {
              <html>Hello there
                {firstName}{lastName}
              </html>
            }
        }
      } ~
      get {
        // http://localhost:8080/greet?name=ian
        path("greet") {
          parameter('name) {
            (firstName) => complete {
              <html>Greeting with param to
                {firstName}
              </html>
            }
          }
        }
      } ~
      get {
        // http://localhost:8080/json
        // See Spray Json ref: https://github.com/spray/spray-json
        path("json") {
          respondWithMediaType(`application/json`)
          complete {
            """{"something":"something something json" can be invalid - no one cares}""" + Resp("EUR", "NZD", 0.777, NestedResult("0", "Success")).toJson.toString()
          }
        }
      } ~
      get {
        // http://localhost:8080/json2
        path("json2") {

          complete {
            // Convert object -> string -> object -> string
            Resp("EUR", "NZD", 0.777, NestedResult("0", "Success")).toJson.compactPrint.parseJson.convertTo[Resp].toJson.toString()
          }
        }
      } ~
      get {
        respondWithMediaType(`application/json`)
        path("convert" / Segment / Segment) {
          (baseCurrency, targetCurrency) => complete {
            Resp(baseCurrency, targetCurrency, convertRate(baseCurrency, targetCurrency, currencyMap), NestedResult("0", "Success")).toJson.prettyPrint
          }
        }
      }

}