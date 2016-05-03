package com.github.icha024

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.concurrent.blocking
import scala.concurrent.duration._
import scala.concurrent.forkjoin.ForkJoinPool

object ExecutionContextPlayground extends App {

  var counter = new AtomicInteger(0)
  val starTime = System.currentTimeMillis()

//  implicit val ec = ExecutionContext.global
  implicit val ec = ExecutionContext.fromExecutor(new ForkJoinPool(2))


  for (i <- 1 to 10) {
    Future {
      blocking { // This may/maynot be honoured. It's an advise to Scala only.
        Thread.sleep(1000)
        val currentCount: Int = counter.incrementAndGet()
        println("Started " + currentCount)
        if (currentCount == 10) {
          println(s"Time taken: ${System.currentTimeMillis() - starTime}")
        }
      }
    }
  }

  Thread.sleep(7000)
  println("ending prog")

}
