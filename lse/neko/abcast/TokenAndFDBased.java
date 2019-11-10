package lse.neko.abcast;

// java imports:
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

// lse.neko imports:
import lse.neko.MessageTypeConst;
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.ProtocolImpl;
import lse.neko.ReceiverInterface;
import lse.neko.SenderInterface;
import lse.neko.UnexpectedMessageException;
import lse.neko.failureDetectors.FailureDetectorInterface;
import lse.neko.failureDetectors.FailureDetectorListener;
import lse.neko.util.Adeliv;
import lse.neko.util.ContentRB;
import lse.neko.util.GUID;
import lse.neko.util.MySystem;
import lse.neko.util.logging.NekoLogger;


/**
 * Class implementing the Atomic Broadcast algorithm based on Tokens and
 * unreliable failure detectors. Described in
 * FIXME: reference the technical report
 */
public class TokenAndFDBased
    extends ProtocolImpl
    implements SenderInterface, ReceiverInterface, FailureDetectorListener
{

    /**
     * method for calculating the network size of an object (debugging).
     */
    private static int networkSize(Object o) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);

            oos.writeObject(o);
            oos.flush();
            return baos.size();
        } catch (IOException e) {
            logger.log(Level.INFO,
                       "IOException {0} while counting bytes in object {1}",
                       new Object[] { e, o });
            return -1;
        }
    }


    /**
     * How large can a proposal set be at most?
     */
    int maxProposalSize = -1;

    /**
     * How large can the sum of the size of all proposals be at most.
     */
    int maxTotalProposalSize = 20;

    /**
     * Should new messages be sent at once or only when holding the token.
     */
    boolean sendAtOnce = true;

    /**
     * Should several (up to f) proposals be allowed in the token?
     */
    boolean multipleProposals = true;

    /**
     * Should decision be broadcast as soon as they are taken?
     */
    boolean broadcastDecisions = true;

    /**
     * List of messages that are waiting to be sent.
     */
    List toBeSent = new ArrayList();

    /**
     * List of processes that have suspected their predecessor and
     * are waiting for part of the adelivered sequence.
     */
    List backloggedProcesses = new LinkedList();

    /**
     * Boolean variable indicating if the predecessor is suspected.
     */
    boolean predecessorSuspected = false;

    void setMaxProposalSize(int maxTotalProposalSize0) {
        this.maxTotalProposalSize = maxTotalProposalSize0;

        if (multipleProposals) {
            // FIXME: here, a proposal can take at most 1/f of the total
            // proposal space. If maxTotalProposalSize is not a multiple of
            // f, this will lead to proposals that might be too small
            // and achieve
            // a total size between maxTotalProposalSize - f + 1
            // and maxTotalProposalSize - 1
            this.maxProposalSize = maxTotalProposalSize / f;
        } else {
            this.maxProposalSize = maxTotalProposalSize;
        }
    }

    // FIXME: things to add:
    // - split seen_i into two sets : delivered and undelivered messages

    // TODO:
    // - split the behavior related to a dynamic token destination set into
    //   a subclass. This would probably implement the readability of this
    //   (very long) class.

    // Optimizations that don't work:
    // - mutual exclusion between message delivery and token processing
    //   cuts performance by about 60%

    // Optimizations that have been implemented:
    // - whenever adelivI changes, call atomicDelivery()
    //   only if there isn't a message pending (nextAdel != null)
    // - use a 'new message' subset of seenI to decide which messages
    //   should be sent with the token
    // - call deliver() only after the token has been sent to the next process.
    //   In this way, the token processing isn't slowed down
    //   because of the delivery of new messages. Since the time
    //   between two token possessions is quite high
    //   (in a setting with 7 processes or more),
    //   messages should be delivered between token possessions.


    /* Message types */
    static final int TOKEN = 78907;
    static final int ADD_TO_DEST = 78908;
    static final int REMOVE_FROM_DEST = 78909;
    static final int RETRANSMISSION = 78910;
    static final int REQUEST_MSG = 78911;
    static final int CONS_SOLUTION = 78912;

    static {
        MessageTypes.instance().register(TOKEN, "TOKEN");
        MessageTypes.instance().register(ADD_TO_DEST, "ADD_TO_DEST");
        MessageTypes.instance().register(REMOVE_FROM_DEST, "REMOVE_FROM_DEST");
        MessageTypes.instance().register(RETRANSMISSION, "RETRANSMISSION");
        MessageTypes.instance().register(REQUEST_MSG, "REQUEST_MSG");
        MessageTypes.instance().register(CONS_SOLUTION, "CONS_SOLUTION");
    }

    /**
     * Maximum number of failures that the algorithm will tolerate.
     * (we must have f(f+1)+1 less than N)
     */
    private final int f;

    /**
     * Last token sent by this process.
     */
    private Token lastToken = null;

    /**
     * Sequence of IDs of ordered messages.
     */
    private Adeliv adelivI = new Adeliv();

    /**
     * Round that this process is currently in.
     */
    private int roundI = 0;

    /**
     * IDs of messages that have been received but not yet ordered.
     */
    private Set aUnordered = new LinkedHashSet();

    /**
     * HashMap of messages that have been rdelivered but not adelivered yet.
     * The mapping is key=id of message, value=message.
     */
    private Map aUndelivered = new HashMap();

    /**
     * HashSet of all (backup) messages received in the Proposal field of the
     * tokens. These messages will initially not be used : we assume that the
     * rbcasts of AB_START are uniform (i.e. if a process, faulty or not,
     * rdelivers a message, then all correct processes deliver that message)
     */
    // FIXME: where is the code that handles this aProposalBackups? When should
    // these messages be resent? Even if we assume that rbcast is uniform, an
    // AB_START message might not be received for an ordered message.
    private Set aProposalBackups = new HashSet();

    /**
     * ID of this process.
     */
    private final int myID;

    /**
     * Number of processes in this run.
     */
    private final int n;

    /**
     * Identifier of my immediate predecessor.
     */
    private final int predecessor;

    /**
     * Set of processes that will receive the token at the next send.
     */
    private Token.Destination tokenDestI = null;

    /**
     * Reliable broadcast interface used for reliably sending the
     * token + messages.
     */
    protected final SenderInterface rbcast;

    /**
     * Failure detector used by the algorithm.
     */
    protected final FailureDetectorInterface failureDetector;

    /**
     * Sequence number for control messages received from the 'f' sucessors.
     * Initially, all sequence numbers are equal to zero.
     */
    int[] seqI = null;

    /**
     * Sequence number of this for control message sending.
     */
    int mySeqI = 0;

    /**
     * Gets the index in seqI for a given process.
     */
    int seqIndex(int successor) {
        return (successor - myID + n) % n - 1;
    }

    /**
     * Initializes the different sets of processes needed for the
     * communication: the destination set for the token, the array of
     * all predecessors and the 'others' array (all processes except me).
     *
     * This method assumes that this.f (the number of tolerated failures)
     * is initialized and that this.myID (my sequence number in the group) is
     * also known
     */
    private void initCtrlDataStructures() {

        // create a new set of token destinations (by default: p_{i+1})
        tokenDestI = new Token.Destination(this.f, this.myID, this.n);

        // create the array of sequence numbers for the f next processes
        seqI = new int[f + 1];
        mySeqI = 0;

        // create the int array with identifiers of all processes except me
        all = new int[n];
        others = new int[n - 1];
        int j = 0;
        for (int i = 0; i < n; i++) {
            all[i] = i;
            if (i != myID) {
                others[j] = i;
                j++;
            }
        }
    }

    /**
     * Constructor which provides the control of all parameters used
     * by the algorithm.
     *
     * FIXME: the reduceTokenSize parameter has been replaced
     * by the multipleProposals parameter
     */
    public TokenAndFDBased(NekoProcess process,
                           SenderInterface rbcast,
                           FailureDetectorInterface fd,
                           int f,
                           boolean sendAtOnce,
                           boolean multipleProposals,
                           boolean broadcastDecisions)
    {
        this(process, rbcast, fd, f);
        this.sendAtOnce = sendAtOnce;
        this.multipleProposals = multipleProposals;
        this.broadcastDecisions = broadcastDecisions;
        logger.log(Level.INFO, "Starting the algorithm with sendAtOnce = {0}",
                Boolean.valueOf(sendAtOnce));

    }

    private ReceiverInterface receiver;

    public void setReceiver(ReceiverInterface receiver) {
        this.receiver = receiver;
    }

    private NekoProcess process;

    /**
     * Initializes the data structures for all processes taking part in the
     * atomic broadcast algorithm using a token.
     *
     * Process p_0 sends a real token to all other processes. The last
     * f processes in the ring (p_{n-f-1},...,p_{n-1}) send a dummy
     * token to all processes in (p_1,...,p_f)
     */
    public TokenAndFDBased(NekoProcess process,
                           SenderInterface rbcast,
                           FailureDetectorInterface fd,
                           int f)
    {

        this.process = process;

        // set the number of processes in the group
        this.n = process.getN();

        // check if the number of tolerated failures can be achieved
        if (f * f + f + 1 > n) {
            String s = "The token-based algorithm cannot tolerate "
                + f + " failures with " + n + " processes";
            throw new IllegalArgumentException(s);
        }

        // set the number of tolerated failures
        this.f = f;

        // set my ID
        this.myID = process.getID();

        // set the ID of my predecessor
        this.predecessor = (myID - 1 + n) % n;

        // set the round number
        roundI = 0;

        // set the RBcast to use and a reference to the failure detector
        this.rbcast = rbcast;
        this.failureDetector = fd;
        this.failureDetector.setListener(this);

        // sets the maximum size for a proposal
        setMaxProposalSize(maxTotalProposalSize);

        // initialize all parts related to control messages and set of processes
        initCtrlDataStructures();

    }

    /**
     * Launches the token and fd based abcast algorithm; the "real" token
     * is sent by process 0 and dummy tokens are sent by processes n-f to n-1.
     * This cannot be done in the constructor of TokenAndFDBased, since the
     * protocol does not have an Id (by setId) at that time.
     */
    public void launch() {
        super.launch();

        // if I am the first process, send a real token
        if (myID == 0) {
            lastToken = Token.empty(myID, roundI);
            // send initial token
            rbcast.send(new NekoMessage(myID,
                        // send the token to the set of dest.
                        tokenDestI.array(),
                        getId(),
                        // create a new initial token
                        lastToken,
                        TOKEN));

            // else, if I'm one of the last processes,
            // send a dummy token with round == -1
        } else if (myID >= n - f) {
            lastToken = Token.empty(myID, -1);
            // send dummy tokens with round == -1
            // the destinations are all processes in 1..myID+f+1
            int [] destinations = new int[(myID + f + 1) % n];
            // put the destinations in the array
            for (int i = 0; i < (myID + f + 1) % n; i++) {
                destinations[i] = i + 1; // destinations go from 1..myID + f + 1
            }

            // send the dummy-token (round == -1)
            rbcast.send(new NekoMessage(myID, destinations, getId(),
                                        lastToken, TOKEN));
        }

    }

    /**
     * ABcast a NekoMessage m.
     *
     * @param m NekoMessage to be ABCast
     */
    public void send(NekoMessage m) {
        GUID id = new GUID(process);
        ContentRB theMessage =
            new ContentRB(id, m.getSource(), m.getProtocolId(),
                          m.getContent(), m.getType());

        // create the NekoMessage
        final NekoMessage data =
            new NekoMessage(m.getSource(),
                    all, // send to all except me
                            // FIXME: this should maybe be changed
                            // to m.getDestinations()
                            // or servers in a future version
                    getId(),
                    theMessage, // the message to send
                    MessageTypeConst.AB_START // we're sending a message
                    );

        // FIXME: In Chandra Toueg : the network is set here... probably not
        // really useful for the token algorithm...

        if (sendAtOnce) {
            // send the message to all
            rbcast.send(data);
        } else {
            // place the message in the list of messages to be sent
            synchronized (toBeSent) {
                toBeSent.add(data);
            }
        }
    }

    /**
     * Array of ints containing the identifiers of all processes except me.
     */
    private int[] others;


    /**
     * Array of ints containing the identifiers of all processes.
     */
    private int[] all;



    /**
     * Token that has been received but cannot be processed yet.
     */
    private Token saved = null;

    /**
     * Method that is called by deliver() whenever a token is received.
     */
    private void deliverToken(Token token) {
        logger.log(Level.INFO, "Received token {0} at time {1}",
                new Object[]{token,
                    new Long(MySystem.currentTimeMicros() % 10000000)
                });
        // update the round of the token if it has been received from a
        // process at the end of the ring
        if (token.sender > myID) {
            token.round++;
        }

        // check if we should update the suspected/unsuspected status of the
        // predecessor
        statusChange(failureDetector.isSuspected(predecessor), predecessor);

        // FIXME: here, we could use a HashMap with mapping between round and
        // token to store the early tokens...
        // if the sender is not p_{i-1} and p_{i-1} is not suspected,
        // save this message
        if (!predecessorSuspected
            && token.sender != predecessor
            && token.round >= roundI)
        {
            // save this token if there have been no saves before
            // or if it is more recent than the last save...
            if (saved == null || token.compareTo(saved) > 0) {
                saved = token;
                return;
            }
        } else if (saved != null && saved.round < roundI) {
            // throw away saved token if it is too old
            saved = null;
        }
        // process the token
        processToken(token);

        logger.log(Level.FINE, "End of token processing at time {0}",
                   new Long(MySystem.currentTimeMicros() % 10000000));
        // exit the handler
        return;
    }

    /**
     * Adds a process to the set of destination processes of the token.
     * This method also retransmits a token with the delivered messages that
     * the 'sender' has not seen yet (i.e. all messages between lastAdelivered
     * and the last message that 'this' has adelivered)
     *
     * @param sender Process to be added to tokenDestI
     * @param lastAdelivered Last message that was delivered by sender
     */
    private void deliverAddToDest(int sender, int lastAdelivered) {
        // add the process to the set of destinations
        tokenDestI.add(sender);
        // if lastAdelivered >= adelivI.lastPos(), then the requesting
        // process is not missing any messages => return.
        if (lastAdelivered >= adelivI.lastPos()) {
            return;
        }

        // retransmit part of the ordered sequence if sender has missed
        // messages this has no holes in its adelivered sequence
        if (!adelivI.hasHoles()) {
            // send the last adelivered messages to the requesting process
            rbcast.send(new NekoMessage(myID,
                                        new int[]{sender},
                                        getId(),
                                        adelivI.tailList(lastAdelivered + 1),
                                        CONS_SOLUTION));
        } else {
            // store this request and eventually resend the requested
            // information to sender
            backloggedProcesses.add(new BackloggedProcess(sender,
                                                        lastAdelivered + 1,
                                                        adelivI.lastPos()));
        }
    }

    /**
     * Removes process 'sender' from the set of destination processes of
     * a token.
     *
     * @param sender ID of the process to be removed from tokenDestI
     */
    private void deliverRemoveFromDest(int sender) {
        // remove 'sender' from the set of token destinations
        tokenDestI.remove(sender);
    }

    /**
     * Add a message to the seenI set and try delivering messages to
     * the application.
     *
     * @param msg ContentRB to be added to seenI
     */
    private void deliverMessage(ContentRB msg) {
        throw new IllegalStateException("The current version of the Token "
                + "based atomic broadcast algorithm assumes that uniform "
                + "reliable broadcast is available (as does the ChandraToueg "
                + "implementation). This method should therefore never be "
                + "needed.");
        /*
        // FIXME: seenI should maybe be transformed into a HashMap??

        // add the message to seen
        if(!adelivI.contains(msg.getId())) {
        aUnordered.add(msg);
        }
        else {
        // FIXME: add the message to the set of other seen messages
        }

        // try delivering messages
        atomicDelivery();
        return;*/
    }

    /**
     * Process a REQUEST_MSG control message.
     */
    private void deliverRequestMessage(int requestingProc, GUID mid) {
        throw new IllegalStateException("The current version of the Token "
                + "based atomic broadcast algorithm assumes that uniform "
                + "reliable broadcast is available (as does the ChandraToueg "
                + "implementation). This method should therefore never be "
                + "needed.");

        // if we do not have the requested message, do nothing
        //FIXME: before we had this : if (!seenI.contains(mid)) {
        /*if(!aUnordered.contains(mid)) {
        //FIXME: add contains call to aUndelivered or something here...
          return;
          }

        // else, send the message to the requesting process
        rbcast.send(new NekoMessage(myID, new int[]{requestingProc}, getId(),
        seenI.get(mid),
        RETRANSMISSION
        ));*/
    }

    /**
     * Reception of a NekoMessage m: the message is processed according to
     * its content.
     */
    public void deliver(NekoMessage m) {
        synchronized (this) {
            switch(m.getType()) {
            // Reception of a token
            case TOKEN :
            {
                // extract the token, make a copy of it (to avoid
                // simultaneous modifications in simulation mode),
                // and deliver it
                deliverToken(new Token((Token) m.getContent()));
                return;
            }
            case ADD_TO_DEST :
            case REMOVE_FROM_DEST :
            {
                // sender of the message
                int sender = m.getSource();

                int [] content = (int []) m.getContent();

                // sequence number of this message
                int seqno = content[0];

                // if the message is older than the most recent one
                // received, throw it away...
                if (seqno <= seqI[seqIndex(sender)]) {
                    return;
                }

                // update the sequence number info for this message
                seqI[seqIndex(sender)] = seqno;

                if (m.getType() == ADD_TO_DEST) {
                    // last message that was adelivered by the sender of
                    // this message
                    int lastAdelivered = content[1];
                    deliverAddToDest(sender, lastAdelivered);

                } else {
                    deliverRemoveFromDest(sender);
                }

                return;
            }
            case MessageTypeConst.AB_START:
            {
                // new message : check if it has already been ordered
                ContentRB msg = (ContentRB) m.getContent();
                aUndelivered.put(msg.getId(), msg);
                if (!adelivI.contains(msg.getId())) {
                    aUnordered.add(msg.getId());
                } else {
                    atomicDelivery();
                }
                return;
            }
            case CONS_SOLUTION:
            {
                // we've received the result of an ordering decision
                Adeliv.Subsequence newDeliv =
                    (Adeliv.Subsequence) m.getContent();
                // add the new messages to adeliv
                atomicDeliveryTokenAdeliv(newDeliv);
                // try delivering the newly ordered messages
                atomicDelivery();
                return;

            }
            case RETRANSMISSION:
            {
                // logger.log(Level.INFO, "Received message {0}",
                //            (Message) m.getContent());
                // add the message to seenI and try to deliver to the
                // application
                deliverMessage((ContentRB) m.getContent());
                return;
            }

            case REQUEST_MSG :
            {
                // logger.log(Level.FINE, "Received request for "
                //             + "message : {0}",
                //            m.getContent());
                int sender = m.getSource();
                GUID mID = (GUID) m.getContent();

                deliverRequestMessage(sender, mID);

                return;
            }
            default:
                // simply continue...
                break;
            }
        }
        throw new UnexpectedMessageException(m);
    }

    /**
     * This function is called each time the suspected status of the
     * predecessor changes. This can happen either when receiving the
     * token or when receiving a CONS_SUSPICION message from the failure
     * detector.
     *
     * @param suspected Boolean indicating if the predecessor is suspected
     */
    public void statusChange(boolean suspected, int p) {

        assert (p == predecessor);

        // if the new status is the same as the old, nothing to be done.
        if (predecessorSuspected == suspected) {
            return;
        }

        predecessorSuspected = suspected;

        int msgType = -1; // type of the message to be sent
        int msgSeq = -1; // sequence number of this message
        // destination processes for the message
        int[] predecessors = new int[f];
        for (int i = 0; i < f; i++) {
            // predecessors: i-2 ... i-f-1
            predecessors[i] = (myID - 2 - i + n) % n;
        }

        // new sequence number for this message
        msgSeq = ++mySeqI;

        // content of the message (either msgSeq or msgSeq+last adelivered msg)
        int [] content = null;

        // decide which type of message we're sending
        if (predecessorSuspected) {
            msgType = ADD_TO_DEST;
            content = new int[]{msgSeq, adelivI.lastContiguousPos()};
        } else {
            msgType = REMOVE_FROM_DEST;
            content = new int[]{msgSeq};
        }

        // send the message to the predecessors
        rbcast.send(new NekoMessage(myID,
                    // send message to all predecessors
                    // (except p_{i-1})
                    predecessors,
                    getId(),
                    content,
                    // ADD_TO_DEST or REMOVE_FROM_DEST
                    // depending on 'predecessorSuspected'
                    msgType
                    ));

        // if the predecessor is suspected and there is a previously
        // saved token, process that token.
        if (predecessorSuspected) {
            if (saved != null) {
                processToken(saved);
                saved = null;
            }
        }
    }

    /**
     * Check if there are backlogged processes waiting for some of the
     * messages in the adelivered sequence. If there are waiting processes
     * and this can send all requested messages, do so.
     */
    protected void checkBackloggedProcesses() {
        if (backloggedProcesses.isEmpty()) {
            return;
        }

        do {
            BackloggedProcess p =
                (BackloggedProcess) backloggedProcesses.get(0);

            if (adelivI.lastContiguousPos() >= p.getLast()) {
                rbcast.send(new NekoMessage(myID,
                            new int[]{p.getId()},
                            getId(),
                            adelivI.subList(p.getFirst(), p.getLast()),
                            CONS_SOLUTION));
                backloggedProcesses.remove(0);
            } else {
                break;
            }

        } while(!backloggedProcesses.isEmpty());
    }

    /**
     * Next entry to be adelivered to the application.
     */
    GUID nextAdel = null;

    /**
     * Add the newly deliverable messages toDeliver to the set of
     * ordered messages. This call should be done only when a decision
     * can be taken; the parameter of the call will be a sequence from
     * token.proposalSeq
     *
     * @param setToDeliver LinkedList of Message to be merged with adelivI
     */
    protected void atomicDeliveryNew(List setToDeliver) {

        logger.log(Level.INFO, "Delivering messages {0}", setToDeliver);

        // extract the GUIDs from the setToDeliver ContentRB messages
        List deliveryGUIDs = new LinkedList();
        for (Iterator it = setToDeliver.iterator(); it.hasNext();) {
            deliveryGUIDs.add(((ContentRB) it.next()).getId());
        }

        // add messages in deliveryGUIDs to adelivI
        // (position of first added element
        // is returned)
        int position = adelivI.addAll(deliveryGUIDs);

        if (broadcastDecisions) {
            //FIXME: put a boolean condition to decide
            // whether the decision should be broadcast
            Adeliv.Subsequence newlyOrdered =
                new Adeliv.Subsequence(position, deliveryGUIDs);

            // create a message with the decision that was reached ...
            NekoMessage decision = new NekoMessage(myID,
                    all,
                    getId(),
                    newlyOrdered,
                    CONS_SOLUTION);
            // ... and send it to all
            rbcast.send(decision);
        }

        // Remove messages that were just ordered from aUnordered
        aUnordered.removeAll(deliveryGUIDs);
    }

    /**
     * Try delivering messages to the application. Before the
     * delivery starts, the messages in toDeliver are merged with the
     * set of already ordered messages
     *
     * @param toDeliver SortedSet of Adeliv.Entry to be merged with adelivI
     */
    protected void atomicDeliveryTokenAdeliv(Adeliv.Subsequence toDeliver) {

        logger.log(Level.INFO, "Adelivering messages {0} from token",
                   toDeliver);
        if (toDeliver.getMsgs().size() > 0) {
            adelivI.addAll(toDeliver);

            // FIXME: iterate through all in toDeliver and remove messages from
            // aUnordered (actually, only iterate through the new messages in
            // toDeliver)
            for (Iterator it = toDeliver.getMsgs().iterator(); it.hasNext();) {
                GUID current = (GUID) it.next();
                aUnordered.remove(current);
            }

            // check if the messages that were delivered are needed by
            // one of the backlogged processes
            checkBackloggedProcesses();
        }
        logger.log(Level.INFO, "Finished adelivering messages {0}", toDeliver);
    }

    /**
     * Deliver messages in adelivI that are also in aUndelivered.
     */
    protected void atomicDelivery() {
        // do the loop while there are still messages to be delivered
        while (nextAdel != null || adelivI.hasNext()) {
            // if no message is pending : take next one in adelivI list
            if (nextAdel == null) {
                nextAdel = (GUID) adelivI.next();
            }
            // if the message that was being processed
            // has been received: deliver
            ContentRB toDeliver = (ContentRB) aUndelivered.get(nextAdel);
            if (toDeliver != null) {

                NekoMessage t =
                    new NekoMessage(toDeliver.getSource(),
                            all, //FIXME : this should be
                                    // the original destinations of the message
                            toDeliver.getProtocolId(),
                            toDeliver.getContent(),
                            toDeliver.getType());

                // adeliver this message to the application
                receiver.deliver(t);
                nextAdel = null;
                //hasRequestedMissing = false;
            } else {
                // do nothing (wait for rbcast to deliver the missing message)

                /*if(!hasRequestedMissing) {
                // send a REQUEST_MSG message
                rbcast.rSend(new NekoMessage(myID,
                // all processes except me
                others,
                // the ID of the missing msg
                nextAdel,
                REQUEST_MSG
                ));
                hasRequestedMissing = true;
                }*/

                // else: we can do nothing... return
                return;
            }
        }

    }


    /**
     * Method for processing the token. This method tries to take a
     * decision on the proposal contained in the token, adds a
     * proposal to the token if possible and updates the
     * adelivI set
     *
     * @param token Token to be processed
     */
    private void processToken(Token token) {

        // if this is an old token, try to extract useful information,
        // then discard it...
        if (token.round < roundI) {
            // store the messages that are proposed
            for (int i = 0; i < token.proposals.size(); i++) {
                aProposalBackups.addAll(token.getProposal(i).msgs);
            }
            // extract all the information from token.adeliv
            atomicDeliveryTokenAdeliv(token.adeliv);
            return;
        } else {
            logger.log(Level.INFO, "Token {0} is valid (roundI = {1})",
                new Object[]{token, new Integer(roundI)});
        }

        // send all stored messages if send-upon-holding-token is used
        if (!sendAtOnce) {
            synchronized (toBeSent) {
                // send each message
                for (Iterator sendable = toBeSent.iterator();
                        sendable.hasNext();)
                {
                    rbcast.send((NekoMessage) sendable.next());
                }
                // clear the array of sendable messages
                toBeSent.clear();
            }
        }


        // start processing the token
        // (that we know is in our round or more recent)
        roundI = token.round;



        // add all messages that have been ordered by the token
        // to our ordered sequence
        atomicDeliveryTokenAdeliv(token.adeliv);


        // first case: this process is more up to date than the token
        // (the first condition avoids the NullPointerException
        // in the case token.adeliv is empty)
        if ((token.adeliv.getMsgs().isEmpty() && !adelivI.isEmpty())
                || (!token.adeliv.getMsgs().isEmpty()
                    && (token.adeliv.lastPos() < adelivI.lastPos())))
        {
            // empty the proposal sequence of the token
            logger.log(Level.INFO, "The token in round {0} does not contain "
                    + "any new order information", new Integer(roundI));
            if (!token.adeliv.getMsgs().isEmpty() && !adelivI.isEmpty()) {
                logger.log(Level.INFO,
                        "Token.adeliv.lastPos() = {0}, adelivI.lastPos() = {1}",
                        new Object[]{new Integer(token.adeliv.lastPos()),
                                     new Integer(adelivI.lastPos())});
            }
            // remove the proposals that we know have been decided upon
            // (i.e. such that the sum of token.adeliv.lastPos() and the size
            // of all previous proposals is smaller than adelivI.lastPos())
            int lastProposal = token.adeliv.lastPos();
            while (lastProposal < adelivI.lastPos()
                    && token.proposalsSize() > 0)
            {
                lastProposal += token.removeProposal().msgs.size();
            }
            // the following assertion always holds (I think...)
            assert (lastProposal == adelivI.lastPos()
                    || token.proposalsSize() == 0);


        } else {
            // second case: the token is more up to date than this process.
            // if the token was received from the predecessor and
            // the proposal is valid, then increment votes
            if (token.sender == predecessor
                    && !token.proposals.isEmpty())
            {
                // increment the votes
                for (int i = 0; i < token.proposals.size(); i++) {
                    token.getProposal(i).incrementVotes();

                    logger.log(Level.CONFIG,
                            "Incrementing votes in round {1}. Now at {0}",
                            new Object[]{
                                new Integer(token.getProposal(i).votes),
                                new Integer(roundI)
                            });
                }
                // else reset votes to 1
            } else {
                logger.log(Level.INFO, "Resetting votes in round {0}",
                        new Integer(roundI));

                for (int i = 0; i < token.proposals.size(); i++) {
                    token.getProposal(i).resetVotes();
                }
            }

            int deliveredUpTo = 0;
            for (Iterator it = token.proposals.iterator(); it.hasNext();) {
                Token.Proposal current = (Token.Proposal) it.next();
                // check if a decision can be made
                if (current.votes >= f + 1) {
                    logger.log(Level.INFO, "Round {0}, Deciding on {1}",
                            new Object[]{new Integer(roundI), current.msgs});
                    // deliver new messages
                    atomicDeliveryNew(current.msgs);

                    deliveredUpTo++;
                }
            }
            // keep the proposals that were not delivered
            token.proposals =
                token.proposals.subList(deliveredUpTo, token.proposals.size());
        }

        // can we propose new messages?
        boolean canProposeNew = (!adelivI.hasHoles()) && (
                // multipleProposals allows up to f proposals
                (multipleProposals && token.proposals.size() < f)
                // otherwise, a single proposal is allowed
                || (!multipleProposals && token.proposals.size() == 0)
                );

        if (canProposeNew) {
            // messages that will be in the new proposal
            List newProposal = new ArrayList();

            // extract messages from aUnordered/aUndelivered
            // that can be added to proposalSeq
        newMessages:
            for (Iterator it = aUnordered.iterator();
                    it.hasNext() && newProposal.size() < maxProposalSize;)
            {
                // current aUndelivered extracted from GUID in aUnordered
                ContentRB current = (ContentRB) aUndelivered.get(it.next());

                // check if the message is not already in a proposal
                for (Iterator props = token.proposals.iterator();
                        props.hasNext();)
                {
                    Token.Proposal curr = (Token.Proposal) props.next();
                    if (curr.msgs.contains(current)) {
                        // message is already being ordered, go the the next one
                        continue newMessages;
                    }
                }

                newProposal.add(current);
            }

            // add a new proposal to the token
            if (newProposal.size() > 0) {
                token.addProposal(newProposal);
            }
        } else {
            logger.log(Level.INFO, "Cannot propose message in round {0}",
                       new Integer(roundI));
        }

        // extract the part of adelivI that will be sent with the token
        Adeliv.Subsequence adelivToSend = null;
        // 1st case: this is the first token ever received or no message has
        // been ordered yet => send the whole adelivI sequence
        if (lastToken == null || token.adeliv.getMsgs().isEmpty()) {
            adelivToSend = adelivI.tailList(0);
            // 2nd case: we have just ordered messages => send these ordered
            // messages (if reduceTokenSize is true)
        } else {

            int firstInNewToken = token.adeliv.startPos();
            int firstInOldToken = lastToken.adeliv.getMsgs().size() == 0
                ? -1
                : lastToken.adeliv.startPos();
            int lastInOldToken = lastToken.adeliv.getMsgs().size() == 0
                ? 0
                : lastToken.adeliv.lastPos();

            // check if there might have been messages in the ordered
            // sequence that were not sent in the last token
            int startPos = -1;
            if (firstInOldToken > firstInNewToken) {
                startPos = firstInNewToken;
            } else {
                startPos = Math.max(firstInNewToken, lastInOldToken);
            }
            adelivToSend = adelivI.tailList(startPos);

            logger.log(Level.INFO, "Round {0}, firstInNewToken = {1}, "
                       + "lastInOldToken = {2}, adelivI.lastPos() is {3}, "
                       + "has Holes ? {4}, startPos = {5}, adelivToSend={6}",
                       new Object[] {
                           new Integer(roundI),
                           new Integer(firstInNewToken),
                           new Integer(lastInOldToken),
                           new Integer(adelivI.lastPos()),
                           Boolean.valueOf(adelivI.hasHoles()),
                           new Integer(startPos),
                           adelivToSend
                       });

        }

        lastToken = new Token(myID, // my identity
                roundI, // the round of circulation
                // proposal for the next ordering
                token.proposals,
                // adelivI (the 'new' part, i.e.
                // that hasn't already circulated one round)
                adelivToSend
                );

        // send the token to the destinations
        rbcast.send(new NekoMessage(myID, tokenDestI.array(),
                    getId(),
                    lastToken,
                    TOKEN
                    ));



        //update the round number
        roundI++;

        // add all proposed messages to the backup set
        for (Iterator it = lastToken.proposals.iterator(); it.hasNext();) {
            Token.Proposal current = (Token.Proposal) it.next();
            aProposalBackups.addAll(current.msgs);
        }

        // deliver any new messages to the application
        atomicDelivery();


    }

    private class BackloggedProcess {
        int pid = -1;
        int firstRequested = -1;
        int lastRequested = -1;

        BackloggedProcess(int pid, int firstRequested, int lastRequested) {
            this.pid = pid;
            this.firstRequested = firstRequested;
            this.lastRequested = lastRequested;
        }

        int getId() {
            return pid;
        }

        int getFirst() {
            return firstRequested;
        }

        int getLast() {
            return lastRequested;
        }
    }

    private static final Logger logger =
        NekoLogger.getLogger(TokenAndFDBased.class.getName());
}

