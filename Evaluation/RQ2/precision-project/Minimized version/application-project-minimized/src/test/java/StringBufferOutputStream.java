import java.io.IOException;
import java.io.OutputStream;

class StringBufferOutputStream extends OutputStream {
    // the target buffer
    private StringBuffer buffer;

    /**
     * Create an output stream that writes to the target StringBuffer
     *
     * @param out The wrapped output stream.
     */
    StringBufferOutputStream(StringBuffer out) {
        buffer = out;
    }


    // in order for this to work, we only need override the single character form, as the others
    // funnel through this one by default.
    public void write(int ch) {
        // just append the character
        buffer.append((char) ch);
    }
}
