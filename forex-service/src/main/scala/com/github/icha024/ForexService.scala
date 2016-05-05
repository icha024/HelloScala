package com.github.icha024

import akka.actor.Actor
import akka.event.Logging
import spray.caching._
import spray.client.pipelining._
import spray.http.MediaTypes._
import spray.http.{Uri, _}
import spray.routing._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, _}
import scala.util.Try
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
          complete("Up")
        }
      }
    } ~
      get {
        // http://localhost:8080/NZD/GBP
        path(Segment / Segment) {
          respondWithMediaType(`text/plain`)
          (baseCurrency, targetCurrency) => convertFromCachedCurrencyMap(baseCurrency, targetCurrency)
        } ~
          // http://localhost:8080/NZD/GBP/13
          path(Segment / Segment / Segment) {
            respondWithMediaType(`text/plain`)
            (baseCurrency, targetCurrency, currencyAmount) => {
              convertFromCachedCurrencyMap(baseCurrency, targetCurrency, currencyAmount)
            }
          }
      }

  def convertFromCachedCurrencyMap(baseCurrency: String, targetCurrency: String, currencyAmount: String = "1.0"): Route =
    onSuccess(
      cachedFetchCurrencies.map(
        currencyMap => calculateRate(currencyMap, baseCurrency, targetCurrency, currencyAmount)))(result => complete(result))

  def calculateRate(currencyMap: Map[String, Double], baseCurrency: String, targetCurrency: String, currencyAmount: String): String = {
    Try((currencyMap(targetCurrency) / currencyMap(baseCurrency) * currencyAmount.toDouble).toString) recover {
      case ex: NumberFormatException => log.error("Error parsing amount: " + ex.getMessage, ex)
        "Error parsing amount"
      case ex: NoSuchElementException => log.error("Currency symbol not available: " + ex.getMessage, ex)
        "Currency symbol not available"
      case ex => log.error("Error converting currency: " + ex.getMessage, ex)
        "Error converting currency"
    } get
  }

  val cache: Cache[Map[String, Double]] = LruCache(timeToLive = 8 hour)

  def cachedFetchCurrencies: Future[Map[String, Double]] = cache() {
    val pipeline = sendReceive
    pipeline(
      Get(
        Uri(
          "https://s3-eu-west-1.amazonaws.com/web-dist/eurofxref-daily.xml" // Test URL
          // "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml"  // Live URL
        )
      )
    ) map { response =>
      val currencyXmlResponse: String = response.entity.asString(HttpCharsets.`UTF-8`)
      if (response.status.isFailure) log.error(s"Received unexpected status ${response.status} : ${currencyXmlResponse}")
      log.info(s"OK, received ${currencyXmlResponse}")

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
}