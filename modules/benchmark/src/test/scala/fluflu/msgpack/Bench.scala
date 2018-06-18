package fluflu.msgpack

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(2)
abstract class Bench

class PackBench extends Bench with circe.PackerBench with shapes.PackerBench

class UnpackBench extends Bench with circe.UnpackerBench with shapes.UnpackerBench

class AstBench extends Bench with circe.PackAstBench with shapes.PackAstBench
