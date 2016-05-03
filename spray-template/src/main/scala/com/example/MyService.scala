package com.example

import akka.actor.Actor

import scala.util.{Failure, Success, Try}
import spray.caching._
import spray.client.pipelining._
import spray.http.MediaTypes._
import spray.http.{Uri, _}
import spray.json._
import spray.routing._
import spray.http.StatusCodes._
import spray.routing.directives.CachingDirectives

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, _}
import scala.xml.XML
import spray.routing.directives.CachingDirectives._

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

  object Resp extends DefaultJsonProtocol {
    implicit val resultFormat = jsonFormat2(NestedResult.apply)
    implicit val respFormat = jsonFormat4(Resp.apply)
  }

  val simpleCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("5 min"))

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

            println("Responding from: " + this.self)
            /**
              * Delay in handling response - this proves Spray uses single thread listener, instead of new anonymous
              * actor (or future) per request. Makes sense because it's a trait bounded to the self-type annotated actor.
              * Now when we have RoundRobinPool of size 2, we can see the actor instance alternate as expected.
              */
            Thread.sleep(1000)
            <html>pong ref { this.self }</html>
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
          // Caching at this level still works for *some*.
          CachingDirectives.cache(simpleCache) { // Delegates to cache control (See: http://spray.io/documentation/1.2.3/spray-routing/caching-directives/cache/)
            println("doing work with xml (nocache)") // This will neven be printed with with no cache
            parameter('name) {
              (firstName) => complete {
                println("more work with xml on complete (nocache)") // This invoked everytime (?)
                <html>Greeting with param to
                  {firstName}
                </html>
              }
            }
          }
        }
      } ~
      get {
        // http://localhost:8080/json
        // See Spray Json ref: https://github.com/spray/spray-json
        path("json") {
          CachingDirectives.alwaysCache(simpleCache) { // Forces caching
            respondWithMediaType(`application/json`)
            complete {
              println("Do some json work (nocache hit)")
              """{"something":"something something json" can be invalid - no one cares}""" + Resp("EUR", "NZD", 0.777, NestedResult("0", "Success")).toJson.toString()
            }
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
          (baseCurrency, targetCurrency) => convertFromCachedCurrencyMap(baseCurrency, targetCurrency, 1.0)
        } ~
        // http://localhost:8080/convert/NZD/GBP/13
        path("convert" / Segment / Segment / Segment) {
          respondWithMediaType(`application/json`)
          (baseCurrency, targetCurrency, currencyAmount) =>  {
            try {
              val currencyAmountDouble = currencyAmount.toDouble
              convertFromCachedCurrencyMap(baseCurrency, targetCurrency, currencyAmountDouble)
            } catch {
              case nfe: NumberFormatException => complete(InternalServerError, "Invalid amount") // "Invalid amount" is the text response going back the wire
            }
          }

          //          (baseCurrency, targetCurrency) => complete{
          //          (baseCurrency, targetCurrency) => onSuccess(fetchCurrencies) {

          //            (baseCurrency, targetCurrency) => onComplete(cachedFetchCurrencies) { // We can pass in a Future directly via onComplete()
          //              currencyMap => complete(
          //                Resp(baseCurrency, targetCurrency, convertRate(baseCurrency, targetCurrency, currencyMap.get), NestedResult("0", "Success")).toJson.prettyPrint
          //              )
          //            }


          //          (baseCurrency, targetCurrency) => onComplete(cachedFetchCurrencies) {
          ////            cachedFetchCurrencies
          //          }


          //            case Sucess(result) => cachedFetchCurrencies
          //          }

//          (baseCurrency, targetCurrency, currencyAmount) => onComplete(fetchCurrencies) {
//          (baseCurrency, targetCurrency, currencyAmount) => onComplete(cachedFetchCurrencies) {
//
//            case Success(currencyMap) => complete(
//                  Resp(baseCurrency, targetCurrency, convertRate(baseCurrency, targetCurrency, currencyMap, Option(currencyAmount.toDouble)), NestedResult("0", "Success")).toJson.prettyPrint
//              )
//            case Failure(ex) => complete(InternalServerError, "Something went wrong: " + ex) // Probably hide error in prod
//
//            /** This works, but doesn't handle errors. */
////            currencyMap => complete(
////              Resp(baseCurrency, targetCurrency, convertRate(baseCurrency, targetCurrency, currencyMap.get), NestedResult("0", "Success")).toJson.prettyPrint
////            )
//          }

        }
      }

  def convertFromCachedCurrencyMap(baseCurrency: String, targetCurrency: String, currencyAmount: Double): Route = onComplete(cachedFetchCurrencies) {
    case Success(currencyMap) =>
      Try(convertRate(baseCurrency, targetCurrency, currencyMap, Option(currencyAmount.toDouble))) match {
        case Success(rate) => complete(Resp(baseCurrency, targetCurrency, rate, NestedResult("0", "Success")).toJson.prettyPrint)
        case Failure(e) => complete(InternalServerError, "Error in the conversion: " + e.getMessage)
      }
    case Failure(ex) => complete(InternalServerError, "Something went wrong: " + ex) // Probably hide error in prod

    /** This works, but doesn't handle errors. */
    //            currencyMap => complete(
    //              Resp(baseCurrency, targetCurrency, convertRate(baseCurrency, targetCurrency, currencyMap.get), NestedResult("0", "Success")).toJson.prettyPrint
    //            )
  }

  val cache: Cache[Map[String, Double]] = LruCache(timeToLive = 5 minutes)

  def cachedFetchCurrencies: Future[Map[String, Double]] = cache() {
    Thread.sleep(1000) // can trigger timeout on server if < 1sec.
    // Handle using error handler, see: http://spray.io/documentation/1.2.3/spray-routing/key-concepts/timeout-handling/
    /** The limit to wait for server is set in application.conf:
         spray.can.server {
          request-timeout = 2s
        }
      */

    fetchCurrencies
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

  def convertRate(target: String, base: String, curMap: Map[String, Double], currencyAmount: Option[Double]): Double = {
    if (!(curMap.isDefinedAt(base)) || !(curMap.isDefinedAt(target))) {
      throw new IllegalArgumentException("Currency requested was not available")
    } else {
      (curMap(base) / curMap(target) * currencyAmount.getOrElse(1.0))
    }
  }
}