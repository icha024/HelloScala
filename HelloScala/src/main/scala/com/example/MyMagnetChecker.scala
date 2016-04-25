package com.example

object MyMagnetChecker extends App {
  trait MyMagnet { // should probably be sealed
    type Result
    def getRes: Result
    def apply(): Result
  }

  object MyMagnet {
    implicit def convertFromInt(myVal: Int) = new MyMagnet {
      override type Result = Int
      override def apply(): Result = myVal
      override def getRes: Result = myVal
    }

    implicit def convertFromString(myVal: String) = new MyMagnet {
      override type Result = String
      override def apply(): Result = "Type is a String: " + myVal
      override def getRes: Result = "String type: " + myVal
    }
  }

  def findStickyType(mag: MyMagnet): Unit = {
    println("Finding magnet of some type... " + mag.getRes)
  }

  findStickyType("abc"); // create 'MyMagnet' with String constructor
  findStickyType(123);   // create 'MyMagnet' with Int constructor

  // Magnet pattern = Instead of method overloading, we use one big pojo with variable amount of fields.
  // Then instead of messy if/else checks on each field of the big pojo to run different init, we use 'implicit' method feature of Scala.
}