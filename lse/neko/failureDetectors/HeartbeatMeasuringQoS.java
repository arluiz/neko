package lse.neko.failureDetectors;

// lse.neko imports:
import lse.neko.NekoProcess;


/**
 * A heartbeat failure detector that measures some
 * QoS parameters. FIXME: as for the design, this class implements
 * something more general than Heartbeat, so
 * we should use something else than inheritance.
 */
public class HeartbeatMeasuringQoS
    extends Heartbeat
{

    public HeartbeatMeasuringQoS(NekoProcess process,
                                 double tSend,
                                 double tReceive,
                                 int[] group)
    {
        super(process, tSend, tReceive, group);
    }

    public HeartbeatMeasuringQoS(NekoProcess process,
                                 double tSend,
                                 double tReceive)
    {
        super(process, tSend, tReceive);
    }

    private class QoSData {

        public QoSData(boolean suspected) {

            this.suspected = suspected;
            time = process.clock();
            suspectedDuration = 0;
            trustedDuration = 0;
            numTSTransition = 0;
            numSTTransition = 0;
        }

        private boolean suspected;
        private double suspectedDuration;
        private double trustedDuration;
        private double time;
        private int numTSTransition;
        private int numSTTransition;

        public synchronized void suspect(boolean newSuspected) {

            double newTime = process.clock();
            double duration = newTime - time;
            if (duration == 0) {
                // to avoid division by zero later
                duration = 1e-6;
            }
            time = newTime;

            if (suspected) {
                suspectedDuration += duration;
            } else {
                trustedDuration += duration;
            }

            if (!suspected && newSuspected) {
                numTSTransition++;
            } else if (suspected && !newSuspected) {
                numSTTransition++;
            }

            suspected = newSuspected;
        }

        /**
         * Extract the QoS parameters.
         * @param out parameter. tM (mistake duration)
         * followed by tMR (mistake recurrence time).
         */
        public synchronized FailureDetectorQoS getQoS() {

            // just to update the stored values
            suspect(suspected);

            // now comes the computation.
            //
            // s := suspectedDuration
            // t := trustedDuration
            // nst := numSTTransition
            // nts := numTSTransition
            //
            // We assume that the observation started when the
            // FD behavior was stabilized and that the observation
            // does not interfere with the FD.
            // Conclusion: tM / tMR == s / (s+t)
            //
            // If nts == nst then tM = s / nst.
            // If nts != nst then they are 1 apart, thus
            //   s / max(nst, nts) <= tM <= s / min(nst, nts).
            //   We use s / ((nst+nts)/2) as an estimate.

            double tM;
            double tMR;

            if (numSTTransition == 0 && numTSTransition == 0) {

                tM = suspectedDuration; // might be 0
                tMR = suspectedDuration + trustedDuration;

            } else {

                tM = suspectedDuration * 2
                    / (numTSTransition + numSTTransition);
                tMR = tM / suspectedDuration
                    * (suspectedDuration + trustedDuration);

            }

            return new FailureDetectorQoS(tM, tMR);

        }
    }

    public static class FailureDetectorQoS {

        public FailureDetectorQoS(double tM, double tMR) {
            this.tM = tM;
            this.tMR = tMR;
        }

        private double tM;
        private double tMR;

        public double getTM() { return tM; }
        public double getTMR() { return tMR; }
    }

    protected void suspect(int id) {

        if (suspected[id]) {
            return;
        }
        if (qosData != null) {
            qosData[id].suspect(true);
        }
        super.suspect(id);
    }

    protected void unsuspect(int id) {

        if (!suspected[id]) {
            return;
        }
        if (qosData != null) {
            qosData[id].suspect(false);
        }
        super.unsuspect(id);
    }

    private QoSData[] qosData;

    public synchronized void clear() {

        qosData = new QoSData[process.getN()];
        for (int i = 0; i < qosData.length; i++) {
            if (i != process.getID()) {
                qosData[i] = new QoSData(suspected[i]);
            }
        }
    }

    /**
     * return_value[i] holds the QoS parameters of the failure
     * detector component monitoring process #i.
     */
    public FailureDetectorQoS[] getQoS() {
        FailureDetectorQoS[] qos = new FailureDetectorQoS[qosData.length];
        for (int i = 0; i < qosData.length; i++) {
            if (qosData[i] == null) {
                continue;
            }
            qos[i] = qosData[i].getQoS();
        }
        return qos;
    }

}

