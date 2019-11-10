package lse.neko.tools.logView;

/**
 * This class represents an object Display Process it contains a
 * boolean which tell us if we must display the messages or not, it
 * alse contain a name to identify the process bettern, i think it
 * replace its id. But keep it id somewhere...
 * @author Jennifer Muller
 */
public class DisplayProcess {

    private int id;
    private String name;
    private boolean display;

    DisplayProcess(int id, String name, boolean display) {
        this.id = id;
        this.name = name;
        this.display = display;
    }

    /**
     * Returns process id.
     */
    public int getId() {
        return this.id;
    }
    /**
     * Returns process name if specified, or null if not specified.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns a boolean which tells if this process must be displayed or not.
     */
    public boolean getDisplayBoolean() {
        return this.display;
    }
}
