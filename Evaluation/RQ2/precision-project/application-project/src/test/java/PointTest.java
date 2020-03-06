import org.junit.jupiter.api.Test;

import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Small driver class to test Point2d
 *
 * @author Robert H. sloan
 */
class PointTest {
    @Test
    void testPoint2d() {
        Point2d pt1 = new Point2d();
        StringBuffer buffer = new StringBuffer();
        StringBufferOutputStream stream = new StringBufferOutputStream(buffer);
        PrintStream printStream = new PrintStream(stream);
        System.setOut(printStream);
        System.out.println("pt1 = " + pt1);

        Point2d pt2 = new Point2d(4.0, 3.0);

        System.out.println("pt2 = " + pt2);

        pt1.setDebug(true);        // turning on debugging
        // statements for pt1

        System.out.println("Distance from " + pt1 + " to " + pt2 +
                " is " + pt1.distanceFrom(pt2));

        System.out.println("Distance from " + pt2 + " to " + pt1 +
                " is " + pt2.distanceFrom(pt1));

        System.out.println("Distance from " + pt1 + " to the origin (0, 0) is " +
                pt1.distanceFromOrigin());

        System.out.println("Distance from " + pt2 + " to the origin (0, 0) is " +
                pt2.distanceFromOrigin());

        pt1.setXY(3, 5);
        System.out.println("pt1 = " + pt1);

        pt2.setXY(-3, -5);
        System.out.println("pt2 = " + pt2);

        System.out.println("Distance from " + pt1 + " to " + pt2 +
                " is " + pt1.distanceFrom(pt2));

        System.out.println("Distance from " + pt2 + " to " + pt1 +
                " is " + pt2.distanceFrom(pt1));

        pt1.setDebug(false);    // turning off debugging
        // statements for pt1

        System.out.println("Distance from " + pt1 + " to the origin (0, 0) is " +
                pt1.distanceFromOrigin());

        System.out.println("Distance from " + pt2 + " to the origin (0, 0) is " +
                pt2.distanceFromOrigin());

        assertTrue(buffer.toString().contains("Debug"));
    }


}

