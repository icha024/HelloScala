package com.example

object MyCake extends App {

  trait GenericDao { // This is an 'interface'
    def Imp: String
  }

  trait SqlDao extends GenericDao { // This is like implementing interface with default in Java8 (An implementation)
    def Imp: String = "SQL Implementation"
  }

  trait CloudantDao extends GenericDao { // This is like implementing interface with default in Java8 (An implementation)
    def Imp: String = "Cloudant Implementation"
  }

  trait MyServiceTrait {
    // This relies on 'interface' instead of 'implementation' - it just normal Java/Spring best practice anyway.
    dao: GenericDao => // Syntax for trait 'self type annotation', we really should just call it 'extending an interface'
    println("My implementation is: " + dao.Imp)
  }

  val svc = new MyServiceTrait with CloudantDao
  println("svc using Cloudant DI returns: " + svc.Imp)

  val svc2 = new MyServiceTrait with SqlDao
  println("svc2 using SQL DI returns: " + svc2.Imp)

}
