public class SwitchHelper {
    int switchRemovalTest(int a) {
        return 0;
    }

    int switchTest(int i) {
        switch (i) {
            case -1:
            case 35:
                return 8;
            case 92:
                return 5;
            case 23:
                return 3;
            default:
                return 0;
        }
    }

    int switchStringTest(String s, int i) {
        switch (s) {
            case "a":
                return -100;
            default:
                return -1;
        }
    }
}
