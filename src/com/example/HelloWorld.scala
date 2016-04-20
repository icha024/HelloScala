import java.text.DateFormat
import java.util.{Date, Locale}

import com.example.Complex
import com.example.mytraits.BaseA

import scala.collection.immutable.IndexedSeq


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
    collectionsCombinators

    val add3 = adder(3, _:Int) // "_ is the unamed magical wildcard"
    print("partial function adder: " + add3(2))


  }

  def adder(a: Int, b: Int): Int = {
    a + b
  }

  def collectionsCombinators: Unit = {
    // Functional combinators (eg. map fn): https://twitter.github.io/scala_school/collections.html
    println("Map List 1 to 10 with even values doubled:")
    var myList = 1 to 10
    myList.map(doubleOnEven).foreach(println)

    println("Filter List 1 to 10 with even only:")
    var myList2 = 1 to 10
    myList2.filter(evenOnly).foreach(println)

    println("Filter List 1 to 10 with even only (shorthand):")
    val shortEvenOnly = (someVal: Int) => someVal % 2 == 0
    myList2.filter(shortEvenOnly).foreach(println)

    println("partition is link filter, but keeps the discarded list as IndexedSeq and return everything in tuple")
    var myList3 = 1 to 10
    val partitionedList: (IndexedSeq[Int], IndexedSeq[Int]) = myList3.partition(shortEvenOnly)
    partitionedList.productIterator.foreach(println)

    println("Folding with curry")
    println("folding left 5")
    var foldList = 1 to 5
    println(foldList.foldLeft(0) { (a: Int, b: Int) => println(a); a + b }) // accumulator in a, new var in b
    println("folding right 5")
    println(foldList.foldRight(0) { (a: Int, b: Int) => println(a); a + b }) // accumulator in b, new var in a

    //    val myList = List("one", "two")
    println("printing big list flatten...")
    val bigList = List(myList, myList2)
    println(bigList.flatten) // flatten does not change original, but return a new one

    val mappedAndFlatList = bigList.flatMap(x => x.map(_ * 2))
    println("mapped and flatten list: " + mappedAndFlatList) // MAP is not FILTER

    //See:  http://stackoverflow.com/questions/19352030/why-list-dropwhile-doesnt-work
    println("drop '2' (longest predicate match): " + mappedAndFlatList.dropWhile((x: Int) => x == 2))
    println("drop '2' (longest predicate match): " + mappedAndFlatList.dropWhile(_ == 2))
    println("doesn't drop '4' (longest predicate match): " + mappedAndFlatList.dropWhile(_ == 4))

    // Use with map (with some case matching) is just like a List of Tuple2
    val myMap = Map(1 -> "one", 2 -> "two", 3 -> "three")
    val newMap = myMap.filter({ case (myKey: Int, myVal: String) => myKey >= 2 })
    println("filtered map = " + newMap)
  }

  def loopGuard: Unit = {
    println("for loop with guards, double each even number 10 or less")
    val listItems = for {
      i <- 1 to 20
      if (i <= 10 && i % 2 == 0)
    } yield i * 2
    println("list items are : " + listItems)
  }

  def evenOnly(someVal: Int): Boolean = {
    someVal % 2 == 0
  }

  def doubleOnEven(someVal: Int): String = {
    if (someVal % 2 == 0) {
      (someVal * 2) + " (doubled)"
    } else {
      someVal.toString
    }
  }

  def tupleShorthand: Unit = {
    // BEWARE: it assigned them tail first!
    val simpleQuery = "somekey" -> "some_details" -> "more" // "more" is second item, its always shorthand for tuple2

    println("what type is simpleQuery? " + simpleQuery)
    simpleQuery.productIterator.foreach(println)
    println("What class is it? " + simpleQuery.getClass)
    simpleQuery.productIterator.foreach(i => println("item: " + i))

    println("first item: " + simpleQuery._1)
    println("second item: " + simpleQuery._2)
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