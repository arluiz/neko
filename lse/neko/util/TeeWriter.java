package lse.neko.util;

// java imports:
import java.io.IOException;
import java.io.Writer;


/**
 * Class for writing character streams into multiple character streams.
 */
public class TeeWriter
    extends Writer
{

    /**
     * The underlying character-output streams.
     */
    protected Writer[] out;

    /**
     * Create a new T-writer.
     *
     * @param out  an array of Writer objects: the underlying streams.
     * Note: if the array is empty, the writer discards all the characters
     * written.
     */
    public TeeWriter(Writer[] out) {
        super(out);
        this.out = out;
    }

    /**
     * Write a single character.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write(int c) throws IOException {
        for (int i = 0; i < out.length; i++) {
            out[i].write(c);
        }
    }

    /**
     * Write a portion of an array of characters.
     *
     * @param  cbuf  Buffer of characters to be written
     * @param  off   Offset from which to start reading characters
     * @param  len   Number of characters to be written
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write(char[] cbuf, int off, int len) throws IOException {
        for (int i = 0; i < out.length; i++) {
            out[i].write(cbuf, off, len);
        }
    }

    /**
     * Write a portion of a string.
     *
     * @param  str  String to be written
     * @param  off  Offset from which to start reading characters
     * @param  len  Number of characters to be written
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void write(String str, int off, int len) throws IOException {
        for (int i = 0; i < out.length; i++) {
            out[i].write(str, off, len);
        }
    }

    /**
     * Flush the stream.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void flush() throws IOException {
        for (int i = 0; i < out.length; i++) {
            out[i].flush();
        }
    }

    /**
     * Close the stream.
     *
     * @exception  IOException  If an I/O error occurs
     */
    public void close() throws IOException {
        for (int i = 0; i < out.length; i++) {
            out[i].close();
        }
    }

}
