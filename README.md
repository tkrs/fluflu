# fluflu
Yet another fluentd logger for scala

[![Build Status](https://travis-ci.org/tkrs/fluflu.svg?branch=master)](https://travis-ci.org/tkrs/fluflu)
[![codecov.io](http://codecov.io/github/tkrs/fluflu/coverage.svg?branch=master)](http://codecov.io/github/tkrs/fluflu?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.tkrs/fluflu-core_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.tkrs/fluflu-core_2.12)

## How to use it

Add to your `build.sbt`

```scala
libraryDependencies += "com.github.tkrs" %% "fluflu-msgpack-circe" % "${version}"
libraryDependencies += "com.github.tkrs" %% "fluflu-monix" % "${version}"
libraryDependencies += "com.github.tkrs" %% "fluflu-queue" % "${version}"
```

And, so look at this [example](https://github.com/tkrs/fluflu/tree/master/examples/src/main/scala)

## Benchmarks

- fluflu-msgpack

Encoder

```
[info] Benchmark                                                Mode  Cnt       Score       Error  Units
[info] MessagePackerBenchmark.encodeCirceAST                   thrpt   10   22228.283 ±   535.242  ops/s
[info] MessagePackerBenchmark.encodeInt10                      thrpt   10  227321.925 ± 23375.573  ops/s
[info] MessagePackerBenchmark.encodeInt30                      thrpt   10   88871.449 ±  2466.712  ops/s
[info] MessagePackerBenchmark.encodeLong10                     thrpt   10  233073.532 ± 30711.071  ops/s
[info] MessagePackerBenchmark.encodeLong30                     thrpt   10   75642.715 ±  6910.070  ops/s
[info] MessagePackerBenchmark.encodeString1000_30              thrpt   10    3234.119 ±    67.094  ops/s
[info] MessagePackerBenchmark.encodeString1000_30_multibyte    thrpt   10     790.470 ±     7.313  ops/s
[info] MessagePackerBenchmark.encodeString100_10               thrpt   10   69304.291 ±  2756.068  ops/s
[info] MessagePackerBenchmark.encodeString100_30               thrpt   10   21676.775 ±   480.649  ops/s
```

Decoder

```
[info] Benchmark                                                Mode  Cnt       Score       Error  Units
[info] MessageUnpackerBenchmark.decodeCirceAST                 thrpt   10   75711.661 ±  1383.031  ops/s
[info] MessageUnpackerBenchmark.decodeInt10                    thrpt   10  311051.383 ± 10202.545  ops/s
[info] MessageUnpackerBenchmark.decodeInt30                    thrpt   10   81559.446 ±  1916.689  ops/s
[info] MessageUnpackerBenchmark.decodeLong10                   thrpt   10   76244.458 ±  2299.118  ops/s
[info] MessageUnpackerBenchmark.decodeLong30                   thrpt   10   23108.003 ±   231.772  ops/s
[info] MessageUnpackerBenchmark.decodeString1000_30            thrpt   10   27111.714 ±   354.472  ops/s
[info] MessageUnpackerBenchmark.decodeString1000_30_multibyte  thrpt   10    4687.042 ±   181.548  ops/s
[info] MessageUnpackerBenchmark.decodeString100_10             thrpt   10  223152.327 ±  7625.497  ops/s
[info] MessageUnpackerBenchmark.decodeString100_30             thrpt   10   61402.145 ±   697.839  ops/s
```

## LICENSE

MIT
