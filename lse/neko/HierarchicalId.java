package lse.neko;


/**
 * Microprotocol IDs may implement this interface to provide an
 * alternative ID. The alternative ID is used if no microprotocol is
 * registered with the microprotocol ID.
 *
 * @see Dispatcher
 */
public interface HierarchicalId {

    Object getParent();

}

