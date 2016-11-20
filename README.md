# fluflu
fluent logger for scala

[![wercker status](https://app.wercker.com/status/d754e7976e64af6e1065568b43b27ac7/m "wercker status")](https://app.wercker.com/project/bykey/d754e7976e64af6e1065568b43b27ac7)

[![codecov.io](http://codecov.io/github/tkrs/fluflu/coverage.svg?branch=master)](http://codecov.io/github/tkrs/fluflu?branch=master)

## Usage

Add to your `build.sbt`

```scala
libraryDependencies += "com.github.tkrs" %% "fluflu-queue" % "0.5.5"
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
    initialBufferSize = 1024,
    initialDelay = 500,
    delay = 500,
    delayTimeUnit = TimeUnit.MILLISECONDS,
    terminationDelay = 10,
    terminationDelayTimeUnit = TimeUnit.SECONDS
  )

  val ccc: CCC = CCC(0, "foo", "", Int.MaxValue, Map("name" -> "fluflu"), Seq(1.2, Double.MaxValue, Double.MinValue))
  async.push(ccc)

  Thread.sleep(1000)

  async.close()
}
```

## TODO

- TEST

## LICENSE

MIT
