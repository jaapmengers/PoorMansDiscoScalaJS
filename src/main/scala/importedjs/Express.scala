package importedjs

import scala.scalajs.js

package object Express extends js.GlobalScope {
  val app: ExpressApp = js.native
}

trait ExpressApp extends js.Object {
  def get(path: String, callback: js.Any): Nothing = js.native
}
