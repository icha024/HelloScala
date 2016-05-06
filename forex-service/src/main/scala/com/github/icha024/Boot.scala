package com.github.icha024

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import akka.actor.{Props, ActorSystem}

object Boot {

  def main(args: Array[String]): Unit = {
    // we need an ActorSystem to host our application in
    implicit val system = ActorSystem("on-spray-can")
    val availableProcessors: Int = Runtime.getRuntime.availableProcessors()

    // create and start our service actor
    val nrOfServices = availableProcessors;
    val service = system.actorOf(RoundRobinPool(nrOfServices).props(Props[ForexServiceActor]), "forex-service")
    println(s"Started running on ${availableProcessors} forex-services actors")

    implicit val timeout = Timeout(5.seconds)
    // start a new HTTP server on port 8080 with our service actor as the handler
    IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = Option(System.getenv("PORT")).getOrElse("8080").toInt)
  }
}
