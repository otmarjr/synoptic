package synoptic.invariants;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;

import gov.nasa.ltl.graph.Graph;
import gov.nasa.ltl.trans.LTL2Buchi;
import gov.nasa.ltl.trans.ParseErrorException;

import synoptic.invariants.ltlchecker.LTLFormula;
import synoptic.model.EventType;
import synoptic.util.InternalSynopticException;

/**
 * Class for code that is shared by all binary temporal synoptic.invariants.
 * 
 * @author Sigurd Schneider
 */
public abstract class BinaryInvariant implements ITemporalInvariant {
    public static Logger logger = Logger.getLogger("BinaryInvariant");

    protected EventType first;
    protected EventType second;

    /**
     * Role identifiers for each predicate. If the per-host model is used then
     * role identifiers are simply host identifiers corresponding to the input
     * vector clock indices.
     */
    protected int firstRoleId = 0;
    protected int secondRoleId = 0;

    protected String relation;
    // CACHE:
    private Graph automaton;

    public BinaryInvariant(EventType typeFirst, EventType typeSecond,
            String relation) {
        first = typeFirst;
        second = typeSecond;
        this.relation = relation;
    }

    @Override
    public String toString() {
        return getLTLString();
    }

    @Override
    public String getRelation() {
        return relation;
    }

    // /**
    // * Removes loops from a trace path in 2n time.
    // *
    // * <pre>
    // * TODO: the current code cannot be used for counter-example traces
    // because
    // * not all loops can be removed without loosing the counter-example
    // semantics.
    // * For example, paths that include either the 'first' or 'second' event
    // type of
    // * the BinaryInvariant should not be removed. I'm not sure if this is
    // sufficient
    // * to guarantee that the counter-example retains correctness. Check and
    // implement.
    // * Note that loop removal is necessary for the NASA model checker
    // counter-example
    // * paths. The fsm-checker (i think) already returns the shortest possible
    // counter-
    // * example.
    // * </pre>
    // *
    // * @param <T>
    // * The type of node in the trace.
    // * @param trace
    // * The trace from which to remove all loops.
    // * @return A new trace that contains no loops.
    // */
    // public static <T extends INode<T>> List<T> removeLoops(List<T> trace) {
    // LinkedList<T> traceWithoutLoops = new LinkedList<T>();
    // LinkedHashMap<T, Integer> visitedAndNextHop = new LinkedHashMap<T,
    // Integer>();
    // logger.fine("Removing loops from trace: " + trace.toString());
    // // First iteration through trace -- keep track of what next node should
    // // be added to the traceWithoutLoops in the visitedAndNextHop map.
    // int i = 0;
    // for (T node : trace) {
    // visitedAndNextHop.put(node, i + 1);
    // i = i + 1;
    // }
    // // Second iteration through trace -- add just the non-looped nodes to
    // // the traceWithoutLoops.
    // i = 0;
    // int addAtVal = 0; // Always add the INITIAL node.
    // // Could be made faster by iterating just through
    // // visitedAndNextHop map starting from the trace[0] node.
    // for (T node : trace) {
    // if (addAtVal == i) {
    // traceWithoutLoops.add(node);
    // addAtVal = visitedAndNextHop.get(node);
    // }
    // i = i + 1;
    // }
    // logger.fine("Trace without loops: " + traceWithoutLoops.toString());
    // return traceWithoutLoops;
    // }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = getClass().hashCode();
        result = prime * result + (first == null ? 0 : first.hashCode());
        result = prime * result + (relation == null ? 0 : relation.hashCode());
        result = prime * result + (second == null ? 0 : second.hashCode());
        return result;
    }

    @Override
    public boolean predicatesSymmetricEquals(ITemporalInvariant other) {
        assert (other instanceof BinaryInvariant);
        if (first.equals(((BinaryInvariant) other).first)) {
            return second.equals(((BinaryInvariant) other).second);
        }
        if (!first.equals(((BinaryInvariant) other).second)) {
            return false;
        }
        return second.equals(((BinaryInvariant) other).first);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BinaryInvariant other = (BinaryInvariant) obj;
        if (first == null) {
            if (other.first != null) {
                return false;
            }
        } else if (!first.equals(other.first)) {
            return false;
        }
        if (relation == null) {
            if (other.relation != null) {
                return false;
            }
        } else if (!relation.equals(other.relation)) {
            return false;
        }
        if (second == null) {
            if (other.second != null) {
                return false;
            }
        } else if (!second.equals(other.second)) {
            return false;
        }
        return true;
    }

    @Override
    public gov.nasa.ltl.graph.Graph getAutomaton() {
        try {
            if (automaton == null) {
                String formula = LTLFormula.prepare(getLTLString());
                logger.fine("Prepared formula: " + formula);
                automaton = LTL2Buchi.translate("! (" + formula + ")");
                logger.fine("Translated formula: " + automaton);
            }
            return automaton;
        } catch (ParseErrorException e) {
            throw InternalSynopticException.Wrap(e);
        }
    }

    @Override
    public Set<EventType> getPredicates() {
        Set<EventType> predicatesSet = new LinkedHashSet<EventType>();
        predicatesSet.add(first);
        predicatesSet.add(second);
        return predicatesSet;
    }

    /**
     * Returns the first invariant predicate.
     * 
     * @return
     */
    public EventType getFirst() {
        return first;
    }

    /**
     * Returns the second invariant predicate.
     * 
     * @return
     */
    public EventType getSecond() {
        return second;
    }

    /**
     * Returns the role identifier corresponding to the first predicates.
     * 
     * @return integer role
     */
    public int getFirstRoleId() {
        return firstRoleId;
    }

    /**
     * Returns the role identifier corresponding to the second predicates.
     * 
     * @return integer role
     */
    public int getSecondRoleId() {
        return secondRoleId;
    }

    public void setFirstRoleId(int roleId) {
        firstRoleId = roleId;
    }

    public void setSecondRoleId(int roleId) {
        secondRoleId = roleId;
    }

}
