# fluflu
Yet another fluentd logger for scala

[![Build Status](https://travis-ci.org/tkrs/fluflu.svg?branch=master)](https://travis-ci.org/tkrs/fluflu)
[![codecov.io](http://codecov.io/github/tkrs/fluflu/coverage.svg?branch=master)](http://codecov.io/github/tkrs/fluflu?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.tkrs/fluflu-core_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.tkrs/fluflu-core_2.12)

## How to use it

Add to your `build.sbt`

```scala
libraryDependencies ++= Seq(
  "com.github.tkrs" %% "fluflu-core" % "x.y.z",
  "com.github.tkrs" %% "fluflu-msgpack-circe" % "x.y.z"
)
```

And, so look at this [example](https://github.com/tkrs/fluflu/tree/master/modules/examples/src/main/scala)

## Benchmarks

- fluflu-msgpack
fluflu.msgpackr

```
[info] Benchmark                                                Mode  Cnt       Score       Error   Units
[info] MessagePackerBenchmark.encodeCirceAST                   thrpt   20  133167.141 ±  2550.616   ops/s
[info] MessagePackerBenchmark.encodeInt10                      thrpt   20  370109.732 ±  9878.608   ops/s
[info] MessagePackerBenchmark.encodeInt30                      thrpt   20  143915.592 ± 29259.983   ops/s
[info] MessagePackerBenchmark.encodeLong10                     thrpt   20  340296.568 ±  5333.504   ops/s
[info] MessagePackerBenchmark.encodeLong30                     thrpt   20  153223.090 ±  2857.427   ops/s
[info] MessagePackerBenchmark.encodeNested                     thrpt   20  103858.391 ±  2043.339   ops/s
[info] MessagePackerBenchmark.encodeString1000_30              thrpt   20   10661.082 ±   181.357   ops/s
[info] MessagePackerBenchmark.encodeString1000_30_multibyte    thrpt   20    2340.150 ±    51.082   ops/s
[info] MessagePackerBenchmark.encodeString100_10               thrpt   20  253301.605 ±  6416.145   ops/s
[info] MessagePackerBenchmark.encodeString100_30               thrpt   20  107762.022 ±  2849.293   ops/s
```

Decoder

```
[info] Benchmark                                                Mode  Cnt       Score       Error   Units
[info] MessageUnpackerBenchmark.decodeCirceAST                 thrpt   20  128765.305 ±  7875.374   ops/s
[info] MessageUnpackerBenchmark.decodeInt10                    thrpt   20  292540.216 ±  8016.177   ops/s
[info] MessageUnpackerBenchmark.decodeInt30                    thrpt   20  130384.440 ±  5428.657   ops/s
[info] MessageUnpackerBenchmark.decodeLong10                   thrpt   20  116669.811 ±  2423.805   ops/s
[info] MessageUnpackerBenchmark.decodeLong30                   thrpt   20   42903.651 ±  1197.532   ops/s
[info] MessageUnpackerBenchmark.decodeNested                   thrpt   20  105284.983 ±  2737.642   ops/s
[info] MessageUnpackerBenchmark.decodeString1000_30            thrpt   20   35019.191 ±   808.341   ops/s
[info] MessageUnpackerBenchmark.decodeString1000_30_multibyte  thrpt   20    5053.968 ±    49.745   ops/s
[info] MessageUnpackerBenchmark.decodeString100_10             thrpt   20  221799.362 ±  4139.716   ops/s
[info] MessageUnpackerBenchmark.decodeString100_30             thrpt   20   96945.679 ±  3381.020   ops/s
```

## LICENSE

MIT
