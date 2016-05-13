package com.github.icha024


import scala.concurrent.forkjoin.ForkJoinPool
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

object FuturePlayground extends App {
  println("Future test")
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool(2))

  def f1 = Future {println("This is f1"); throw new NumberFormatException("Force ex 1")}
  def f2 = Future {println("This is f2"); 2}
  def f3 = Future {println("This is f3"); 3}
  def f4 = Future {println("This is f4"); 4}
  def f5 = Future {println("This is f5"); 5}
  def f6 = Future {println("This is f6"); 6}

  println("T1:")
  f1 fallbackTo f2
  Thread.sleep(1000)

  println("T2:")
  f3 fallbackTo f4
  Thread.sleep(1000)

  println("T3:")
  val third = f1 recoverWith {case _ => println("trigger"); f5}
  third.onComplete{case Success(x) => println(s"result: ${x}")} // x is value
  Thread.sleep(1000)

  println("T4:")
  val fourth =f3 recoverWith {case _ => f6}
  fourth.onComplete{case Success(x) => println(s"result: ${x}")}
  Thread.sleep(1000)

  println("T5:")
  val fifth = f1 recover {case _ => println("trigger"); f5}
  fifth.onComplete{case Success(x) => println(s"result: ${x}")} // x is future
  Thread.sleep(1000)

  println("T6:")
  val sixth =f3 recover {case _ => f6}
  sixth.onComplete{case Success(x) => println(s"result: ${x}")}
  Thread.sleep(1000)

}
