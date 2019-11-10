package lse.neko.abcast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import lse.neko.util.Adeliv;
import lse.neko.util.logging.NekoLogger;

/**
 * Class representing a token in the token-based atomic broadcast
 * algorithm.
 */
public class Token implements Serializable, Comparable {

    /**
     * Serial version UID for this token class.
     */
    static final long serialVersionUID = -8307126753980970700L;

    /**
     * NekoProcess ID of the Sender of the token. Should be final, but
     * it is kept as it is because of serialization issues.
     */
    int sender;

    /**
     * Round in which this token circulates. Not final: can
     * change if the token is received by p_0..f from p_n-f..n-1.
     */
    int round;

    /**
     * SortedSet of GUID objects (identifiers of messages)
     * that have been ordered.
     */
    transient Adeliv.Subsequence adeliv;

    /**
     * Sequence of proposals that are currently transported
     * by the token. A process can add a proposal as long as the
     * number of proposals is smaller or equal than f
     */
    transient List proposals;

        /**
         * Constructs an empty token (with empty lists/sequences of messages).
         * @param sender The sender of this token
         * (should be process 0 or a process among p_{n-f}..p_{n})
         * @param round The round of this token (should be 0 or -1)
     */

    public static Token empty(int sender, int round) {
        return new Token(sender,
                         round,
                         new LinkedList(),
                         new Adeliv.Subsequence(0, new LinkedList()));
    }

    /**
     * Constructs a token based on information contained in a given
     * token. This constructor actually creates a deep clone of
     * the parameter token.
     *
     * @param token The token to be copied into this
     */
    public Token(Token token) {
        this(token.sender, token.round, token.proposals, token.adeliv);
    }

    /**
     * Constructor of the token. The parameters correspond to the
     * content of the token and are cloned before being put in the
     * token (i.e. the original data structures cannot be modified
     * by calls that affect the token)
     */
    public Token(int sender, int round, List proposals,
                 Adeliv.Subsequence adeliv)
    {
        this.sender = sender;
        this.round = round;
        this.proposals = new LinkedList();
        for (Iterator it = proposals.iterator(); it.hasNext();) {
            this.proposals.add(new Proposal(it.next()));
        }
        this.adeliv = new Adeliv.Subsequence(adeliv);
    }


    public String toString() {
        return "Token from p" + sender + ", round " + round
            + ".\n\t- Proposal : " + proposals
            + "\n\t- Adeliv : " + adeliv;
    }

    public void addProposal(List msgs) {
        proposals.add(new Proposal(msgs));
    }

    public Proposal removeProposal() {
        return (Proposal) proposals.remove(0);
    }

    public Proposal getProposal(int i) {
        return (Proposal) proposals.get(i);
    }

    public int proposalsSize() {
        return proposals.size();
    }

    public int compareTo(Object o) {
        Token t = (Token) o;

        // if this.round is older than t.round, return -1 (older)
        if (round < t.round
            || (round == t.round && sender < t.sender))
        {
            return -1;
        } else if (round == t.round && sender == t.sender) {
            // compareTo returns 0 if the round and the sender of both tokens
            // are the same
            return 0;
        } else {
            // other cases (round > t.round ||
            //              (round == t.round && sender > t.sender))
            // => this is newer than t
            return 1;
        }
    }

    /**
     * Serialization of Token. Starts by writing int values
     * (writes sender, round : 2x int), then writes each one
     * of the proposals. Finally, the adelivered sequence is
     * written
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
        logger.finest("Writing " + this);
        s.writeInt(sender);
        s.writeInt(round);

        // write the proposals
        s.writeInt(proposals.size());
        for (Iterator it = proposals.iterator(); it.hasNext();) {
            s.writeObject((Proposal) it.next());
        }

        s.writeObject(adeliv);
    }

    private void readObject(ObjectInputStream s)
        throws IOException, ClassNotFoundException
    {

        sender = s.readInt();
        round = s.readInt();

        // read the number of proposals + the proposals themselves
        int proposalsSize = s.readInt();
        this.proposals = new LinkedList();
        for (int i = 0; i < proposalsSize; i++) {
            this.proposals.add((Proposal) s.readObject());
        }

        // read the adeliv sequence
        this.adeliv = (Adeliv.Subsequence) s.readObject();

        logger.fine("Read token: Adeliv : " + this.adeliv
                    + ", propSeq : " + this.proposals);
    }

    /**
     * Class representing a token proposal.
     */
    static class Proposal implements Serializable {

        /**
         * Votes accumulated by this proposal.
         */
        int votes;

        /**
         * The proposal itself.
         */
        transient List msgs;

        /**
         * Constructor for creating a proposal based on a previous proposal.
         */
        public Proposal(Object o) {
            Proposal t = (Proposal) o;
            this.votes = t.votes;
            this.msgs = new LinkedList(t.msgs);
        }

        /**
         * Constructor : creates a proposal with 1 initial vote.
         */
        public Proposal(List msgs) {
            this.msgs = msgs;
            this.votes = 1;
        }

        /**
         * increments the votes of this proposal.
         */
        public void incrementVotes() {
            votes++;
        }

        /**
         * reset the votes (votes = 1) of this proposal.
         */
        public void resetVotes() {
            votes = 1;
        }

        public String toString() {
            return msgs + " (votes=" + votes + ")";
        }

        private void writeObject(ObjectOutputStream s) throws IOException {
            s.writeInt(votes);

            s.writeInt(msgs.size()); // write size of the proposal
            for (Iterator it = msgs.iterator(); it.hasNext();) {
                s.writeObject(it.next());
            }
        }

        private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException
        {
            votes = s.readInt();
            int size = s.readInt();
            this.msgs = new LinkedList();

            for (int i = 0; i < size; i++) {
                this.msgs.add(s.readObject());
            }
        }
    }

    /**
     * Class representing the Destination. Its internal representation
     * is a Set of Integers representing the IDs of the destination processes
     */
    public static class Destination extends HashSet {
        int [] destArray = null;

        /**
         * Constructor for the Token.Destination class. Initially, only
         * the successor of this process is in the set
         *
         * @param f Number of tolerated failures
         * @param myID Identifier of the process using this destination set
         * @param n Number of processes in the system
         */
        Destination(int f, int myID, int n) {
            // create a hash set with an initial capacity of f+1
            // and a load factor of 1.0
            super(f + 1, 1.0f);
            // add the identifier of my successor to the destination list
            super.add(new Integer((myID + 1) % n));
            // the array only contains this' successor
            destArray = new int[]{(myID + 1) % n};
        }

        /**
         * Adds a process to the set of token destinations.
         */
        synchronized void add(int proc) {
            super.add(new Integer(proc));
            destArray = null;
        }

        /**
         * Removes a process from the set of token destinations.
         */
        synchronized void remove(int proc) {
            super.remove(new Integer(proc));
            destArray = null;
        }

        /**
         * Returns an array representation of the set of token destinations.
         */
        synchronized int [] array() {
            if (destArray != null) {
                return destArray;
            } else {
                // create a new int array
                destArray = new int[super.size()];
                int i = 0;
                // iterate through dest and put all elements in destArray
                for (Iterator it = super.iterator(); it.hasNext(); i++) {
                    destArray[i] = ((Integer) it.next()).intValue();
                }
                // return the newly created array
                return destArray;
            }
        }

        public String toString() {
            String result = "TokenDestination : {";
            for (Iterator it = super.iterator(); it.hasNext();) {
                result += " " + it.next() + " ";
            }
            result += "}";

            return result;
        }

    }

    private static final Logger logger =
        NekoLogger.getLogger(Token.class.getName());


}


