package com.example

object MyCake extends App {
  trait GenericDao {
    def Imp: String
  }

  trait SqlDao extends GenericDao {
    def Imp: String = "SQL Implementation"
  }

  trait CloudantDao extends GenericDao {
    def Imp: String = "Cloudant Implementation"
  }

  trait MyServiceTrait {
    dao: GenericDao =>
    //println("My implementation is: " + dao.Imp)
  }

  class FunService {
  }

  val svc = new MyServiceTrait with CloudantDao
  println("Using cloud DI: " + svc.Imp)

  val svc2 = new MyServiceTrait with SqlDao
  println("Using SQL DI: " + svc2.Imp)

}
