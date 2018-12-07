# fluflu
Yet another fluentd logger for scala

[![Build Status](https://travis-ci.org/tkrs/fluflu.svg?branch=master)](https://travis-ci.org/tkrs/fluflu)
[![codecov.io](http://codecov.io/github/tkrs/fluflu/coverage.svg?branch=master)](http://codecov.io/github/tkrs/fluflu?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.tkrs/fluflu-core_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.tkrs/fluflu-core_2.12)

## How to use it

Add to your `build.sbt`

```scala
libraryDependencies += "com.github.tkrs" %% "fluflu-core" % "x.y.z"
// and
libraryDependencies += "com.github.tkrs" %% "fluflu-msgpack-circe" % "x.y.z"
// or
libraryDependencies += "com.github.tkrs" %% "fluflu-msgpack-mess" % "x.y.z"
```

And, so look at this [example](https://github.com/tkrs/fluflu/tree/master/modules/examples/src/main/scala)

## LICENSE

MIT
