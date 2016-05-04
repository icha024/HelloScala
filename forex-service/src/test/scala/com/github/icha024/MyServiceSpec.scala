//package com.github.icha024
//
//import org.specs2.mutable.Specification
//import spray.testkit.Specs2RouteTest
//import spray.http._
//import StatusCodes._
//
//class ForexServiceSpec extends Specification with Specs2RouteTest with ForexService {
//  def actorRefFactory = system
//
//  "ForexService" should {
//
//    "return a greeting for GET requests to the root path" in {
//      Get() ~> serviceRoute ~> check {
//        responseAs[String] must contain("Up")
//      }
//    }
//
//    "leave GET requests to other paths unhandled" in {
//      Get("/NZD/GBP") ~> serviceRoute ~> check {
//        handled must beEqualTo("0.4719680818663581")
//      }
//    }
//
////    "return a MethodNotAllowed error for PUT requests to the root path" in {
////      Put() ~> sealRoute(serviceRoute) ~> check {
////        status === MethodNotAllowed
////        responseAs[String] === "HTTP method not allowed, supported methods: GET"
////      }
////    }
//  }
//}
