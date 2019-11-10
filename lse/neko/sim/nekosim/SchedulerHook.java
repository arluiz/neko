package lse.neko.sim.nekosim;

/**
 * Objects that implement this interface can be used
 * to modify the behavior of the scheduler.
 * This is necessary when Neko is integrated with another
 * simulation engine.
 * @see NekoSimSystem#registerSchedulerHook
 */
public interface SchedulerHook {

    /**
     * This function is called whenever Neko is stepping
     * the simulation time. More precisely: once Neko has executed
     * all actions scheduled for a time t, it calls this function.
     * The clock still shows t, and the <code>nextTime</code>
     * argument gives the time of the next registered action.
     * This function may schedule actions, even actions that should
     * happen before <code>nextTime</code>.
     * Upon return from this function, Neko steps the simulation
     * clock (to <code>nextTime</code> or an earlier time)
     * and executes all actions at that time.
     *
     * @param nextTime the time of the next scheduled action.
     * Double.MAX_VALUE means that Neko has no next action
     * (the simulation is about to finish).
     * @see NekoSimSystem#clock
     */
    void steppingTime(double nextTime);

}
