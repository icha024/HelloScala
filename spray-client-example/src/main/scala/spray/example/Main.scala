// See: https://github.com/spray/spray/blob/release/1.2/examples/spray-client/simple-spray-client/src/main/scala/spray/examples/Main.scala
package spray.examples

import scala.util.{Failure, Success}
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.event.Logging
import akka.io.IO
import spray.json.{DefaultJsonProtocol, JsonFormat}
import spray.can.Http
import spray.httpx.SprayJsonSupport
import spray.client.pipelining._
import spray.util._

import scala.concurrent.ExecutionContext

// Define JSON object for unmarshalling
case class Elevation(location: Location, elevation: Double)
case class Location(lat: Double, lng: Double)
case class GoogleApiResult[T](status: String, results: List[T]) // 'T' is Elevation
//case class GoogleApiResult(status: String, results: List[Elevation]) // declare type directly

object ElevationJsonProtocol extends DefaultJsonProtocol { // Remember, these are MAGNETS !!!!!
  implicit val locationFormat = jsonFormat2(Location)
//  implicit val elevationFormat = jsonFormat2(Elevation) // Same as the .apply version
  implicit val elevationFormat = jsonFormat2(Elevation.apply)

//  implicit def googleApiResultFormat[T :JsonFormat] = jsonFormat2(GoogleApiResult.apply[T])
  implicit val googleApiResultFormat = jsonFormat2(GoogleApiResult.apply[Elevation]) // This also works!

//  implicit val googleApiResultFormat = jsonFormat2(GoogleApiResult.apply) // Wont compile, T not inferred
//  implicit val googleApiResultFormat[Elevation] = jsonFormat2(GoogleApiResult.apply[Elevation]) // Won't compile
//  implicit val googleApiResultFormat[T :GoogleApiResult] = jsonFormat2(GoogleApiResult.apply[T]) // 'val' wont compile, need to be 'def' if passing in generic on left.

//  implicit val googleApiResultFormat = jsonFormat2(GoogleApiResult) // This would work if we don't use generic in case class.

  /** For generic type basics see: https://twitter.github.io/scala_school/type-basics.html
    * Probably because type need to be 'nailed down' at the time of invocation (?!)
    *
    * My understanding, the implicit magnet needs to be initilized for it to start attracting metal and make things stick to it.
    * That is why we need to nail down the type in magnets, can't leave it loosely defined.
    */
}

object Main extends App {
  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("simple-spray-client")

  // This is nice, my other example used to use: implicit val ec = ExecutionContext.global
  // They are actually the same thing, src: implicit def dispatcher: ExecutionContextExecutor
//  import system.dispatcher // execution context for futures below
  implicit val ec = ExecutionContext.global // This still works as the alternative

  val log = Logging(system, getClass)

  log.info("Requesting the elevation of Mt. Everest from Google's Elevation API...")

  import ElevationJsonProtocol._
  import SprayJsonSupport._
  val pipeline = sendReceive ~> unmarshal[GoogleApiResult[Elevation]] // type 'T' is Elevation
//  val pipeline = sendReceive ~> unmarshal[GoogleApiResult] // This would work if we did't use generic

  val responseFuture = pipeline {
    Get("http://maps.googleapis.com/maps/api/elevation/json?locations=27.988056,86.925278&sensor=false")
//    Get("http://maps.example.com/maps/api/elevation/json?locations=27.988056,86.925278&sensor=false") // invalid domain
//    Get("https://s3-eu-west-1.amazonaws.com/web-dist/elevation2.json") // unexpected json element name

/**    Proper response looks like this: */
//    {
//      "results" : [
//      {
//        "elevation" : 8815.7158203125,
//        "location" : {
//          "lat" : 27.988056,
//          "lng" : 86.92527800000001
//        },
//        "resolution" : 152.7032318115234
//      }
//      ],
//      "status" : "OK"
//    }
  }
  responseFuture onComplete {
    // Format is: (statusCode, List[Elevation]). The cons op return the 'head' elem of list.
    case Success(GoogleApiResult(_, Elevation(_, elevation) :: _)) =>
//      log.info("The elevation of Mt. Everest is: {} m", elevation) // slf4j-esque syntax
      log.info(s"The elevation of Mt. Everest is: ${elevation} m") // Rolling with it Scala style (like C's sprintf)
      shutdown()

    case Success(somethingUnexpected) => // When will this happen (?!)
      log.warning("The Google API call was successful but returned something unexpected: '{}'.", somethingUnexpected)
      shutdown()

    case Failure(error) => // Eg. Unreachable url (ie. maps.example.com) or unmarshalling fails (eg. Status field not found)
      log.error(error, "Couldn't get elevation")
      shutdown()
  }

  // Probably don't need to shutdown every request  if we have a long running server (?)
  def shutdown(): Unit = {
    IO(Http).ask(Http.CloseAll)(1.second).await
    system.shutdown()
  }
}