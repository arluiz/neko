package lse.neko;

/**
 * Message type constants for events within a protocol stack.
 * FIXME: split it up. This class should disappear.
 */
public class EventTypeConst {

    public static final int CONS_NEW_ROUND = 100;
    public static final int CONS_START = 101;
    public static final int CONS_SUSPICION = 11;
    public static final int FD_START_SENDING = 16;
    public static final int FD_STOP_SENDING = 17;
    public static final int FD_START_RECEIVE = 18;
    public static final int FD_STOP_RECEIVE = 19;
    public static final int AB_SOLUTION_DELIVERED = 26;

    static {
        MessageTypes.instance().register(CONS_NEW_ROUND, "CONS_NEW_ROUND");
        MessageTypes.instance().register(CONS_START, "CONS_START");
        MessageTypes.instance().register(CONS_SUSPICION, "CONS_SUSPICION");
        MessageTypes.instance().register(FD_START_SENDING, "FD_START_SENDING");
        MessageTypes.instance().register(FD_STOP_SENDING, "FD_STOP_SENDING");
        MessageTypes.instance().register(FD_START_RECEIVE, "FD_START_RECEIVE");
        MessageTypes.instance().register(FD_STOP_RECEIVE, "FD_STOP_RECEIVE");
        MessageTypes.instance().register(AB_SOLUTION_DELIVERED,
                                         "AB_SOLUTION_DELIVERED");
    }

}
