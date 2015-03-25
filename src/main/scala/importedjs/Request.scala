package importedjs

import scala.scalajs.js

package object Request extends js.GlobalScope {
  val requestInstance: RequestInstance = js.native
}

trait RequestInstance extends js.Object {
  def put(data: js.Any, callback: js.Any): js.Any = js.native
}
