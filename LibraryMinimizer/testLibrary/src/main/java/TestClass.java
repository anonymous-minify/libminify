import java.util.Arrays;
import java.util.List;

class TestClass {

    static TestObject staticObject = new TestObject(5, new int[]{5, 6, 7});

    static int doSomething(TestObject testObject) {
        if (testObject.compare(3, 5)) {
            int x = testObject.getValue();
            int y = x + 3;
            int z = 4;
            return x + y + z;
        } else {
            String s = JavaClass.DoSomething(testObject.getValue() - 1);
            return 3;
        }
    }

    static String stringTest(String s, boolean b) {
        if (s == null)
            return "s should not be null";

        if (b) {
            String s2 = "peter" + s;
            return s2 + "a";
        } else return "b";
    }

    static int stringTestMethods(String s1, String s2) {
        if (s1.startsWith(s2)) {
            return 5;
        } else if (s1.endsWith(s2)) {
            return 3;
        } else if (s1.length() == 5) {
            return 4;
        } else {
            return 2;
        }
    }

    static int staticMethodCallTest() {
        if (intCompareTest(4, 5) == 32) {
            return 5;
        } else {
            return 4;
        }
    }

    static void longCompareTest(long d1, long d2) {
        if (d1 <= d2) {
            System.out.println();
        }
    }

    static int intCompareTest(int i1, int i2) {
        if (i1 < i2) {
            return i1;
        } else return i2;
    }

    static int switchTest(int a) {
        switch (a) {
            case -1:
            case 35:
            case 47:
            case 63:
            case 92:
                return 5;
            case 64:
                return 4;
            default:
                return 0;
        }
    }

    static int tableSwitchTest(String s, int i) {
        switch (s) {
            case "a":
                return -100;
            case "45b":
                return 1;
            case "c":
                if (i < 5)
                    return 3;
                return 2;
            default:
                return -1;
        }
    }

    static int tryTest(int i) {
        int a = 5;
        try {
            if (i < 5) {
                a = i + 3;
            } else {
                a = i + 5;
            }
        } catch (RuntimeException ex) {
            a = i + 9;
        } catch (Exception e) {
            a = i + 7;
        } finally {
            a++;
        }

        if (a < 10) {
            return a + 5;
        } else {
            return a + 8;
        }
    }

    static int lambdaTest(int a) {
        List<String> myList = Arrays.asList("element1", "element2", "element3");
        myList.stream().filter(s -> s.length() > 1).forEach(System.out::println);
        return a;
    }

    public static int test(int i, boolean b) {
        try {
            if (b) {
                return 3;
            } else {
                switch (i) {
                    case 5:
                        if (!b) {
                            return 6;
                        }
                        break;
                    case 6:
                        return 7;
                }
            }
        } catch (Exception e) {
            if (!b)
                return 4;
        }
        return 5;
    }

    public static int loopTest(int a) {
        int x = 0;
        for (int i = 0; i < a; i++) {
            if (i < 5000)
                x++;
        }

        return x;
    }
}