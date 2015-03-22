package com.github.plokhotnyuk.scala_vs_java

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

import scala.annotation.tailrec

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
class ScalaSumOfArithmeticSeries {
  @Param(Array("10", "100", "1000", "10000"))
  var n: Int = _

  @Benchmark
  def formula(): Int = ((n + 1) * n) >> 1

  @Benchmark
  def loop(): Int = {
    @tailrec
    def loop(n: Int, s: Int): Int = if (n > 0) loop(n - 1, s + n) else s

    loop(n, 0)
  }
}