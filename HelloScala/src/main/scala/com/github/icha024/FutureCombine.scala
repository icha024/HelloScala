package com.github.icha024

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try

object FutureCombine extends App {

  println("start testing future combination")
  implicit val ec = ExecutionContext.global

  val a = Future {
    Thread.sleep(2000)
    Try(1)
  }
  val b = Future(Try(2))
  val c = Future(Try(throw new Exception("s happened")))

  val fSeq: Seq[Future[Try[Int]]] = Seq(a, b, c)

  private val fCount: Future[Int] = for {
    x <- Future.sequence(fSeq)
  } yield x.filter(_.isSuccess).length
  //  } yield x.collect { case Success(y) => y }.length // ALTERNATIVE

  println("Awaiting completion")
  private val res: Int = Await.result(fCount, 10.seconds)
  println(s"result is: $res")
}


