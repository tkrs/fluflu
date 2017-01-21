# fluflu
fluent logger for scala

[![wercker status](https://app.wercker.com/status/d754e7976e64af6e1065568b43b27ac7/m "wercker status")](https://app.wercker.com/project/bykey/d754e7976e64af6e1065568b43b27ac7)

[![codecov.io](http://codecov.io/github/tkrs/fluflu/coverage.svg?branch=master)](http://codecov.io/github/tkrs/fluflu?branch=master)

## Usage

Add to your `build.sbt`

```scala
libraryDependencies += "com.github.tkrs" %% "fluflu-queue" % "0.5.10"
```

## Example
```scala
  case class CCC(
    i: Int,
    ttt: String,
    uuu: String,
    sss: Int, mmm: Map[String, String],
    ggg: Seq[Double]
  )

  implicit val clock: Clock = Clock.systemUTC()

  val rnd: Random = new Random(System.nanoTime())
  val reconnectionBackoff: Backoff =
    ExponentialBackoff(Duration.ofNanos(500), Duration.ofSeconds(5), rnd)
  val rewriteBackoff: Backoff =
    ExponentialBackoff(Duration.ofNanos(500), Duration.ofSeconds(5), rnd)

  val messenger = fluflu.DefaultMessenger(
    host = "127.0.0.1",
    port = 24224,
    reconnectionTimeout = Duration.ofSeconds(10),
    rewriteTimeout = Duration.ofSeconds(10),
    reconnectionBackoff = reconnectionBackoff,
    rewriteBackoff = rewriteBackoff
  )

  val async: Async = Async(
    messenger = messenger,
    initialBufferSize = 2048,
    initialDelay = Duration.ofMillis(50),
    delay = Duration.ofMillis(500),
    terminationDelay = Duration.ofSeconds(10)
  )

  val ccc: CCC = CCC(0, "foo", "", Int.MaxValue, Map("name" -> "fluflu"), Seq(1.2, Double.MaxValue, Double.MinValue))
  async.push(ccc)

  Thread.sleep(1000)

  async.close()
}
```

## Benchmarks

- fluflu-msgpack

Encoder

```
Benchmark                                                Mode  Cnt        Score       Error  Units
MessagePackerBenchmark.encodeCirceAST                   thrpt   30    22302.420 ±   218.314  ops/s
MessagePackerBenchmark.encodeInt10                      thrpt   30   156721.194 ±  1688.422  ops/s
MessagePackerBenchmark.encodeInt30                      thrpt   30    59962.061 ±  3885.709  ops/s
MessagePackerBenchmark.encodeLong10                     thrpt   30   146390.636 ±  5752.345  ops/s
MessagePackerBenchmark.encodeLong30                     thrpt   30    58957.480 ±  1388.892  ops/s
MessagePackerBenchmark.encodeString1000_30              thrpt   30     3213.112 ±    31.397  ops/s
MessagePackerBenchmark.encodeString1000_30_multibyte    thrpt   30      782.617 ±     8.658  ops/s
MessagePackerBenchmark.encodeString100_10               thrpt   30    60195.681 ±   545.277  ops/s
MessagePackerBenchmark.encodeString100_30               thrpt   30    19866.150 ±   141.902  ops/s
```

Decoder

```
Benchmark                                                Mode  Cnt        Score       Error  Units
MessageUnpackerBenchmark.decodeCirceAST                 thrpt   30   622342.441 ± 12140.401  ops/s
MessageUnpackerBenchmark.decodeInt10                    thrpt   30  1836780.120 ± 15702.416  ops/s
MessageUnpackerBenchmark.decodeInt30                    thrpt   30   843618.908 ±  7494.512  ops/s
MessageUnpackerBenchmark.decodeLong10                   thrpt   30   937996.728 ± 10616.039  ops/s
MessageUnpackerBenchmark.decodeLong30                   thrpt   30   839720.811 ±  8092.234  ops/s
MessageUnpackerBenchmark.decodeString1000_30            thrpt   30   561789.541 ± 13513.640  ops/s
MessageUnpackerBenchmark.decodeString1000_30_multibyte  thrpt   30   573531.092 ±  9846.417  ops/s
MessageUnpackerBenchmark.decodeString100_10             thrpt   30   592889.941 ± 12490.031  ops/s
MessageUnpackerBenchmark.decodeString100_30             thrpt   30   570698.816 ± 16276.081  ops/s
```

## LICENSE

MIT
