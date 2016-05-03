package com.github.icha024

object CurryTest extends App {
  def filter(xs: List[Int], p: Int => Boolean): List[Int] =
  //    if (xs.isEmpty) List(1)
    if (xs.isEmpty) xs
    else if (p(xs.head)) xs.head :: filter(xs.tail, p)
    else filter(xs.tail, p)

  def modN(n: Int)(x: Int) = ((x % n) == 0)

  val nums = List(1, 2, 3, 4, 5, 6, 7, 8)
  println(filter(nums, modN(2)))
  println(filter(nums, modN(3)))

  println("8 % 4 = " + modN(4)(8))

  def finalFn(i: Int, partialModFn: (Int) => Boolean): Any = partialModFn(i)

  println("7 % 4 = " +finalFn(7, modN(4))) // same as modN(4)(7)

  println("8 % 4 = " + modN(4)(8))
  println("8 % 4 = " + finalFn(8, modN(4)))


}