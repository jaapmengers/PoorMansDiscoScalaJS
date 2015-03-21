package importedjs.socketio.server

import scala.scalajs.js

package object socketio extends js.GlobalScope {
  val SocketManager: SocketManager = js.native
}

trait Socket extends js.Object {
  var json: js.Any = js.native
  var log: js.Any = js.native
  var volatile: js.Any = js.native
  var broadcast: js.Any = js.native
  def in(room: String): Socket = js.native
  def to(room: String): Socket = js.native
  def join(name: String, fn: js.Function): Socket = js.native
  def unjoin(name: String, fn: js.Function): Socket = js.native
  def set(key: String, value: js.Any, fn: js.Function): Socket = js.native
  def get(key: String, value: js.Any, fn: js.Function): Socket = js.native
  def has(key: String, fn: js.Function): Socket = js.native
  def del(key: String, fn: js.Function): Socket = js.native
  def disconnect(): Socket = js.native
  def send(data: js.Any, fn: js.Function): Socket = js.native
  def emit(ev: js.Any): Socket = js.native
}

trait SocketNamespace extends js.Object {
  def clients(room: String): js.Array[Socket] = js.native
  var log: js.Any = js.native
  var store: js.Any = js.native
  var json: js.Any = js.native
  var volatile: js.Any = js.native
  def in(room: String): SocketNamespace = js.native
  def on(evt: String, fn: js.Function): SocketNamespace = js.native
  def to(room: String): SocketNamespace = js.native
  def except(id: js.Any): SocketNamespace = js.native
  def send(data: js.Any): js.Dynamic = js.native
  def emit(name: String, data: js.Any): js.Dynamic = js.native
  def socket(sid: js.Any, readable: Boolean): Socket = js.native
  def authorization(fn: js.Function): js.Dynamic = js.native
}

trait SocketManager extends js.Object {
  def get(key: js.Any): js.Dynamic = js.native
  def set(key: js.Any, value: js.Any): SocketManager = js.native
  def enable(key: js.Any): SocketManager = js.native
  def disable(key: js.Any): SocketManager = js.native
  def enabled(key: js.Any): Boolean = js.native
  def disabled(key: js.Any): Boolean = js.native
  def configure(env: String, fn: js.Function): SocketManager = js.native
  def configure(fn: js.Function): SocketManager = js.native
  def of(nsp: String): SocketNamespace = js.native
  def on(ns: String, fn: js.Function): SocketManager = js.native
  var sockets: SocketNamespace = js.native
}
