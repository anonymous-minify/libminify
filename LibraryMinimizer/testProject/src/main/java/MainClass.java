import org.apache.commons.text.similarity.LongestCommonSubsequence;

public class MainClass {
    public static void main(String[] args) {
        TestObject testObject = new TestObject(4, new int[]{5, 6, 7});
        int x = TestClass.doSomething(testObject);
        //int y = TestClass.doSomething(new TestObject(x));
        int z = TestClass.switchTest(63);
        int a = TestClass.switchTest(63);
        int b = TestClass.tableSwitchTest("a", 2);
        int c = TestClass.tableSwitchTest("c", 8);
        int e = TestClass.tryTest(4);
        String st = TestClass.stringTest("test", true);
        int f = TestClass.stringTestMethods("test", "st");
        TestClass.staticMethodCallTest();
        TestClass.longCompareTest(5, 4);
        TestClass.intCompareTest(5, 4);
        String s = org.apache.commons.text.WordUtils.capitalizeFully("test");
        LongestCommonSubsequence sequence = new LongestCommonSubsequence();
        Integer i = sequence.apply("abc", "ab");
        TestClass.lambdaTest(5);
        TestClass.staticObject.getArray();
        TestClass.test(4, false);
        System.out.println(z + a + x + b + c + e);
        System.out.println(TestClass.loopTest(10000));
    }

    public void secondMain() {
        TestClass.longCompareTest(6, 4);
    }
}
