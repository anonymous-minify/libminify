import java.io.IOException;

public class HelloWorldException {
    public static void main(String[] args) throws Exception {
        try {
            System.out.println("Bienvenitos!");
            throw new Exception("Generic Exception");
        } catch (IOException e) {
            System.out.println("should never happen");
        }
    }
}
