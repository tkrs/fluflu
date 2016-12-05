package fluflu.msgpack

import java.util.concurrent.TimeUnit

import io.circe.generic.auto._
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class MessagePackerBenchmark extends TestData {

  private[this] val packer = MessagePacker()

  @Benchmark def decodeInt10: Unit = packer.encode(`int max 10`)

  @Benchmark def decodeLong10: Unit = packer.encode(`long max 10`)

  @Benchmark def decodeString100_10: Unit = packer.encode(`string 100 10`)

  @Benchmark def decodeInt30: Unit = packer.encode(`int max 30`)

  @Benchmark def decodeLong30: Unit = packer.encode(`long max 30`)

  @Benchmark def decodeString100_30: Unit = packer.encode(`string 100 30`)

  @Benchmark def decodeString1000_30: Unit = packer.encode(`string 1000 30`)

  @Benchmark def decodeString1000_30_multibyte: Unit = packer.encode(`string 1000 30 multibyte`)

  @Benchmark def decodeCirceAST: Unit = packer.encode(circeAST)

}
