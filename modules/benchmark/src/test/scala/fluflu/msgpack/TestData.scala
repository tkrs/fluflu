package fluflu.msgpack

import java.nio.ByteBuffer

import cats.syntax.either._
import fluflu.msgpack.circe.{MessagePacker, MessageUnpacker}
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.msgpack.core.MessagePack

class TestData {
  import models._

  val packer   = MessagePacker(MessagePack.DEFAULT_PACKER_CONFIG)
  val unpacker = MessageUnpacker(_: ByteBuffer, MessagePack.DEFAULT_UNPACKER_CONFIG)

  val string100: String       = "a" * 100
  val string1000: String      = "z" * 1000
  val string1000multi: String = "\uD842\uDF9F" * 1000

  type Prof = Fix[ProfF]

  object pack {
    val fixType: Prof =
      Fix(
        ProfF(
          "Hilbert",
          1885,
          List(
            Fix(ProfF("Ackermann", 1925, Nil)),
            Fix(ProfF("Curry", 1930, Nil)),
            Fix(
              ProfF("Weyl",
                    1908,
                    List(
                      Fix(
                        ProfF("Mac Lane",
                              1934,
                              List(
                                Fix(ProfF("Howard", 1956, Nil)),
                                Fix(ProfF("Awodey", 1997, Nil))
                              ))))))
          )
        ))

    val `int max 10` = Int10(Int.MaxValue,
                             Int.MaxValue,
                             Int.MaxValue,
                             Int.MaxValue,
                             Int.MaxValue,
                             Int.MaxValue,
                             Int.MaxValue,
                             Int.MaxValue,
                             Int.MaxValue,
                             Int.MaxValue)
    val `long max 10` = Long10(Long.MaxValue,
                               Long.MaxValue,
                               Long.MaxValue,
                               Long.MaxValue,
                               Long.MaxValue,
                               Long.MaxValue,
                               Long.MaxValue,
                               Long.MaxValue,
                               Long.MaxValue,
                               Long.MaxValue)
    val `string 100 10` = String10(string100,
                                   string100,
                                   string100,
                                   string100,
                                   string100,
                                   string100,
                                   string100,
                                   string100,
                                   string100,
                                   string100)

    val `int max 30` = Int30(
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MaxValue,
      Int.MinValue
    )
    val `long max 30` = Long30(
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MaxValue,
      Long.MinValue
    )
    val `string 100 30` = String30(
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100,
      string100
    )
    val `string 1000 30` = String30(
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string1000,
      string100
    )
    val `string 1000 30 multibyte` = String30(
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string1000multi,
      string100
    )

    val circeAST: Json = Json.arr(Json.fromString("prefix.tag"),
                                  Json.fromLong(System.currentTimeMillis() / 1000),
                                  `string 100 30`.asJson)
  }

  object unpack {
    val fixType = ByteBuffer.wrap(packer.encode(pack.fixType).toTry.get)

    val `int max 10`: ByteBuffer = ByteBuffer.wrap(
      packer
        .encode(
          Int10(Int.MaxValue,
                Int.MaxValue,
                Int.MaxValue,
                Int.MaxValue,
                Int.MaxValue,
                Int.MaxValue,
                Int.MaxValue,
                Int.MaxValue,
                Int.MaxValue,
                Int.MaxValue))
        .getOrElse(Array.empty[Byte]))
    val `long max 10` = ByteBuffer.wrap(
      packer
        .encode(
          Long10(Long.MaxValue,
                 Long.MaxValue,
                 Long.MaxValue,
                 Long.MaxValue,
                 Long.MaxValue,
                 Long.MaxValue,
                 Long.MaxValue,
                 Long.MaxValue,
                 Long.MaxValue,
                 Long.MaxValue))
        .getOrElse(Array.empty[Byte]))
    val `string 100 10` = ByteBuffer.wrap(
      packer
        .encode(
          String10(string100,
                   string100,
                   string100,
                   string100,
                   string100,
                   string100,
                   string100,
                   string100,
                   string100,
                   string100))
        .getOrElse(Array.empty[Byte]))

    val `int max 30` = ByteBuffer.wrap(
      packer
        .encode(Int30(
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MaxValue,
          Int.MinValue
        ))
        .getOrElse(Array.empty[Byte]))
    val `long max 30` = ByteBuffer.wrap(
      packer
        .encode(Long30(
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MaxValue,
          Long.MinValue
        ))
        .getOrElse(Array.empty[Byte]))
    val `string 100 30` = ByteBuffer.wrap(
      packer
        .encode(String30(
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100,
          string100
        ))
        .getOrElse(Array.empty[Byte]))
    val `string 1000 30` = ByteBuffer.wrap(
      packer
        .encode(String30(
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string1000,
          string100
        ))
        .getOrElse(Array.empty[Byte]))
    val `string 1000 30 multibyte`: ByteBuffer = ByteBuffer.wrap(
      packer
        .encode(String30(
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string1000multi,
          string100
        ))
        .getOrElse(Array.empty[Byte]))

    val circeAST: ByteBuffer = ByteBuffer.wrap(
      packer
        .encode(
          Json.arr(Json.fromString("prefix.tag"),
                   Json.fromLong(System.currentTimeMillis() / 1000),
                   pack.`string 100 30`.asJson))
        .getOrElse(Array.empty[Byte]))
  }
}
