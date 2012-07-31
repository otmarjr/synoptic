package dynoptic.model.fifosys.gfsm;

import java.util.LinkedHashSet;
import java.util.Set;

import dynoptic.model.fifosys.FifoSys;
import dynoptic.model.fifosys.channel.ChannelId;

/**
 * <p>
 * A GFSM captures the execution space of a CFSM. We use this model to (1)
 * maintain the observed states/event, and (2) to carry out complex operations
 * like refinement.
 * </p>
 * <p>
 * This model is easier to deal with than a CFSM because it captures all global
 * information in a single place (e.g., all enabled transitions from a single
 * global configuration). A CFSM can be thought of as an abstraction of a GFSM
 * -- a CFSM does not deal with concrete observations. The CFSM model is useful
 * for visualization and for input to the McScM model checker.
 * </p>
 * <p>
 * A GFSM can also be thought of as a representation of the operational
 * semantics, or some number of executions of some hidden/abstract CFSM. Note
 * that although it captures or describes prior executions, it cannot actually
 * be executed or maintain instantaneous execution state -- for this, use
 * FifoSysExecution.
 * </p>
 * <p>
 * A GFSM is composed of GFSMStates, which are actually _partitions_ of the
 * observed global configurations. Refinement causes a re-shuffling of the
 * observations, and new partitions to be created and added to the GFSM.
 * Therefore, a GFSM is highly mutable. Each mutation of the GFSM is a single
 * complete step of the Dynoptic algorithm.
 * </p>
 */
public class GFSM extends FifoSys<GFSMState> {

    //
    // NOTE: The processInits/processAccepts were intended as a cache
    // optimization, but since GFSMStates can mutate, it is non-trivial to keep
    // this cache up to date without GFSMStates signaling to the container GFSM.
    // For now, this is optimization is disabled.
    //
    // Per-process initial and accept states, ordered by process id. That is,
    // processInits[0] contains the set of all GFSMState instances that contain
    // at least one observation in which process id 0 was in initial state.
    // List<Set<GFSMState>> processInits;
    // List<Set<GFSMState>> processAccepts;
    //
    // Note that this.initStates and this.acceptStates are global init/accept
    // states. That is, these are states that contain at least one
    // observation per process, for all processes, in which the process is in
    // initial/accept state.
    //
    // For n processes, these sets satisfy the invariants:
    // processInits[0] \intersect processInits[1] \intersect ... \intersect
    // processInits[n-1] = initStates
    //
    // And likewise for acceptStates

    public GFSM(int numProcesses, Set<ChannelId> channelIds) {
        super(numProcesses, channelIds);
        // Per-process Inits and Accepts Sets will be created on demand as new
        // GFSM states are added. They will also be de-allocated when empty.
        // processInits = new ArrayList<Set<GFSMState>>(numProcesses);
        // processAccepts = new ArrayList<Set<GFSMState>>(numProcesses);
    }

    // //////////////////////////////////////////////////////////////////

    @Override
    public Set<GFSMState> getInitStates() {
        Set<GFSMState> ret = new LinkedHashSet<GFSMState>();
        for (GFSMState s : states) {
            if (s.isInitial()) {
                ret.add(s);
            }
        }
        return ret;
    }

    @Override
    public Set<GFSMState> getAcceptStates() {
        Set<GFSMState> ret = new LinkedHashSet<GFSMState>();
        for (GFSMState s : states) {
            if (s.isAccept()) {
                ret.add(s);
            }
        }
        return ret;
    }

    // //////////////////////////////////////////////////////////////////

    /** Returns the set of partitions that are accepting for a pid. */
    public Set<GFSMState> getAcceptStatesForPid(int pid) {
        Set<GFSMState> ret = new LinkedHashSet<GFSMState>();
        for (GFSMState s : states) {
            if (s.isAcceptForPid(pid)) {
                ret.add(s);
            }
        }
        return ret;

        // if (processAccepts.get(pid) != null) {
        // return processAccepts.get(pid);
        // }
        // return Collections.emptySet();
    }

    /** Returns the set of partitions that are initial for a pid. */
    public Set<GFSMState> getInitialStatesForPid(int pid) {
        Set<GFSMState> ret = new LinkedHashSet<GFSMState>();
        for (GFSMState s : states) {
            if (s.isInitialForPid(pid)) {
                ret.add(s);
            }
        }
        return ret;

        // if (processInits.get(pid) != null) {
        // return processInits.get(pid);
        // }
        // return Collections.emptySet();
    }

    /** Adds a new partition/state s to this GFSM. */
    public void addGFSMState(GFSMState s) {
        assert !states.contains(s);

        states.add(s);

        // Update global init/accept states.
        // if (s.isAccept()) {
        // acceptStates.add(s);
        // }
        // if (s.isInitial()) {
        // initStates.add(s);
        // }
        //
        // // Update per-pid init/accept states.
        // for (int pid = 0; pid < numProcesses; pid++) {
        // if (s.isAcceptForPid(pid)) {
        // addStateToList(processAccepts, pid, s);
        // }
        // if (s.isInitialForPid(pid)) {
        // addStateToList(processInits, pid, s);
        // }
        // }

        recomputeAlphabet();
    }

    /** Removes the partition/state s from this GFSM. */
    public void removeGFSMState(GFSMState s) {
        assert states.contains(s);

        states.remove(s);

        // Update global init/accept states.
        // if (s.isAccept()) {
        // assert acceptStates.contains(s);
        // acceptStates.remove(s);
        // }
        // if (s.isInitial()) {
        // assert initStates.contains(s);
        // initStates.remove(s);
        // }
        //
        // // Update per-pid init/accept states.
        // for (int pid = 0; pid < numProcesses; pid++) {
        // if (s.isAcceptForPid(pid)) {
        // rmStateFromList(processAccepts, pid, s);
        // }
        // if (s.isInitialForPid(pid)) {
        // rmStateFromList(processInits, pid, s);
        // }
        // }

        recomputeAlphabet();
    }

    // //////////////////////////////////////////////////////////////////

    // /** Adds s to the set at list[pid]; creates this set if it doesn't exist.
    // */
    // private void addStateToList(List<Set<GFSMState>> list, int pid, GFSMState
    // s) {
    // assert pid >= 0 && pid < numProcesses;
    //
    // Set<GFSMState> set;
    // if (list.get(pid) == null) {
    // set = new LinkedHashSet<GFSMState>();
    // list.set(pid, set);
    // } else {
    // set = list.get(pid);
    // }
    // set.add(s);
    // }
    //
    // /**
    // * Removes s from the set at list[pid]; if the set is then empty, assigns
    // * list[pid] to null.
    // */
    // private void rmStateFromList(List<Set<GFSMState>> list, int pid,
    // GFSMState s) {
    // assert pid >= 0 && pid < numProcesses;
    // Set<GFSMState> set = list.get(pid);
    // assert set.contains(s);
    //
    // set.remove(s);
    // if (set.size() == 0) {
    // list.set(pid, null);
    // }
    // }
}
