public class SwitchHelper {
    int switchRemovalTest(int a) {
        switch (a) {
            case 25:
                return 5;
            case -123:
                return 4;
            default:
                return 0;
        }
    }

    int switchTest(int i) {
        switch (i) {
            case -1:
            case 35:
            case 47:
                return 8;
            case 63:
            case 92:
                return 5;
            case 64:
            case 23:
                return 3;
            case 75:
            case -15:
                return 4;
            default:
                return 0;
        }
    }

    int switchStringTest(String s, int i) {
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
}
