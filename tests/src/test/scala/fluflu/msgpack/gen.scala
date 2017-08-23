package fluflu
package msgpack

import org.scalacheck.Gen

object gen {

  val int64Min = BigInt("-9223372036854775808")
  val uint64Max = BigInt("18446744073709551615")

  val genBigInt: Gen[BigInt] =
    Gen.oneOf(Gen.const(int64Min),
              Gen.negNum[Long].map(BigInt.apply),
              Gen.posNum[Long].map(BigInt.apply),
              Gen.const(uint64Max))
}
