package lse.neko;

/**
 * All microprotocols should implement this interface.
 * Beside this, microprotocols should obey a set of
 * conventions.
 *
 * Microprotocols are stored in a container of type
 * {@link NekoProcess}.
 *
 * <p>
 *
 * Configuration and communication with other microprotocols should
 * happen as follows:
 *
 * <ul>
 *
 * <li>Microprotocols communicate with microprotocols in the same
 * container by calling a method on the appropriate reference.</li>
 *
 * <li>Microprotocols are configured using dependency injection.  This
 * means that they should get all references to other microprotocols
 * passed to the constructor or setter methods. They should not get
 * such references themselves.</li>
 *
 * <li>They should not try to determine the class type of references
 * to other microprotocols. Otherwise, intercepting method calls for
 * purposes such as logging becomes difficult.</li>
 *
 * <li>Microprotocols have an identifier that is unique within the
 * container. This ID is set with <code>setId</code>. The container
 * provides the means to look up microprotocols by this ID. Check out
 * the {@link AbstractId} class to see how unique IDs can be
 * constructed.</li>
 *
 * <li>IDs are used when communicating with microprotocols in other
 * containers. {@link NekoMessage} includes a constructor argument
 * protocolId. Microprotocols often communicate with a microprotocol
 * that has the same ID, and thus use <code>getId</code> when
 * constructing <code>NekoMessage</code>s.</li>
 *
 * <li>IDs are also useful for diagnostic messages.</li>
 *
 * </ul>
 *
 * The lifecycle of microprotocols consists of the following actions,
 * in the order of occurrence:
 *
 * <ul>
 *
 * <li>The constructor is called.</li>
 *
 * <li>Setters are called. Calling <code>setId</code> is
 * obligatory. Usually, calling each setter is obligatory.</li>
 *
 * <li>Once all necessary setters have been called (and finished), the
 * microprotocol is fully constructed (this is not an explicit action,
 * just a point of reference for other actions).</li>
 *
 * <li>The <code>launch</code> method is called. The microprotocol is
 * registered in the container by this method.</li>
 *
 * <li>Other methods may be called. They may be called once the
 * microprotocol is fully constructed, even before <code>launch</code>.</li>
 *
 * </ul>
 *
 * The microprotocol may call methods of other microprotocols once it
 * is fully constructed, and a method (<code>launch</code> or another)
 * is called.
 */
public interface Protocol {

    /**
     * Sets the identifier of this microprotocol.
     */
    void setId(Object id);

    /**
     * Returns the identifier of this microprotocol.
     */
    Object getId();

    /**
     * This method is called once when the microprotocol is set up.
     * Only the constructor and set methods are called before this
     * method is called. The microprotocol should only call methods on
     * other microprotocols after this method is called.
     *
     * The method is not called <code>start</code> to avoid confusion
     * with <code>Thread.start</code>. However, <code>launch</code> is
     * the right place to start private threads of the microprotocol.
     *
     * You should normally call super.launch() when overriding this
     * method.
     */
    void launch();

}
