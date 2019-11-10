package lse.neko;

/**
 * Message type constants.
 * FIXME: split it up. This class should disappear.
 */
public class MessageTypeConst {

    public static final int START = 0;

    /**
     * Shutdown notification to the application.
     * The content is an integer, telling what phase the shutdown
     * is entering.
     */
    public static final int STOP = 1;

    /**
     * Messages used by the shutdown protocol.
     */
    public static final int SHUTDOWN = 2;
    public static final int CONS_ESTIMATE = 6;
    public static final int CONS_PROPOSE = 3;
    public static final int CONS_ACK = 4;
    public static final int CONS_DECISION = 5;
    public static final int CONS_ABORT = 7;
    public static final int FD_I_M_ALIVE = 9;
    public static final int FD_ARE_YOU_ALIVE = 10;
    public static final int AB_START = 25;
    public static final int RBCAST_MESSAGE = 50;

    static {
        MessageTypes.instance().register(STOP, "STOP");
        MessageTypes.instance().register(SHUTDOWN, "SHUTDOWN");
        MessageTypes.instance().register(CONS_ESTIMATE, "CONS_ESTIMATE");
        MessageTypes.instance().register(CONS_PROPOSE, "CONS_PROPOSE");
        MessageTypes.instance().register(CONS_ACK, "CONS_ACK");
        MessageTypes.instance().register(CONS_DECISION, "CONS_DECISION");
        MessageTypes.instance().register(CONS_ABORT, "CONS_ABORT");
        MessageTypes.instance().register(FD_I_M_ALIVE, "FD_I_M_ALIVE");
        MessageTypes.instance().register(FD_ARE_YOU_ALIVE, "FD_ARE_YOU_ALIVE");
        MessageTypes.instance().register(AB_START, "AB_START");
        MessageTypes.instance().register(RBCAST_MESSAGE, "RBCAST_MESSAGE");
    }

}
