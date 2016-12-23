# fluflu
fluent logger for scala

[![wercker status](https://app.wercker.com/status/d754e7976e64af6e1065568b43b27ac7/m "wercker status")](https://app.wercker.com/project/bykey/d754e7976e64af6e1065568b43b27ac7)

[![codecov.io](http://codecov.io/github/tkrs/fluflu/coverage.svg?branch=master)](http://codecov.io/github/tkrs/fluflu?branch=master)

## Usage

Add to your `build.sbt`

```scala
libraryDependencies += "com.github.tkrs" %% "fluflu-queue" % "0.5.6"
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
MessagePackerBenchmark.encodeCirceAST                   thrpt  100    21276.120 ±   323.674  ops/s
MessagePackerBenchmark.encodeInt10                      thrpt  100   134450.609 ±  4078.495  ops/s
MessagePackerBenchmark.encodeInt30                      thrpt  100    52893.677 ±  3841.534  ops/s
MessagePackerBenchmark.encodeLong10                     thrpt  100   144049.873 ±  4982.727  ops/s
MessagePackerBenchmark.encodeLong30                     thrpt  100    55832.522 ±  1392.915  ops/s
MessagePackerBenchmark.encodeString1000_30              thrpt  100     2895.452 ±   105.649  ops/s
MessagePackerBenchmark.encodeString1000_30_multibyte    thrpt  100      767.023 ±     9.856  ops/s
MessagePackerBenchmark.encodeString100_10               thrpt  100    64048.843 ±   362.516  ops/s
MessagePackerBenchmark.encodeString100_30               thrpt  100    19806.988 ±   102.978  ops/s
```

Decoder

```
Benchmark                                                Mode  Cnt        Score       Error  Units
MessageUnpackerBenchmark.decodeCirceAST                 thrpt  100   609080.108 ±  4209.928  ops/s
MessageUnpackerBenchmark.decodeInt10                    thrpt  100  1717002.663 ± 36772.146  ops/s
MessageUnpackerBenchmark.decodeInt30                    thrpt  100   705608.670 ±  3973.877  ops/s
MessageUnpackerBenchmark.decodeLong10                   thrpt  100   887895.077 ±  9118.823  ops/s
MessageUnpackerBenchmark.decodeLong30                   thrpt  100   701917.870 ± 12142.367  ops/s
MessageUnpackerBenchmark.decodeString1000_30            thrpt  100   567494.589 ±  5793.185  ops/s
MessageUnpackerBenchmark.decodeString1000_30_multibyte  thrpt  100   556861.228 ± 18426.292  ops/s
MessageUnpackerBenchmark.decodeString100_10             thrpt  100   579314.084 ±  8949.473  ops/s
MessageUnpackerBenchmark.decodeString100_30             thrpt  100   556954.006 ± 17998.414  ops/s
```

## TODO

- TEST

## LICENSE

MIT
