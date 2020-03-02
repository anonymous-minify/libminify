package testCode;

public class MethodSlicerTestCode {
    public void testIfRewrite(boolean b) {
        if (b) {
            int i = 5;
            int a = i + 8;
            System.out.println(a);
        } else {
            int y = 45;
            int x = y + 8;
            System.out.println(x);
        }
    }

    public void testGotoNegativeRewriteTest(int i) {
        while (i < 5) {
            i++;
            System.out.println(i);
        }
    }

    public int testSwitchRewrite(int i, int a) {
        switch (i) {
            case 1:
                return 5;
            case 100:
                if (a > 200) {
                    return i + 3;
                } else {
                    return i + 5;
                }
            default:
                return i;
        }
    }

    public int testSwitchRewriteCodeRemovalBefore(int i, int a) {
        if (a > 150) {
            switch (i) {
                case 1:
                    return 5;
                case 100:
                    if (a > 200) {
                        return i + 3;
                    } else {
                        return i + 5;
                    }
                default:
                    return i;
            }
        } else {
            return i + 3;
        }
    }

    public int testSwitchRewriteMultipleCases(int i, int a) {
        switch (i) {
            case 1:
            case 9:
            case 10:
                return 5;
            case 100:
            case 5:
                if (a > 200) {
                    return i + 3;
                } else {
                    return i + 5;
                }

            case 200:
            default:
                return i;
        }
    }

    public int testSwitchRewriteWhile(int i, int a) {
        int b = 0;
        while (a < 150) {
            switch (i) {
                case 1:
                    b = 5;
                    break;
                case 100:
                    if (a < 100) {
                        b = i + 3;
                    } else {
                        b = i + 5;
                    }
                    break;
                default:
                    b = i;
                    break;
            }
        }
        return b;
    }

    public int testTableSwitch(String s, int i) {
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

    public int testMultipleNestedIfs(int i1, int i2) {
        while (i1 <= i2) {
            System.out.println(String.format("%d is smaller than %d", i1, i2));
            if (i1 == i2) {
                System.out.println(String.format("%d is equal to %d", i1, i2));
            } else {
                i1++;
            }

            if (i1 != i2) {
                i2++;
            } else {
                System.out.println(String.format("%d is equal to %d", i1, i2));
            }
        }

        return 8;
    }

    static int testTryCorrection(int i) {
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

}
