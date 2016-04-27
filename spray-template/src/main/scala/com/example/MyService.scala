package com.example

import akka.actor.Actor
import spray.caching._
import spray.client.pipelining._
import spray.http.MediaTypes._
import spray.http.{Uri, _}
import spray.json._
import spray.routing._
import scala.concurrent.duration._

import scala.concurrent.{ExecutionContext, _}
import scala.xml.XML

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MyServiceActor extends Actor with MyService {

  implicit val ec = ExecutionContext.global

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
  // Brings in all the Akka context and implicit values.
  this: MyServiceActor =>

  case class NestedResult(statusCode: String, message: String)
  case class Resp(baseCurrency: String, currency: String, rate: Double, result: NestedResult)

  val cache: Cache[Map[String, Double]] = LruCache(timeToLive = 5 minutes)
//  def cachedOp[T](key: T): Future[Map[String, Double]] = cache(key) {
  def cachedOp: Future[Map[String, Double]] = cache() {
    fetchCurrencies
  }

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
        // http://localhost:8080/convert/NZD/GBP
        path("convert" / Segment / Segment) {
          respondWithMediaType(`application/json`)
//          (baseCurrency, targetCurrency) => complete{
//          (baseCurrency, targetCurrency) => onSuccess(fetchCurrencies) {
            (baseCurrency, targetCurrency) => onComplete(cachedOp) { // We can pass in a Future directly via onComplete()
              currencyMap => complete(
                Resp(baseCurrency, targetCurrency, convertRate(baseCurrency, targetCurrency, currencyMap.get), NestedResult("0", "Success")).toJson.prettyPrint
              )
            }
        }
      }

  def fetchCurrencies: Future[Map[String, Double]] = {
    //see: https://www.implicitdef.com/2015/11/19/comparing-scala-http-client-libraries.html
    val pipeline = sendReceive
    pipeline(
      // Building the request
      Get(
        Uri(
          "https://s3-eu-west-1.amazonaws.com/web-dist/eurofxref-daily.xml"
//          "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml"
        )
        //                .withQuery("some_parameter" -> "some_value", "some_other_parameter" -> "some_other_value")
      )
      //                .withHeaders(HttpHeaders.`Cache-Control`(CacheDirectives.`no-cache`))
    )
      .map { response =>
        // Treating the response
        println(s"The response header Content-Length was ${response.header[HttpHeaders.`Content-Length`]}")
        val currencyXmlResponse: String = response.entity.asString(HttpCharsets.`UTF-8`)
        if (response.status.isFailure) {
          sys.error(s"Received unexpected status ${response.status} : ${currencyXmlResponse}")
        }
        println(s"OK, received ${currencyXmlResponse}")
        val cubeArray = XML.loadString(currencyXmlResponse) \ "Cube" \ "Cube"
        var currencyMap = (for {
          eachCube <- cubeArray \ "Cube"
          currency <- eachCube \ "@currency"
          rate <- eachCube \ "@rate"
        } yield (currency.toString -> rate.toString.toDouble)).toMap

        currencyMap += ("EUR" -> 1.0)
        currencyMap
      }
  }

  def convertRate(target: String, base: String, curMap: Map[String, Double]): Double = {
    if (!(curMap.isDefinedAt(base)) || !(curMap.isDefinedAt(target))) {
      0.0 // Invalid base / or target rate. TODO: handle this proeprly
    } else {
      (curMap(base) / curMap(target))
    }
  }
}