package com.example

object XmlParserPlayground extends App {

  // Play with XML, see: http://www.codecommit.com/blog/scala/working-with-scalas-xml-support

  val eRates = <gesmes:Envelope xmlns:gesmes="http://www.gesmes.org/xml/2002-08-01" xmlns="http://www.ecb.int/vocabulary/2002-08-01/eurofxref">
    <gesmes:subject>Reference rates</gesmes:subject>
    <gesmes:Sender>
      <gesmes:name>European Central Bank</gesmes:name>
    </gesmes:Sender>
    <Cube>
      <Cube time="2016-04-26">
        <Cube currency="USD" rate="1.1287"/>
        <Cube currency="JPY" rate="125.45"/>
        <Cube currency="BGN" rate="1.9558"/>
        <Cube currency="CZK" rate="27.027"/>
        <Cube currency="DKK" rate="7.4418"/>
        <Cube currency="GBP" rate="0.77483"/>
        <Cube currency="HUF" rate="312.20"/>
        <Cube currency="PLN" rate="4.3799"/>
        <Cube currency="RON" rate="4.4747"/>
        <Cube currency="SEK" rate="9.1545"/>
        <Cube currency="CHF" rate="1.1000"/>
        <Cube currency="NOK" rate="9.2278"/>
        <Cube currency="HRK" rate="7.4785"/>
        <Cube currency="RUB" rate="74.8948"/>
        <Cube currency="TRY" rate="3.1951"/>
        <Cube currency="AUD" rate="1.4600"/>
        <Cube currency="BRL" rate="3.9943"/>
        <Cube currency="CAD" rate="1.4276"/>
        <Cube currency="CNY" rate="7.3345"/>
        <Cube currency="HKD" rate="8.7545"/>
        <Cube currency="IDR" rate="14917.56"/>
        <Cube currency="ILS" rate="4.2484"/>
        <Cube currency="INR" rate="75.1373"/>
        <Cube currency="KRW" rate="1299.14"/>
        <Cube currency="MXN" rate="19.7889"/>
        <Cube currency="MYR" rate="4.4279"/>
        <Cube currency="NZD" rate="1.6417"/>
        <Cube currency="PHP" rate="52.864"/>
        <Cube currency="SGD" rate="1.5280"/>
        <Cube currency="THB" rate="39.708"/>
        <Cube currency="ZAR" rate="16.3492"/>
      </Cube>
    </Cube>
  </gesmes:Envelope>

  println("Looping currency XML data")
  val cubeArray = eRates \ "Cube" \ "Cube"
  println("Cube array: " + cubeArray)
  for (eachCube <- cubeArray \ "Cube") {
    println("eachCube: " + eachCube)
    val currency = eachCube \ "@currency"
    val rate = eachCube \ "@rate"
    println(s"Currency: $currency with Rate: $rate")
  }
}
