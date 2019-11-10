package lse.neko.tools.logView;

/**
 * This class is used to create object that represent messages we
 * have not managed to match with another. Since it is useful to keep
 * trace of messages that have been lost, we create LostMessage
 * instead of creating Arrow.
 * @author Jennifer Muller
 */
class LostMessage {

    private NekoEvent event;
    private char identifier;
    private float time;
    private int process;

    LostMessage(float time,
                char identifier,
                NekoEvent event,
                int process)
    {
        this.event = event;
        this.identifier = identifier;
        this.time = time;
        this.process = process;
    }

    /**
     * Returns the event of the LostMessage in the format of a Neko Event.
     */
    public NekoEvent getEvent() {
        return event;
    }

    /**
     * Returns the identifier of the LostMessage.
     */
    public char getIdentifier() {
        return identifier;
    }

    /**
     * Returns the time of the LostMessage.
     */
    public Float getTime() {
        return new Float(time);
    }

    public int getProcess() {
        return process;
    }

}
