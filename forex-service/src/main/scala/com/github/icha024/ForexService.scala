package com.github.icha024

import akka.actor.Actor
import akka.event.Logging
import spray.caching._
import spray.client.pipelining._
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.http.{Uri, _}
import spray.routing._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, _}
import scala.util.{Failure, Success, Try}
import scala.xml.XML

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class ForexServiceActor extends Actor with ForexService {

  implicit val ec = ExecutionContext.global

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(serviceRoute)
}

// this trait defines our service behavior independently from the service actor
trait ForexService extends HttpService {
  // Brings in all the Akka context and implicit values.
  this: ForexServiceActor =>

  val log = Logging(context.system, this)

  val serviceRoute =
    path("") {
      get {
        respondWithMediaType(`text/plain`) {
          // XML is marshalled to `text/xml` by default, so we simply override here
          complete {
            "Up"
          }
        }
      }
    } ~
      get {
        // http://localhost:8080/convert/NZD/GBP
        path(Segment / Segment) {
          respondWithMediaType(`text/plain`)
          (baseCurrency, targetCurrency) => convertFromCachedCurrencyMap(baseCurrency, targetCurrency, 1.0)
        } ~
          // http://localhost:8080/convert/NZD/GBP/13
          path(Segment / Segment / Segment) {
            respondWithMediaType(`text/plain`)
            (baseCurrency, targetCurrency, currencyAmount) => {
              detach() {
                // Detach the future to another EC (remember we have 1 sec blocking delay in cache service)
                Try(currencyAmount.toDouble) match {
                  case Success(amount) => convertFromCachedCurrencyMap(baseCurrency, targetCurrency, amount)
                  case Failure(ex) => log.error("Invalid amount", ex)
                    complete(InternalServerError, "Invalid amount") // "Invalid amount" is the text response going back the wire
                }
              }
            }
          }
      }

  def convertFromCachedCurrencyMap(baseCurrency: String, targetCurrency: String, currencyAmount: Double): Route = onComplete(cachedFetchCurrencies) {
    case Success(currencyMap) =>
      Try(convertRate(baseCurrency, targetCurrency, currencyMap, currencyAmount)) match {
        case Success(rate) => complete(rate.toString)
        case Failure(e) => log.error("Error in conversion", e)
          complete(InternalServerError, "Error in the conversion.")

      }
    case Failure(ex) => log.error("Error getting currency map", ex)
      complete(InternalServerError, "Error getting currency map")
  }

  val cache: Cache[Map[String, Double]] = LruCache(timeToLive = 8 hour)

  def cachedFetchCurrencies: Future[Map[String, Double]] = cache() {
    fetchCurrencies
  }

  def fetchCurrencies: Future[Map[String, Double]] = {
    val pipeline = sendReceive
    pipeline(
      Get(
        Uri(
          "https://s3-eu-west-1.amazonaws.com/web-dist/eurofxref-daily.xml"  // Test URL
          // "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml"  // Live URL
        )
      )
    )
      .map { response =>
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

  def convertRate(target: String, base: String, curMap: Map[String, Double], currencyAmount: Double): Double = {
    if (!(curMap.isDefinedAt(base)) || !(curMap.isDefinedAt(target))) {
      throw new IllegalArgumentException("Currency requested was not available")
    } else {
      (curMap(base) / curMap(target) * currencyAmount)
    }
  }
}