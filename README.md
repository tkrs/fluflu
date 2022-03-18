# fluflu

Yet another fluentd logger for scala

[![CI](https://github.com/tkrs/fluflu/workflows/CI/badge.svg)](https://github.com/tkrs/fluflu/actions?query=workflow%3ACI)
[![Release](https://github.com/tkrs/fluflu/workflows/Release/badge.svg)](https://github.com/tkrs/fluflu/actions?query=workflow%3ARelease)
[![Tagging](https://github.com/tkrs/fluflu/actions/workflows/tagging.yml/badge.svg)](https://github.com/tkrs/fluflu/actions/workflows/tagging.yml)
[![codecov.io](http://codecov.io/github/tkrs/fluflu/coverage.svg?branch=master)](http://codecov.io/github/tkrs/fluflu?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.tkrs/fluflu-core_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.tkrs/fluflu-core_2.12)

## How to use it

Add to your `build.sbt`

```scala
libraryDependencies ++= Seq(
  "fluflu-core",
  "fluflu-msgpack-mess"
).map(m => "com.github.tkrs" %% m % "x.y.z")
```

And, so look at this [example](https://github.com/tkrs/fluflu/tree/master/modules/examples/src/main/scala)

## LICENSE

MIT
