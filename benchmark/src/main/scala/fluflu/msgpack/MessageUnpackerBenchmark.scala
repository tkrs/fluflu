package fluflu.msgpack

import java.util.concurrent.TimeUnit

import io.circe.{ Error, Json }
import io.circe.generic.auto._
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 5)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(2)
class MessageUnpackerBenchmark extends TestData {
  import models._
  import unpack._

  @Benchmark
  def decodeInt10: Either[Error, Int10] = unpacker(`int max 10`.duplicate()).decode[Int10]

  @Benchmark
  def decodeLong10: Either[Error, Long10] = unpacker(`long max 10`.slice()).decode[Long10]

  @Benchmark
  def decodeString100_10: Either[Error, String10] = unpacker(`string 100 10`.duplicate()).decode[String10]

  @Benchmark
  def decodeInt30: Either[Error, Int30] = unpacker(`int max 30`.duplicate()).decode[Int30]

  @Benchmark
  def decodeLong30: Either[Error, Long30] = unpacker(`long max 30`.duplicate()).decode[Long30]

  @Benchmark
  def decodeString100_30: Either[Error, String30] = unpacker(`string 100 30`.duplicate()).decode[String30]

  @Benchmark
  def decodeString1000_30: Either[Error, String30] = unpacker(`string 1000 30`.duplicate()).decode[String30]

  @Benchmark
  def decodeString1000_30_multibyte: Either[Error, String30] = unpacker(`string 1000 30 multibyte`.duplicate()).decode[String30]

  @Benchmark
  def decodeCirceAST: Either[Error, Json] = unpacker(circeAST.duplicate()).decode[Json]

}
