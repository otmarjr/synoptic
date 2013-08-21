/*
 * This code is in part based on the code from Clemens Hammacher's
 * implementation of a partition refinement algorithm for Bisimulation
 * minimization.
 * 
 * Source: https://ccs.hammacher.name
 * 
 * License: Eclipse Public License v1.0.
 */

package synoptic.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import synoptic.algorithms.graphops.IOperation;
import synoptic.algorithms.graphops.PartitionMerge;
import synoptic.algorithms.graphops.PartitionMultiSplit;
import synoptic.algorithms.graphops.PartitionSplit;
import synoptic.benchmarks.PerformanceMetrics;
import synoptic.benchmarks.TimedTask;
import synoptic.invariants.CExamplePath;
import synoptic.invariants.ITemporalInvariant;
import synoptic.invariants.TemporalInvariantSet;
import synoptic.invariants.constraints.TempConstrainedInvariant;
import synoptic.main.SynopticMain;
import synoptic.model.EventNode;
import synoptic.model.Partition;
import synoptic.model.PartitionGraph;
import synoptic.model.interfaces.ITransition;
import synoptic.util.InternalSynopticException;
import synoptic.util.time.ITime;

/**
 * Partition graphs can be transformed using two algorithms -- coarsening and
 * refinement. This class implements refinement using the Bisim algorithm (
 * {@code Bisimulation.splitUntilAllInvsSatisfied}). Coarsening is implemented
 * with a modified version of the kTails algorithm (
 * {@code Bisimulation.mergePartitions}). This algorithm merges partitions in
 * the partition graph without unsatisfying invariants that are satisfied.
 */
public class Bisimulation {
    public static Logger logger = Logger.getLogger("Bisimulation");

    /**
     * Consider incoming transitions for splitting TODO: expose this as a
     * command line option
     */
    private static boolean incomingTransitionSplit = true;

    /** Suppress default constructor for non-instantiability */
    private Bisimulation() {
        throw new AssertionError();
    }

    /**
     * Splits the partitions in {@code pGraph} until ALL synoptic.invariants
     * returned by {@code pGraph.getInvariants()} are satisfied.
     * 
     * @param pGraph
     *            the partition graph to refine\split
     */
    public static void splitUntilAllInvsSatisfied(PartitionGraph pGraph) {
        // TODO: assert that the pGraph represents totally ordered traces.

        TimedTask refinement = PerformanceMetrics.createTask("refinement",
                false);
        SynopticMain syn = SynopticMain.getInstanceWithExistenceCheck();
        if (syn.options.dumpIntermediateStages) {
            syn.exportNonInitialGraph(syn.getIntermediateDumpFilename("r", 0),
                    pGraph);
        }

        int numSplitSteps = 0;
        int prevNumSplitSteps = 0;

        Set<ITemporalInvariant> unsatisfiedInvariants = new LinkedHashSet<ITemporalInvariant>();
        unsatisfiedInvariants.addAll(pGraph.getInvariants().getSet());
        Set<ITemporalInvariant> satisfiedInvariants = new LinkedHashSet<ITemporalInvariant>();

        List<CExamplePath<Partition>> counterExampleTraces = null;

        while (true) {
            // Recompute the counter-examples for the unsatisfied invariants.
            counterExampleTraces = new TemporalInvariantSet(
                    unsatisfiedInvariants).getAllCounterExamples(pGraph);

            if (counterExampleTraces == null
                    || counterExampleTraces.size() == 0) {
                logger.fine("Invariants satisfied. Stopping.");
                break;
            }

            // /////////
            // Update the sets with satisfied/unsatisfied invariants.

            // NOTE: By performing a split we might satisfy more than just the
            // invariant the split was intended to satisfy. Therefore, by just
            // considering these invariants we would be under-approximating the
            // invariants we've satisfied, and over-approximating unsatisfied
            // invariants. Instead, we re-compute counter-examples AFTER all the
            // splits and rely on these for determining the set of
            // satisfied/unsatisfied invariants.

            unsatisfiedInvariants.clear();
            for (CExamplePath<Partition> relPath : counterExampleTraces) {
                unsatisfiedInvariants.add(relPath.invariant);
            }
            satisfiedInvariants.clear();
            satisfiedInvariants.addAll(pGraph.getInvariants().getSet());
            satisfiedInvariants.removeAll(unsatisfiedInvariants);
            // /////////
            // logger.fine("New graph size: " + pGraph.getNodes().size()
            // + ", unsat invs remaining: " + unsatisfiedInvariants.size());

            // Perform the splitting.
            prevNumSplitSteps = numSplitSteps;
            numSplitSteps = performSplits(numSplitSteps, pGraph,
                    counterExampleTraces);

            if (numSplitSteps == prevNumSplitSteps) {
                // No splits were performed, which means that we could not
                // eliminate the present counter-examples. Since this function
                // should only be applied to totally ordered traces, this is a
                // bug (this is known to be possible for partially ordered
                // traces).

                throw new InternalSynopticException(
                        "Could not satisfy invariants: "
                                + unsatisfiedInvariants);
            }

        }

        if (syn.options.dumpIntermediateStages) {
            syn.exportNonInitialGraph(
                    syn.getIntermediateDumpFilename("r", numSplitSteps), pGraph);
        }

        PerformanceMetrics.get().record("numOfSplitSteps", numSplitSteps);
        refinement.stop();
    }

    /**
     * Performs a single arbitrary split if we could not find any splits that
     * satisfy a previously unsatisfied invariant. If we did find such splits,
     * then we perform ALL of them. Returns the updated numSplitSteps count,
     * which is incremented by the number of splits applied to the pGraph.
     * 
     * @param numSplitSteps
     *            The number of split steps made so far.
     * @param pGraph
     *            The graph, whose partitions we will split.
     * @param counterExampleTraces
     *            A list of counter-example traces that we attempt to eliminate
     *            by splitting.
     * @return The updated numSplitSteps count.
     */
    public static int performSplits(int numSplitSteps, PartitionGraph pGraph,
            List<CExamplePath<Partition>> counterExampleTraces) {

        // Stores all splits that cause an invariant to be satisfied, indexed by
        // partition to which they are applied.
        LinkedHashMap<Partition, PartitionMultiSplit> splitsToDoByPartition = new LinkedHashMap<Partition, PartitionMultiSplit>();

        // If we have no counterexamples, then we are done.
        if (counterExampleTraces == null || counterExampleTraces.size() == 0) {
            return numSplitSteps;
        }

        // Permute the counter-examples, but do so deterministically for the
        // same random seed argument.
        Collections.shuffle(counterExampleTraces,
                SynopticMain.getInstanceWithExistenceCheck().random);

        // logger.fine("" + counterExampleTraces.size()
        // + " unsatisfied invariants and counter-examples: "
        // + counterExampleTraces);

        // The set of all invariants for which we have a split that makes the
        // graph satisfy the invariant.
        LinkedHashSet<ITemporalInvariant> newlySatisfiedInvariants = new LinkedHashSet<ITemporalInvariant>();

        // Contains the first valid split, which will be performed if no other
        // split (that would resolve an invariant) is available.
        IOperation arbitrarySplit;

        arbitrarySplit = getInvSatisfyingSplits(counterExampleTraces, pGraph,
                splitsToDoByPartition, newlySatisfiedInvariants);

        // String logStr;
        if (splitsToDoByPartition.size() == 0) {
            // We have no splits that resolve invariants. Perform an arbitrary
            // split, if we have one.
            if (arbitrarySplit == null) {
                logger.fine("no valid split available, exiting.");
                return numSplitSteps;
            }
            // logStr = "split[" + numSplitSteps + "] : arbitrary split: "
            // + arbitrarySplit;

            pGraph.apply(arbitrarySplit);

        } else {
            // We have splits that resolve invariants, perform all of them.
            // int i = 0;
            for (PartitionMultiSplit split : splitsToDoByPartition.values()) {
                pGraph.apply(split);
                // logger.fine("split[" + numSplitSteps + "." + i + "] : " +
                // split);
                // i++;
            }

            // logStr = "split[" + numSplitSteps + "] " + "new invs satisfied: "
            // + newlySatisfiedInvariants.size();
        }

        // logger.fine(logStr);

        if (SynopticMain.getInstanceWithExistenceCheck().options.dumpIntermediateStages) {
            SynopticMain.getInstanceWithExistenceCheck()
                    .exportNonInitialGraph(
                            SynopticMain.getInstanceWithExistenceCheck()
                                    .getIntermediateDumpFilename("r",
                                            numSplitSteps + 1), pGraph);
        }

        return numSplitSteps + 1;

    }

    /**
     * Merge partitions in pGraph that are k-equal (kTails equality), with k=0
     * without unsatisfying any of the pGraph invariants..
     * 
     * @param pGraph
     */
    public static void mergePartitions(PartitionGraph pGraph) {
        TemporalInvariantSet invariants = pGraph.getInvariants();
        mergePartitions(pGraph, invariants, 1);
    }

    /**************************************************************************/
    /** Private methods below. */

    /**
     * Compute possible splits to resolve the invariant violation shown by path
     * counterexampleTrace.
     * 
     * @param counterexampleTrace
     *            The path to remove
     * @param pGraph
     *            The graph from which the path shall be removed
     * @return A list of partition splits that resolve the invariant violation
     */
    private static List<PartitionSplit> getSplits(
            CExamplePath<Partition> counterexampleTrace, PartitionGraph pGraph) {

        // Constrained invariant
        if (counterexampleTrace.invariant instanceof TempConstrainedInvariant<?>) {
            return getSplitsConstrained(counterexampleTrace, pGraph);
        }

        // Unconstrained invariant
        {
            return getSplitsUnconstrained(counterexampleTrace, pGraph);
        }
    }

    /**
     * Compute possible splits to resolve the constrained invariant violation
     * shown by path counterexampleTrace. This is done by
     */
    private static List<PartitionSplit> getSplitsConstrained(
            CExamplePath<Partition> counterexampleTrace, PartitionGraph pGraph) {

        // Holds the return values.
        List<PartitionSplit> candidateSplits = new ArrayList<PartitionSplit>();

        // Our position while traversing the counter-example path
        Partition curPart = null;
        Partition endPart = null;

        // This method must only be passed counter-example paths for
        // constrained invariants
        assert counterexampleTrace.invariant instanceof TempConstrainedInvariant<?>;
        TempConstrainedInvariant<?> inv = (TempConstrainedInvariant<?>) counterexampleTrace.invariant;

        // TODO: Uncomment when the lower-bound subtypes are implemented
        // Check if this is a lower-bound constrained invariant
        // boolean isLower = false;
        // if (inv instanceof APLowerTracingSet || inv instanceof
        // AFbyLowerTracingSet) {
        // isLower = true;
        // }

        // Get the single relation of the invariant
        Set<String> invRelation = new HashSet<String>(1);
        invRelation.add(inv.getRelation());

        // Retrieve the counter-example path, its size (excluding terminal)
        List<Partition> cExPath = counterexampleTrace.path;
        int pathSize = counterexampleTrace.path.size() - 1;

        // Get the last non-terminal partition, and check for null
        endPart = cExPath.get(pathSize - 1);
        if (endPart == null) {
            throw new InternalSynopticException(
                    "Counter-example path with a null Partition");
        }

        // Retrieve the time deltas and the violated time bound
        List<ITime> tDeltas = counterexampleTrace.tDeltas;
        ITime tBound = inv.getConstraint().getThreshold();

        // Event paths between curPart and endPart which, if replacing the old
        // curPart->endPart, would resolve the violation
        Set<List<EventNode>> legalSubpaths;
        // Event paths between curPart and endPart which retain the violation
        Set<List<EventNode>> illegalSubpaths;

        // Walk the path in reverse
        for (int i = pathSize - 2; i > 0; --i) {

            // Get the current partition, and check for null
            curPart = cExPath.get(i);
            if (curPart == null) {
                throw new InternalSynopticException(
                        "Counter-example path with a null Partition");
            }

            // Only consider splitting on this partition if there is a stitch of
            // min/max transitions into and out of this partition
            if (!stitchExists(counterexampleTrace, i)) {
                continue;
            }

            legalSubpaths = new HashSet<List<EventNode>>();
            illegalSubpaths = new HashSet<List<EventNode>>();

            // Time to get to current partition from t=0 state
            ITime curTime = tDeltas.get(i);

            // Check if we're already over the time bound
            if (tBound.lessThan(curTime)) {
                continue;
            }

            // Any subpath <= this target time is legal
            ITime targetSubpathTime = tBound.computeDelta(curTime);
            curTime = null;

            // Walk through paths of EventNodes, finding any that run from
            // firstPart to secondPart and placing them in the list of
            // either legal or illegal subpaths
            for (EventNode curEv : curPart.getEventNodes()) {
                EventNode ev = curEv;

                // Initialize the current, ongoing subpath and its time
                List<EventNode> currentSubpath = new ArrayList<EventNode>();
                currentSubpath.add(ev);
                ITime currentSubpathTime = tBound.getZeroTime();

                // Walk the event path until an event within endPart is
                // encountered or the path ends
                while (!ev.isTerminal()) {

                    // Get the only transition out of this event with the
                    // relation of this invariant
                    ITransition<EventNode> trans = ev
                            .getTransitionsWithExactRelations(invRelation).get(
                                    0);

                    // Move to the next event, and update the subpath and
                    // running subpath time
                    ev = trans.getTarget();
                    currentSubpath.add(ev);
                    currentSubpathTime = currentSubpathTime.incrBy(trans
                            .getTimeDelta());

                    // We've found a curPart->endPart path if the new event is
                    // in endPart
                    if (ev.getParent() == endPart) {
                        // TODO: Make this lower-bound-friendly

                        // Illegal path which would not resolve the violation
                        if (targetSubpathTime.lessThan(currentSubpathTime)) {
                            illegalSubpaths.add(currentSubpath);
                        }

                        // Legal path which would resolve the violation
                        else {
                            legalSubpaths.add(currentSubpath);
                        }
                        break;
                    }
                }
            }

            // Both lists must be non-empty for a valid split to exist
            if (legalSubpaths.isEmpty() || illegalSubpaths.isEmpty()) {
                continue;
            }

            // TODO: Split on curPart
        }

        return candidateSplits;
    }

    /**
     * Check if the partition at index in the counter-example trace contains a
     * stitch, which means that the targets of all min/max transitions into this
     * partition and the sources of all min/max transitions out of this
     * partition are not equal sets.
     */
    private static boolean stitchExists(
            CExamplePath<Partition> counterexampleTrace, int index) {

        // Targets of all min/max transitions into this partition
        Set<EventNode> arrivingEvents = new HashSet<EventNode>();
        // Sources of all min/max transitions out of this partition
        Set<EventNode> departingEvents = new HashSet<EventNode>();

        // Populate events at which we can arrive from the previous partition in
        // the path
        for (ITransition<EventNode> arrivingTrans : counterexampleTrace.transitionsList
                .get(index)) {
            arrivingEvents.add(arrivingTrans.getTarget());
        }

        // Populate events from which we can depart to reach the next partition
        // in the path
        for (ITransition<EventNode> departingTrans : counterexampleTrace.transitionsList
                .get(index + 1)) {
            departingEvents.add(departingTrans.getSource());
        }

        // Non-equal sets means there is a stitch
        if (arrivingEvents.equals(departingEvents)) {
            return false;
        }
        return true;
    }

    /**
     * Compute possible splits to resolve the unconstrained invariant violation
     * shown by path counterexampleTrace. This is done by following the path in
     * the original (event) graph and determining the point where the partition
     * graph allows a transition it should not allow. The event graph is
     * accessed via the events stored by the partitions.
     */
    private static List<PartitionSplit> getSplitsUnconstrained(
            CExamplePath<Partition> counterexampleTrace, PartitionGraph pGraph) {
        /**
         * Holds the return values.
         */
        List<PartitionSplit> candidateSplits = new ArrayList<PartitionSplit>();
        /**
         * The messages (i.e. nodes in the original graph) that are on the
         * counterexampleTrace.
         */
        LinkedHashSet<EventNode> hot = new LinkedHashSet<EventNode>();
        hot.addAll(counterexampleTrace.path.get(0).getEventNodes());
        Partition prevPartition = null;
        Partition nextPartition = null;
        Partition curPartition = null;
        // logger.fine("" + counterexampleTrace.path);

        // TODO: retrieve an interned copy of this set
        String relation = counterexampleTrace.invariant.getRelation();
        Set<String> relationSet = new LinkedHashSet<String>();
        relationSet.add(relation);

        // Walk along the path
        for (Partition part : counterexampleTrace.path) {
            if (part == null) {
                throw new InternalSynopticException(
                        "Relation path with a null Partition");
            }
            prevPartition = curPartition;
            curPartition = nextPartition;
            nextPartition = part;
            hot.retainAll(part.getEventNodes());
            // If we cannot follow further, then we found the partition we need
            // to split.
            if (hot.size() == 0) {
                break;
            }
            // Compute the valid successor messages in the original trace.
            LinkedHashSet<EventNode> successorEvents = new LinkedHashSet<EventNode>();

            for (EventNode m : hot) {
                for (ITransition<EventNode> t : m
                        .getTransitionsWithIntersectingRelations(relationSet)) {
                    // successorEvents.addAll(m.getSuccessors(relations));
                    successorEvents.add(t.getTarget());
                }
            }
            hot = successorEvents;
        }

        ITransition<Partition> outgoingTransition = curPartition
                .getTransitionWithExactRelation(nextPartition, relationSet);
        ITransition<Partition> incomingTransition = null;
        if (prevPartition != null) {
            incomingTransition = prevPartition.getTransitionWithExactRelation(
                    curPartition, relationSet);
        }
        if (outgoingTransition != null) {
            // logger.fine("outgoingTrans:" + outgoingTransition);
            PartitionSplit newSplit = curPartition
                    .getCandidateSplitBasedOnOutgoing(outgoingTransition);
            // logger.fine("outgoingSplit:" + newSplit);
            if (newSplit != null) {
                candidateSplits.add(newSplit);
            }

        }
        if (incomingTransition != null && incomingTransitionSplit) {
            // logger.fine("incomingTrans:" + incomingTransition);

            Set<String> relations = incomingTransition.getRelation();
            PartitionSplit newSplit;
            if (relations.size() == 1) {
                // Single relation case.
                newSplit = curPartition.getCandidateSplitBasedOnIncoming(
                        prevPartition, relations);
            } else {
                // Multi-relational case.
                newSplit = curPartition.getCandidateSplitBasedOnIncoming(
                        prevPartition, relations);
            }

            // logger.fine("incomingSplit:" + newSplit);
            if (newSplit != null) {
                candidateSplits.add(newSplit);
            }
        }
        return candidateSplits;
    }

    /**
     * Performs the splitOp on the pGraph to see whether or not the resulting
     * graph has no other counter-examples for the invariant inv (i.e. whether
     * or not the graph after the split satisfies inv).
     * 
     * @param inv
     *            The invariant to check for satisfiability after the splitOp.
     * @param pGraph
     *            The partition graph to apply to the splitOp to.
     * @param splitOp
     *            The split operation to apply to pGraph
     * @return true if the split makes the graph satisfy the invariant, and
     *         false otherwise.
     */
    private static boolean splitSatisfiesInvariantGlobally(
            ITemporalInvariant inv, PartitionGraph pGraph,
            PartitionMultiSplit splitOp) {

        // Perform the split.
        IOperation rewindOperation = pGraph.apply(splitOp);

        // See if splitting resolved the violation.
        CExamplePath<Partition> violation = TemporalInvariantSet
                .getCounterExample(inv, pGraph);

        // Undo the split (rewind) to get back the input graph.
        pGraph.apply(rewindOperation);

        // The invariant has more violations after the split.
        if (violation != null) {
            return false;
        }
        // The split has no other violations once the split is
        // performed.
        return true;
    }

    /**
     * Returns an arbitrary split that resolves an arbitrary counter-example
     * trace in counterexampleTraces. Populates the splitsToDoByPartition map
     * with those splits that make a previously unsatisfied invariant true in
     * the new (refined) graph.
     * 
     * @param counterexampleTraces
     * @param pGraph
     * @param splitsToDoByPartition
     *            The HashMap recording splits by partition -- updated to
     *            include all splits that make the graph satisfy previously
     *            unsatisfied invariants.
     * @param newlySatisfiedInvariants
     * @return an arbitrary split that may be useful in the case that
     *         splitsToDoByPartition is empty and there are no splits that lead
     *         to new invariant satisfaction.
     */
    private static IOperation getInvSatisfyingSplits(
            List<CExamplePath<Partition>> counterexampleTraces,
            PartitionGraph pGraph,
            HashMap<Partition, PartitionMultiSplit> splitsToDoByPartition,
            Set<ITemporalInvariant> newlySatisfiedInvariants) {

        IOperation arbitrarySplit = null;
        SynopticMain syn = SynopticMain.getInstanceWithExistenceCheck();

        // TODO: we are considering counter-example traces in an arbitrary
        // order. This heuristic should be turned into a customizable strategy.
        for (CExamplePath<Partition> counterexampleTrace : counterexampleTraces) {
            // logger.fine("Considering counterexample: "
            // + counterexampleTrace.toString());

            // The invariant that we will attempt to satisfy globally with a
            // single split.
            ITemporalInvariant inv = counterexampleTrace.invariant;

            // Skip to next counter-example if we have previously recorded a
            // split that satisfies the invariant corresponding to this
            // counter-example (and which therefore satisfies this
            // counter-example, too).
            if (newlySatisfiedInvariants.contains(inv)) {
                continue;
            }

            // Get the possible splits that might resolve this counter-example.
            List<PartitionSplit> candidateSplits = getSplits(
                    counterexampleTrace, pGraph);

            // Permute the list of candidates.
            Collections.shuffle(candidateSplits, syn.random);

            // Save an arbitrary split to return to caller, if we haven't saved
            // one already.
            if (arbitrarySplit == null && !candidateSplits.isEmpty()) {
                arbitrarySplit = candidateSplits.get(0);
            }

            // logger.fine("candidateSplits are: " +
            // candidateSplits.toString());

            // Find a single split in candidateSplits that makes the
            // invariant corresponding to the counter-example true in the
            // entire graph.
            //
            // a. If no such split exists, then continue to the next
            // counter-example.
            //
            // b. If such a split exists, integrate it into whatever splits we
            // might have found earlier (for previous counter-examples).
            //
            for (PartitionSplit candidateSplit : candidateSplits) {
                if (syn.options.performExtraChecks) {
                    // getSplits() should never generate invalid splits.
                    if (!candidateSplit.isValid()) {
                        throw new InternalSynopticException(
                                "getSplits() generated an invalid split.");
                    }
                }

                PartitionMultiSplit splitOp = new PartitionMultiSplit(
                        candidateSplit);
                Partition partitionBeingSplit = candidateSplit.getPartition();

                // TODO: we check satisfiability of each split _independently_.
                // This means that we are looking for very rare splits that
                // satisfy _different_ invariants individually. A more realistic
                // search would (1) apply each split that satisfies an
                // invariant, and (2) continue searching for more such splits on
                // the _mutated_ pGraph.

                if (splitSatisfiesInvariantGlobally(inv, pGraph, splitOp)) {
                    // If we already have a split for that partition,
                    // incorporate the new split into it.
                    if (splitsToDoByPartition.containsKey(partitionBeingSplit)) {
                        splitsToDoByPartition.get(partitionBeingSplit)
                                .incorporate(splitOp);
                        logger.info("Incorporating new split by partition: "
                                + splitOp.toString());
                    } else {
                        // Otherwise, record this split as the only one for this
                        // partition
                        splitsToDoByPartition.put(partitionBeingSplit, splitOp);
                        // logger.info("New split by partition: " +
                        // splitOp.toString());
                    }

                    // Remember that we can resolve this invariant
                    // violation.
                    newlySatisfiedInvariants.add(inv);
                    // Found the split that completely satisfies the
                    // invariant, no need to consider other splits.
                    break;
                }
            }
        }
        return arbitrarySplit;
    }

    /**
     * This is basically the k-Tails algorithm except that it respects
     * invariants -- if any are violated during a merge, the particular merge is
     * aborted.
     * 
     * @param pGraph
     *            the graph to coarsen
     * @param invariants
     *            the invariants to maintain during merge, can be null
     * @param k
     *            the k parameter for k-equality
     */
    private static void mergePartitions(PartitionGraph pGraph,
            TemporalInvariantSet invariants, int k) {
        int outerItters = 0;

        // The blacklist keeps a history of partitions we've attempted to merge
        // and which did not work out because they resulted in invariant
        // violations.
        Map<Partition, Set<Partition>> mergeBlacklist = new LinkedHashMap<Partition, Set<Partition>>();

        SynopticMain syn = SynopticMain.getInstanceWithExistenceCheck();
        while (true) {
            if (syn.options.dumpIntermediateStages) {
                syn.exportNonInitialGraph(
                        syn.getIntermediateDumpFilename("c", outerItters),
                        pGraph);
            }
            outerItters++;

            logger.fine("--------------------------------");
            if (!mergePartitions(pGraph, mergeBlacklist, invariants, k)) {
                break;
            }
        }

        if (syn.options.dumpIntermediateStages) {
            syn.exportNonInitialGraph(
                    syn.getIntermediateDumpFilename("c", outerItters), pGraph);
        }
    }

    /**
     * Attempts to merge partitions that are k-equivalent, while respecting
     * invariants. Tries all pairs of partitions from pGraph, except for those
     * that are in the mergeBlacklist (these have been attempted previously and
     * are known to violate invariants). Returns true if at least one merge was
     * performed, otherwise returns false.
     * 
     * @param pGraph
     * @param mergeBlacklist
     * @param invariants
     * @param k
     * @return
     */
    private static boolean mergePartitions(PartitionGraph pGraph,
            Map<Partition, Set<Partition>> mergeBlacklist,
            TemporalInvariantSet invariants, int k) {
        ArrayList<Partition> partitions = new ArrayList<Partition>();
        partitions.addAll(pGraph.getNodes());

        // Attempt to merge all pairs of partitions in the current graph.
        for (Partition p : partitions) {
            for (Partition q : partitions) {
                // 1. Can't merge a partition with itself
                if (p == q) {
                    continue;
                }

                logger.fine("Attempting to merge: " + p + "(hash: "
                        + p.hashCode() + ") + " + q + "(hash: " + q.hashCode()
                        + ")");

                // 2. Only merge partitions that are k-equivalent
                if (!KTails.kEquals(p, q, k)) {
                    logger.fine("Partitions are not k-equivalent(k=" + k + ")");
                    continue;
                }

                // 3. Ignore partition pairs that were previously tried (are
                // in blacklist)
                if ((mergeBlacklist.containsKey(p) && mergeBlacklist.get(p)
                        .contains(q))
                        || (mergeBlacklist.containsKey(q) && mergeBlacklist
                                .get(q).contains(p))) {
                    logger.fine("Partitions are in the merge blacklist.");
                    continue;
                }

                Set<Partition> parts = new LinkedHashSet<Partition>();
                parts.addAll(pGraph.getNodes());
                IOperation rewindOperation = pGraph.apply(new PartitionMerge(p,
                        q));

                CExamplePath<Partition> cExample = null;

                if (invariants != null) {
                    cExample = invariants.getFirstCounterExample(pGraph);
                }

                if (cExample != null) {
                    // The merge created a violation. Remember this pair of
                    // partitions so that we don't try it again.
                    logger.fine("Merge violates invariant: "
                            + cExample.toString());

                    if (!mergeBlacklist.containsKey(p)) {
                        mergeBlacklist.put(p, new LinkedHashSet<Partition>());
                    }
                    mergeBlacklist.get(p).add(q);

                    // Undo the merge.
                    pGraph.apply(rewindOperation);

                    if (SynopticMain.getInstanceWithExistenceCheck().options.performExtraChecks) {
                        pGraph.checkSanity();
                    }

                    // We cannot change the partition sets because we are
                    // iterating over the partitions. Therefore, check that
                    // the resulting partition set is the same as the
                    // original partition set.
                    if (!(parts.containsAll(pGraph.getNodes()) && pGraph
                            .getNodes().containsAll(parts))) {
                        throw new InternalSynopticException(
                                "partition set changed due to rewind: "
                                        + rewindOperation);
                    }

                } else {
                    logger.fine("Merge of partitions " + p.getEType()
                            + " nodes maintains invs, accepted.");
                    return true;
                }
            }
        }

        // Unable to find any k-equivalent partitions; we're done.
        return false;
    }
}