package com.example

object ExtractorLab extends App {

  val httpUrl = "http://www.google.com"
  val ftpUrl = "ftp://www.google.com"
  val sshUrl = "ssh://www.google.com"

  val url = new MyUrl(httpUrl)
  printProtoType(url)
  val url2 = new MyUrl(ftpUrl)
  printProtoType(url2)
  val url3 = new MyUrl(sshUrl)
  printProtoType(url3)
  val url4 = new MyUrl("")
  printProtoType(url4)
  val url5 = new MyUrl(null)
  printProtoType(url5)

  def printProtoType(url: MyUrl): Unit = {
    url match {
//      case MyUrl(x) => println("x matches: " + x) // Both constructor pattern is valid
      case MyUrl(protocol, _) if protocol.equals("http") => println("It's a HTTP endpoint")
      case MyUrl(protocol, _) if protocol.equals("ftp") => println("FTP is the way")
      case MyUrl(protocol, _) => println("I have no idea what this is: " + protocol)
      case _ => println("Unknown input")
    }
  }

  val urlList = List(url, url2, url3, url4, url5)
//  urlList.map(_.protocol.getOrElse("Not specified")).foreach(prot => println("protocol is: " + prot))
  urlList.filter(_.url != None).map(_.url).foreach(outUrl => println("url is: " + outUrl.get))
  urlList.filter(_.protocol != None).map(_.protocol).foreach(protocol => if (!protocol.get.isEmpty) println("protocol is: " + protocol.get))

  println("do the same with flatmap")
  urlList.flatMap(_.url).foreach(flatUrl => println("flatmap url: " + flatUrl))
  urlList.flatMap(_.protocol).foreach(flatProt => if (!flatProt.isEmpty) println("flatmap protocol: " + flatProt))
}

class MyUrl(val inputUrl: String)  {
//  private val urlSplit: Array[String] = if (inputUrl.isEmpty) Array.fill[String](2)("") else inputUrl.split("://")
  private val urlSplit: Array[String] = Option(inputUrl).getOrElse("").split("://")
  def protocol = Option(urlSplit(0))
  def url = if(urlSplit.length < 2) None else Some(urlSplit(1))
}

object MyUrl {
  def unapply(arg: MyUrl): Option[(String, String)] = {
//    if (!(arg.url == null) && !(arg.protocol == null)) Some((arg.protocol.getOrElse(""), arg.url))
//    else None
    Some((arg.protocol.getOrElse("(undefined)"), arg.url.getOrElse("(undefined)")))
  }
//  def unapplySeq(arg: MyUrl): Option[Seq[String]] = {
//    if (!arg.url.isEmpty && !arg.protocol.isEmpty) Some(Array(arg.protocol, arg.url))
//    else None
//  }
}


