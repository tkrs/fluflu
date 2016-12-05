package fluflu.msgpack

import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._

class TestData {

  val string100: String = "a" * 100
  val string1000: String = "z" * 100
  val string1000multi: String = "か" * 1000

  val `int max 10` = Int10(Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue)
  val `long max 10` = Long10(Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue)
  val `string 100 10` = String10(string100, string100, string100, string100, string100, string100, string100, string100, string100, string100)

  val `int max 30` = Int30(Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MaxValue, Int.MinValue)
  val `long max 30` = Long30(Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MaxValue, Long.MinValue)
  val `string 100 30` = String30(string100, string100, string100, string100, string100, string100, string100, string100, string100, string100, string100, string100, string100, string100, string100, string100, string100, string100, string100, string100, string100, string100, string100, string100, string100, string100, string100, string100, string100, string100)
  val `string 1000 30` = String30(string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string1000, string100)
  val `string 1000 30 multibyte` = String30(string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string1000multi, string100)

  val circeAST: Json = Json.arr(Json.fromString("prefix.tag"), Json.fromLong(System.currentTimeMillis() / 1000), `string 100 30`.asJson)

}
