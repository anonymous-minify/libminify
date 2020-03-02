public class StringUtils {
    private String value;
    private boolean enableTrimming = false;
    private boolean enableErrorOutput = false;

    public StringUtils(String value) {
        this.value = value;
    }

    public int toInt() {
        String truncatedValue = value;
        if (enableTrimming) {
            truncatedValue = truncatedValue.trim();
        }


        try {
            return Integer.parseInt(truncatedValue);
        } catch (NumberFormatException ex) {
            if (enableErrorOutput)
                System.err.println(ex.getMessage());
            throw ex;
        }
    }

    public void enableTrimming() {
        this.enableTrimming = true;
    }

    public void enableErrorOutput() {
        this.enableErrorOutput = true;
    }

    public String getValue() {
        return value;
    }

    public void compare(Integer i, String s) {

        if (i.equals(s.length())) {

        }
    }
}


class Main {
    public static void main(String[] args) {
        StringUtils util = new StringUtils(args[0]);
        printInt(util);
    }

    public static void printInt(StringUtils util) {
        util.enableTrimming();
        System.out.println(util.toInt());
    }

    public static void otherMain(String[] args) {
        System.out.println(args[0]);
    }

    public static int i(int i) {
        switch (i) {
            case 1:
                return (10);
            case 10:
                return (100);
            default:
                return (10000);
        }
    }

    static int safeParseInt(String s) {
        int i = 0;
        try {
            i = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            System.out.println(e.getMessage());
            i = 100;
        } finally {
            System.out.println(i);
        }

        return i;
    }
}