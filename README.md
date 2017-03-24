# fluflu
Yet another fluentd logger for scala

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
Benchmark                                                Mode  Cnt       Score      Error  Units
MessageUnpackerBenchmark.decodeCirceAST                 thrpt   10   56195.041 ±  889.966  ops/s
MessageUnpackerBenchmark.decodeInt10                    thrpt   10  203983.660 ± 7464.717  ops/s
MessageUnpackerBenchmark.decodeInt30                    thrpt   10   56725.173 ± 2688.908  ops/s
MessageUnpackerBenchmark.decodeLong10                   thrpt   10   64396.997 ±  588.559  ops/s
MessageUnpackerBenchmark.decodeLong30                   thrpt   10   20645.105 ± 1467.250  ops/s
MessageUnpackerBenchmark.decodeString1000_30            thrpt   10   23706.552 ±  365.460  ops/s
MessageUnpackerBenchmark.decodeString1000_30_multibyte  thrpt   10    4427.903 ±   36.471  ops/s
MessageUnpackerBenchmark.decodeString100_10             thrpt   10  153727.843 ± 5463.203  ops/s
MessageUnpackerBenchmark.decodeString100_30             thrpt   10   46904.645 ± 1960.118  ops/s
```

## LICENSE

MIT
