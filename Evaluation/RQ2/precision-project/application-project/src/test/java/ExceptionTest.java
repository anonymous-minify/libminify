import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ExceptionTest {

    //This test checks if the unnecessary catch is removed
    @Test
    void testExceptionHelper() {
        Object obj = new Object();
        ExceptionHelper handler = new ExceptionHelper();
        Assertions.assertNotNull(handler.checkIfNotNull(obj));
    }

    //The tested method always throws an exception
    @Test
    void divBy0Test() {
        MathHelper helper = new MathHelper();
        Assertions.assertThrows(ArithmeticException.class, () -> helper.divAbove1(1, 0));
    }

}