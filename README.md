# fluflu
Yet another fluentd logger for scala

[![wercker status](https://app.wercker.com/status/d754e7976e64af6e1065568b43b27ac7/m "wercker status")](https://app.wercker.com/project/bykey/d754e7976e64af6e1065568b43b27ac7)

[![codecov.io](http://codecov.io/github/tkrs/fluflu/coverage.svg?branch=master)](http://codecov.io/github/tkrs/fluflu?branch=master)

## How to use it

Add to your `build.sbt`

```scala
libraryDependencies += "com.github.tkrs" %% "fluflu-queue" % "0.8.0"
```

And, so look at this [example](https://github.com/tkrs/fluflu/tree/master/examples/src/main/scala)

## Benchmarks

- fluflu-msgpack

Encoder

```
[info] Benchmark                                                Mode  Cnt       Score       Error  Units
[info] MessagePackerBenchmark.encodeCirceAST                   thrpt   10   22350.084 ±   526.572  ops/s
[info] MessagePackerBenchmark.encodeInt10                      thrpt   10  184904.815 ± 11332.698  ops/s
[info] MessagePackerBenchmark.encodeInt30                      thrpt   10   64691.319 ±   897.389  ops/s
[info] MessagePackerBenchmark.encodeLong10                     thrpt   10  165388.501 ± 15679.018  ops/s
[info] MessagePackerBenchmark.encodeLong30                     thrpt   10   59541.444 ±   715.820  ops/s
[info] MessagePackerBenchmark.encodeString1000_30              thrpt   10    3107.516 ±    42.144  ops/s
[info] MessagePackerBenchmark.encodeString1000_30_multibyte    thrpt   10     783.734 ±     9.827  ops/s
[info] MessagePackerBenchmark.encodeString100_10               thrpt   10   64000.552 ±  1384.534  ops/s
[info] MessagePackerBenchmark.encodeString100_30               thrpt   10   19990.773 ±   528.701  ops/s
```

Decoder

```
[info] Benchmark                                                Mode  Cnt       Score       Error  Units
[info] MessageUnpackerBenchmark.decodeCirceAST                 thrpt   10   60774.690 ±  1061.519  ops/s
[info] MessageUnpackerBenchmark.decodeInt10                    thrpt   10  217220.767 ±  9616.568  ops/s
[info] MessageUnpackerBenchmark.decodeInt30                    thrpt   10   60322.357 ±  1446.670  ops/s
[info] MessageUnpackerBenchmark.decodeLong10                   thrpt   10   67641.680 ±   962.903  ops/s
[info] MessageUnpackerBenchmark.decodeLong30                   thrpt   10   21795.237 ±   192.654  ops/s
[info] MessageUnpackerBenchmark.decodeString1000_30            thrpt   10   23987.411 ±   313.592  ops/s
[info] MessageUnpackerBenchmark.decodeString1000_30_multibyte  thrpt   10    4532.262 ±   167.964  ops/s
[info] MessageUnpackerBenchmark.decodeString100_10             thrpt   10  173045.726 ±  2893.257  ops/s
[info] MessageUnpackerBenchmark.decodeString100_30             thrpt   10   48516.540 ±  1189.041  ops/s
```

## LICENSE

MIT
