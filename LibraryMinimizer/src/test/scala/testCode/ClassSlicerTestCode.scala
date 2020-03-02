package testCode

class ClassSlicerTestCode {
  def main(a: Int): Unit = {
    if (a > 5) {
      foo1(a)
    }
  }

  def foo2(a: Int): Int = a + 5

  def foo1(a: Int): Int = a + foo2(a)

}
