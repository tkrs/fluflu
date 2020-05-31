# fluflu

Yet another fluentd logger for scala

[![Build Status](https://travis-ci.com/tkrs/fluflu.svg?branch=master)](https://travis-ci.com/tkrs/fluflu)
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
