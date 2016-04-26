package com.example

import akka.actor.Actor
import spray.http.MediaTypes._
import spray.routing._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MyServiceActor extends Actor with MyService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)
}


// this trait defines our service behavior independently from the service actor
trait MyService extends HttpService {

  val myRoute =
    path("") {
      get {
        respondWithMediaType(`text/html`) {
          // XML is marshalled to `text/xml` by default, so we simply override here
          complete {
            <html>
              <body>
                <h1>Say hello to
                  <i>spray-routing</i>
                  on
                  <i>spray-can</i>
                  !</h1>
              </body>
            </html>
          }
        }
      }
    } ~
      (post | get) { // http://localhost:8080/ping
        path("ping") {
          complete {
            <html>pong</html>
          }
        }
      } ~
      get { // http://localhost:8080/hello/ian/chan
        path("hello" / Segment / Segment) {
          (firstName, lastName) =>
            complete {
              <html>Hello there
                {firstName}{lastName}
              </html>
            }
        }
      } ~
  get{ // http://localhost:8080/greet?name=ian
    path("greet"){
      parameter('name) {
        (firstName) => complete {
          <html>Greeting with param to {firstName}</html>
        }
      }
    }
  }
}