package com.example

import akka.actor.{Actor, _}
import akka.event.Logging
import akka.routing.RoundRobinPool
import akka.util.Timeout

import scala.concurrent.duration._
import akka.pattern.ask

import scala.concurrent.ExecutionContext

object ActorPlayground extends App {

  case class MsgOne(description: String)
  case class MsgTwo(description: String)
  case class MsgThree(description: String)

  // Create an Akka system
  implicit val system = ActorSystem("PlaygroundSystem")
  implicit val timeout = Timeout(5.seconds)
  implicit val ec = ExecutionContext.global

  val a1 = system.actorOf(RoundRobinPool(1).props(Props[ActOne]), "demo-service1")
  val a2 = system.actorOf(RoundRobinPool(2).props(Props[ActTwo]), "demo-service2")
  val a3 = system.actorOf(Props[ActThree], "service3")

  a1 ! MsgOne("My custom message 1")
  a2 ! MsgTwo("custom msg 2")


  Thread.sleep(2000)
  system.shutdown()


  class ActOne extends Actor {
    def receive = {
      case MsgOne(msg) => println("Received msgOne: " + msg)
      case _ => println("Message unrecognised 1")
    }
  }

  class ActTwo extends Actor {
    def receive = {
      case MsgTwo(msg) => println("Received msgTwo: " + msg);
        (for {
           theResponse <- a3 ? MsgThree("msg 3")
        } yield theResponse).foreach(msg => msg match {
          case MsgTwo(theMsg) => println("received: " + theMsg)
        })
      case _ => println("Message unrecognised 2")
    }
  }

  class ActThree extends Actor {
    def receive = {
      case MsgThree(msg) => println("Received MsgThree: " + msg)
        sender ! MsgTwo("this is my reply!!")
      case _ => println("Message unrecognised 3")
    }
  }
}

