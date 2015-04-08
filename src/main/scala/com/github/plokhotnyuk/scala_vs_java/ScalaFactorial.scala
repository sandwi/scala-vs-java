package com.github.plokhotnyuk.scala_vs_java

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
class ScalaFactorial {
  @Param(Array("10", "100", "1000", "10000"))
  var n: Int = _

  @Benchmark
  def loop(): BigInt = if (n > 20) loop(1, n) else fastLoop(1, n)

  @Benchmark
  def recursion(): BigInt = if (n > 20) recursion(1, n) else fastLoop(1, n)

  @Benchmark
  def recursionPar(): BigInt = if (n > 20) recursionPar(1, n) else fastLoop(1, n)

  @Benchmark
  def split(): BigInt = if (n > 180) split(n) else if (n > 20) recursion(1, n) else fastLoop(1, n)

  @annotation.tailrec
  private def fastLoop(n1: Int, n2: Int, p: Long = 1): Long = if (n2 > n1) fastLoop(n1, n2 - 1, p * n2) else p

  private def loop(n1: Int, n2: Int): BigInt = {
    @annotation.tailrec
    def loop(n1: Int, n2: Int, l: Long, p: Long, r: BigInt): BigInt =
      if (n1 <= n2) {
        if (p < l) loop(n1 + 1, n2, l, p * n1, r)
        else loop(n1 + 1, n2, l, n1, r * p)
      } else r * p

    loop(n1, n2, Long.MaxValue >> (32 - Integer.numberOfLeadingZeros(n2)), 1, 1)
  }

  private def recursion(n1: Int, n2: Int): BigInt =
    if (n2 - n1 < 65) loop(n1, n2)
    else {
      val nm = (n1 + n2) >> 1
      recursion(nm + 1, n2) * recursion(n1, nm)
    }

  private def recursionPar(n1: Int, n2: Int): BigInt =
    if (n2 - n1 < 700) recursion(n1, n2)
    else {
      val nm = (n1 + n2) >> 1
      val f = Future(recursionPar(nm + 1, n2))
      recursionPar(n1, nm) * Await.result(f, Duration.Inf)
    }

  private def split(n: Int): BigInt = {
    @annotation.tailrec
    def loop2(n1: Int, n2: Int, l: Long, p: Long, r: BigInt): BigInt =
      if (n1 <= n2) {
        if (p < l) loop2(n1 + 2, n2, l, p * n1, r)
        else loop2(n1 + 2, n2, l, n1, r * p)
      } else r * p

    def recursion2(n1: Int, n2: Int): BigInt =
      if (n2 - n1 < 65) loop2(n1, n2, Long.MaxValue >> (32 - Integer.numberOfLeadingZeros(n2)), 1, 1)
      else {
        val nm = ((n1 + n2) >> 1) | 1
        recursion2(nm, n2) * recursion2(n1, nm - 2)
      }

    @annotation.tailrec
    def split(n: Int, i: Int, s: Int, o: Int, p: BigInt, r: BigInt): BigInt =
      if (i >= 0) {
        val h = n >> i
        val o1 = (h - 1) | 1
        if (o < o1) {
          val p1 = p * recursion2(o + 2, o1)
          split(n, i - 1, s + h, o1, p1, r * p1)
        } else split(n, i - 1, s + h, o1, p, r)
      } else r << s

    split(n, 31 - Integer.numberOfLeadingZeros(n), -n, 1, 1, 1)
  }
}

/*
Timestamp        S0C    S1C    S0U    S1U      EC       EU        OC         OU MC        MU       CCSC     CCSU    YGC      YGCT  FGC       FGCT   GCT
       587444.6 314560.0 314560.0 54323.1  0.0   2516608.0 1783007.4 13631488.0 8663961.3  162116.0 157305.4 19476.0 18530.8  60732 4282.272  70     62.730 4345.002
       587445.1 314560.0 314560.0 54323.1  0.0   2516608.0 1862695.8 13631488.0 8663961.3  162116.0 157305.4 19476.0 18530.8  60732 4282.272  70     62.730 4345.002
       587445.6 314560.0 314560.0 54323.1  0.0   2516608.0 1933960.2 13631488.0 8663961.3  162116.0 157305.4 19476.0 18530.8  60732 4282.272  70     62.730 4345.002
       587446.1 314560.0 314560.0 54323.1  0.0   2516608.0 2009226.8 13631488.0 8663961.3  162116.0 157305.4 19476.0 18530.8  60732 4282.272  70     62.730 4345.002
       587446.6 314560.0 314560.0 54323.1  0.0   2516608.0 2115092.5 13631488.0 8663961.3  162116.0 157305.4 19476.0 18530.8  60732 4282.272  70     62.730 4345.002
       587447.1 314560.0 314560.0 54323.1  0.0   2516608.0 2237246.5 13631488.0 8663961.3  162116.0 157305.4 19476.0 18530.8  60732 4282.272  70     62.730 4345.002
       587447.6 314560.0 314560.0 54323.1  0.0   2516608.0 2355277.8 13631488.0 8663961.3  162116.0 157305.4 19476.0 18530.8  60732 4282.272  70     62.730 4345.002
       587448.1 314560.0 314560.0 54323.1  0.0   2516608.0 2488094.7 13631488.0 8663961.3  162116.0 157305.4 19476.0 18530.8  60732 4282.272  70     62.730 4345.002
       587448.6 314560.0 314560.0 54323.1 45286.4 2516608.0 2516608.0 13631488.0 8667016.9  162116.0 157305.4 19476.0 18530.8  60733 4282.272  70     62.730 4345.002
       587449.1 314560.0 314560.0 54323.1 51664.7 2516608.0 2516608.0 13631488.0 8667407.5  162116.0 157305.4 19476.0 18530.8  60733 4282.272  70     62.730 4345.002
       587449.6 314560.0 314560.0 54323.1 51664.7 2516608.0 2516608.0 13631488.0 8667407.5  162116.0 157305.4 19476.0 18530.8  60733 4282.272  70     62.730 4345.002
       587450.1 314560.0 314560.0 54323.1 51664.7 2516608.0 2516608.0 13631488.0 8667407.5  162116.0 157305.4 19476.0 18530.8  60733 4282.272  70     62.730 4345.002
       587450.6 314560.0 314560.0 54323.1 51664.7 2516608.0 2516608.0 13631488.0 8667407.5  162116.0 157305.4 19476.0 18530.8  60733 4282.272  70     62.730 4345.002
       587451.0 314560.0 314560.0 54323.1 51664.7 2516608.0 2516608.0 13631488.0 8667407.5  162116.0 157305.4 19476.0 18530.8  60733 4282.272  70     62.730 4345.002
       587451.5 314560.0 314560.0 54323.1 51664.7 2516608.0 2516608.0 13631488.0 8667407.5  162116.0 157305.4 19476.0 18530.8  60733 4282.272  70     62.730 4345.002
       587452.0 314560.0 314560.0 54323.1 51664.7 2516608.0 2516608.0 13631488.0 8667407.5  162116.0 157305.4 19476.0 18530.8  60733 4282.272  70     62.730 4345.002
 */