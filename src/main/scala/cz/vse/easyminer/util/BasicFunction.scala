package cz.vse.easyminer.util

object Match {
  def default: PartialFunction[Any, Unit] = { case _ => }
  def apply[T](x: T)(body: PartialFunction[T, Unit]) = (body orElse default)(x)
}

object Lift {
  def default[U]: PartialFunction[Any, Option[U]] = { case _ => None }
  def apply[T, U](x: T)(body: PartialFunction[T, Option[U]]) = (body orElse default)(x)
}

object AutoLift {
  def apply[T, U](x: T)(body: PartialFunction[T, U]) = body.lift(x)
}

object BasicFunction {

  import scala.language.reflectiveCalls
  
  def tryClose[A, B <: { def close(): Unit }](closeable: B)(f: B => A): A = try { f(closeable) } finally { closeable.close() }
  
  def tryCloseBool[A, B <: { def close(): Boolean }](closeable: B)(f: B => A): A = try { f(closeable) } finally { closeable.close() }
  
  def optToThat[T]: PartialFunction[Option[T], T] = {case Some(x) => x} 
  
}
