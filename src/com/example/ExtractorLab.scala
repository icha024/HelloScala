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

  def printProtoType(url: MyUrl): Unit = {
    url match {
      case MyUrl(protocol, _) if protocol.equals("http") => println("It's a HTTP endpoint")
      case MyUrl(protocol, _) if protocol.equals("ftp") => println("FTP is the way")
      case MyUrl(protocol, _) => println("I have no idea what this is: " + protocol)
      case _ => println("Unknown input")
    }
  }
}

class MyUrl(val inputUrl: String)  {
  private val urlSplit: Array[String] = if (inputUrl.isEmpty) Array.fill[String](2)("") else inputUrl.split("://")
  def protocol = urlSplit(0)
  def url = urlSplit(1)
}

object MyUrl {
  def unapply(arg: MyUrl): Option[(String, String)] = {
    if (!(arg.url == null) && !(arg.protocol == null)) Some((arg.protocol, arg.url))
    else None
  }
//  def unapplySeq(arg: MyUrl): Option[Seq[String]] = {
//    if (!arg.url.isEmpty && !arg.protocol.isEmpty) Some(Array(arg.protocol, arg.url))
//    else None
//  }
}


