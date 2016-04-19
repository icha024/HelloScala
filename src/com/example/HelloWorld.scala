import java.text.DateFormat
import java.util.{Date, Locale}

import com.example.Complex
import com.example.mytraits.BaseA


object HelloWorld extends BaseA {
  def main(arg: Array[String]): Unit = {
    println("Hello world")
    val now = new Date
    val df = DateFormat.getDateInstance(DateFormat.LONG, Locale.FRANCE)
    println(df format now)
    println(df.format(now))
    //    oncePerSecond(() => println("Time flies: " + new Date))

    val myComplex = new Complex(1.2, 3.4)
    println("my comp im: " + myComplex.im + ", toString: " + myComplex.toString)
    myComplex.im = 6.7
    //    myComplex.re = 99.8 // not valid without setter.
    myComplex.zzz = 78.9
    println("my comp im: " + myComplex.im + ", toString: " + myComplex.toString)

    playWithList()
    havingCurry

    implicit val tupImp = "my implicit param"
    tuplefun
    tuplefun(ee = "my explicitly param passed by field name")

    testingTraitsExample

    tupleShorthand
  }

  def tupleShorthand: Unit = {
    val simpleQuery = "somekey" -> "some_details" -> "more" // "more" is ignored, its always shorthand for tuple2
    println("what type is simpleQuery? " + simpleQuery)
    simpleQuery.productIterator.foreach(println)
    println("What class is it? " + simpleQuery.getClass)
    simpleQuery.productIterator.foreach(i => println("item: " + i))
  }

  def testingTraitsExample: Unit = {
    println("Checking trait thing, inside")
    draw()

    println("Checking trait thing, my class obj")
    val traitObj = new MyTraitTest
    traitObj.draw()

    println("Checking trait thing, my class obj2")
    val traitObj2 = new MyTraitThing2
    traitObj2.draw()
  }

  class MyTraitThing2 extends BaseB

  //  def saySomething(): Unit = println("Time flies: " + new Date)

  def tuplefun(implicit ee: String): Unit = {
    var (a, b) = (1, "something")
    var c, d = (3, 4)
    println("a is : " + a)
    println("b is : " + b)
    println("c is : " + c)
    println("d is : " + d)
    println("ee (implicit) is : " + ee)
  }

  def havingCurry: Unit = {
    def modN(n: Int)(x: Int) = ((x % n) == 0)
    println("8 % 4 = " + modN(4)(8))

    // Alternative
    def finalFn(i: Int, partialModFn: (Int) => Boolean): Any = partialModFn(i)
    println("8 % 4 = " + finalFn(8, modN(4)))
  }

  def playWithList(): Unit = {
    val animalsList = List("dog", "cat");
    val superAnimals: List[String] = "eagle" :: animalsList ::: List("dolphin")
    println(superAnimals)
    val retSomething = superAnimals match {
      case start :: end => println("start: " + start + ", end: " + end + ", head: " + superAnimals.head + "" +
        ", take right 2: " + superAnimals.takeRight(2) + ", tail: " + superAnimals.tail + ", last: " + superAnimals.last)
        1
      case _ => println("No match!")
        2
    }
    println("retSomething was: " + retSomething);
  }

  def oncePerSecond(callback: () => Unit): Unit = {
    while (true) {
      callback()
      Thread sleep 1000
    }
  }
}

class MyTraitTest extends BaseA with BaseB with BaseC

trait BaseB extends BaseA {
  override def draw(): Unit = {
    println("Hello from base B")
  }
}

trait BaseC extends BaseA {
  override def draw(): Unit = {
    println("Hello from base C")
  }
}