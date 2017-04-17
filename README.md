# fluflu
Yet another fluentd logger for scala

[![wercker status](https://app.wercker.com/status/d754e7976e64af6e1065568b43b27ac7/m "wercker status")](https://app.wercker.com/project/bykey/d754e7976e64af6e1065568b43b27ac7)

[![codecov.io](http://codecov.io/github/tkrs/fluflu/coverage.svg?branch=master)](http://codecov.io/github/tkrs/fluflu?branch=master)

## How to use it

Add to your `build.sbt`

```scala
libraryDependencies += "com.github.tkrs" %% "fluflu-queue" % "0.7.0"
```

And, so look at this [example](https://github.com/tkrs/fluflu/tree/master/examples/src/main/scala)

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
