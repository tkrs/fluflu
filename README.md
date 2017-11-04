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

And, so look at this [example](https://github.com/tkrs/fluflu/tree/master/modules/examples/src/main/scala)

## Benchmarks

- fluflu-msgpack

Encoder

```
[info] Benchmark                                              Mode  Cnt       Score       Error  Units
[info] MessagePackerBenchmark.encodeCirceAST                 thrpt   20  128408.501 ±  1866.924  ops/s
[info] MessagePackerBenchmark.encodeInt10                    thrpt   20  315999.334 ±  4285.059  ops/s
[info] MessagePackerBenchmark.encodeInt30                    thrpt   20  143539.468 ± 13205.880  ops/s
[info] MessagePackerBenchmark.encodeLong10                   thrpt   20  343151.509 ± 19161.044  ops/s
[info] MessagePackerBenchmark.encodeLong30                   thrpt   20  145687.290 ±  5606.780  ops/s
[info] MessagePackerBenchmark.encodeNested                   thrpt   20  102871.196 ±  1771.029  ops/s
[info] MessagePackerBenchmark.encodeString1000_30            thrpt   20   10593.208 ±    96.932  ops/s
[info] MessagePackerBenchmark.encodeString1000_30_multibyte  thrpt   20    2395.656 ±    28.052  ops/s
[info] MessagePackerBenchmark.encodeString100_10             thrpt   20  239773.304 ±  5713.490  ops/s
[info] MessagePackerBenchmark.encodeString100_30             thrpt   20  105076.870 ±  1324.373  ops/s
```

Decoder

```
[info] Benchmark                                                Mode  Cnt       Score      Error  Units
[info] MessageUnpackerBenchmark.decodeCirceAST                 thrpt   20  118432.943 ± 1295.806  ops/s
[info] MessageUnpackerBenchmark.decodeInt10                    thrpt   20  261754.469 ± 3832.825  ops/s
[info] MessageUnpackerBenchmark.decodeInt30                    thrpt   20  114688.456 ± 2408.395  ops/s
[info] MessageUnpackerBenchmark.decodeLong10                   thrpt   20  110070.005 ± 1526.381  ops/s
[info] MessageUnpackerBenchmark.decodeLong30                   thrpt   20   41417.811 ± 1033.903  ops/s
[info] MessageUnpackerBenchmark.decodeNested                   thrpt   20  103433.867 ± 2209.280  ops/s
[info] MessageUnpackerBenchmark.decodeString1000_30            thrpt   20   33895.083 ±  402.245  ops/s
[info] MessageUnpackerBenchmark.decodeString1000_30_multibyte  thrpt   20    5183.450 ±   67.308  ops/s
[info] MessageUnpackerBenchmark.decodeString100_10             thrpt   20  214975.326 ± 4839.443  ops/s
[info] MessageUnpackerBenchmark.decodeString100_30             thrpt   20   92710.917 ± 1210.946  ops/s
```

## LICENSE

MIT
