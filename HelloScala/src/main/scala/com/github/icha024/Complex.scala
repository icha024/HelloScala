package com.github.icha024

class Complex (var real: Double, var imaginary: Double) {
  def re = real
  def im = imaginary
  def im_= (newIm: Double): Unit = {
    imaginary = newIm
  }

  var zzz = 888.13;

  override def toString: String = "Real: " + re + ", Im: " + im + ", ZZZ: " + zzz
}
