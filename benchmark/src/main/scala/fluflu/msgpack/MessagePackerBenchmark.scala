package fluflu.msgpack

import java.util.concurrent.TimeUnit

import io.circe.generic.auto._
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 5)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(2)
class MessagePackerBenchmark extends TestData {
  import pack._

  @Benchmark def encodeInt10: Either[Throwable, Array[Byte]] = packer.encode(`int max 10`)

  @Benchmark def encodeLong10: Either[Throwable, Array[Byte]] = packer.encode(`long max 10`)

  @Benchmark def encodeString100_10: Either[Throwable, Array[Byte]] =
    packer.encode(`string 100 10`)

  @Benchmark def encodeInt30: Either[Throwable, Array[Byte]] = packer.encode(`int max 30`)

  @Benchmark def encodeLong30: Either[Throwable, Array[Byte]] = packer.encode(`long max 30`)

  @Benchmark def encodeString100_30: Either[Throwable, Array[Byte]] =
    packer.encode(`string 100 30`)

  @Benchmark def encodeString1000_30: Either[Throwable, Array[Byte]] =
    packer.encode(`string 1000 30`)

  @Benchmark def encodeString1000_30_multibyte: Either[Throwable, Array[Byte]] =
    packer.encode(`string 1000 30 multibyte`)

  @Benchmark def encodeCirceAST: Either[Throwable, Array[Byte]] = packer.encode(circeAST)
}
