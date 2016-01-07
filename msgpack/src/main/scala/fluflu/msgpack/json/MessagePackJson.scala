package fluflu.msgpack.json

object MessagePackJson {

  import scala.collection.mutable.ListBuffer
  import io.circe.{ HCursor, Json }
  import fluflu.msgpack.MessagePack
  import MessagePack._

  def apply() = new MessagePack[Json]() {

    override def pack(doc: Json): Array[Byte] = {
      val buffer: ListBuffer[Byte] = ListBuffer.empty
      go(doc.hcursor, buffer)
      buffer.toArray
    }

    private[this] def go(hc: HCursor, buffer: ListBuffer[Byte]): Unit =
      hc.focus match {
        case js if js.isArray =>
          js.asArray match {
            case None => // TODO: error?
            case Some(arr) =>
              buffer ++= markArray(arr.size)
              arr.foreach(e => go(e.hcursor, buffer))
          }
        case js if js.isObject => //
          js.asObject match {
            case None => // TODO: error?
            case Some(e) =>
              val l = e.toList
              markMap(l.size).foreach(buffer += _)
              l.foreach {
                case (k, v) =>
                  formatOfString(k).foreach(buffer += _)
                  go(v.hcursor, buffer)
              }
          }
        case js if js.isNull => buffer ++= nilFormat
        case js if js.isBoolean => js.asBoolean match {
          case None => // TODO: error?
          case Some(x) => buffer ++= boolFormat(x)
        }
        case js if js.isNumber =>
          val num = js.asNumber
          num match {
            case None => // TODO: error?
            case Some(x) =>
              val n = x.toBigDecimal
              if (n.isWhole())
                buffer ++= intFormat(n.toLong)
              else
                buffer ++= formatOfDouble(n.toDouble)
          }
        case js if js.isString =>
          js.asString match {
            case None => // TODO: error?
            case Some(s) => buffer ++= formatOfString(s)
          }
        case _ => println(hc)
      }

    override def unpack(a: Array[Byte]): Option[Json] = ???
  }
}

