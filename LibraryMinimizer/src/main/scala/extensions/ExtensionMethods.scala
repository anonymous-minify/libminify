package extensions

import scala.collection.mutable
import scala.collection.mutable.ListBuffer


object ExtensionMethods {

  implicit class IterableMutableList[T](it: TraversableOnce[T]) {
    def toMutableList: mutable.MutableList[T] = {
      mutable.MutableList(it).flatten
    }

    def toListBuffer: ListBuffer[T] = {
      mutable.ListBuffer(it).flatten
    }
  }

  implicit class ExtendendMutableList[T](list: mutable.MutableList[T]) {
    def +=(iterable: Iterable[T]): Unit = {
      for (elem <- iterable) list += elem
    }
  }

  implicit class ExtendedListBuffer[T](listBuffer: mutable.ListBuffer[T]) {
    def +=(iterable: Iterable[T]): Unit = {
      for (elem <- iterable) listBuffer += elem
    }

    def -=(iterable: Iterable[T]): Unit = {
      for (elem <- iterable) listBuffer -= elem
    }
  }

}
