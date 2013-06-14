package synoptic.invariants.fsmcheck;

import java.util.List;

import synoptic.invariants.BinaryInvariant;
import synoptic.model.event.EventType;
import synoptic.model.interfaces.INode;
import synoptic.model.interfaces.ITransition;

/**
 * Represents a set of "A never followed by B" synoptic.invariants to simulate,
 * recording the shortest historical path to reach a particular state. This
 * finite state machine enters a particular state (s2) when A is provided. If we
 * are in this state when a B is provided, then we enter into a failure state.
 * 
 * @author Michael Sloan (mgsloan@gmail.com)
 * @see NFbyInvFsms
 * @see FsmStateSet
 */
public class NFbyTracingSet<T extends INode<T>> extends TracingStateSet<T> {
    HistoryNode aNotSeen; // A not seen
    HistoryNode aSeen; // A seen (and no Bs yet after it)
    HistoryNode bSeenAfter; // A seen, followed by B -- failure state

    EventType a, b;

    public NFbyTracingSet(EventType a, EventType b) {
        this.a = a;
        this.b = b;
    }

    public NFbyTracingSet(BinaryInvariant inv) {
        this(inv.getFirst(), inv.getSecond());
    }

    @Override
    public <Node extends INode<Node>> void setInitial(T input,
            List<? extends ITransition<Node>> transitions) {
        EventType name = input.getEType();
        HistoryNode newHistory = new HistoryNode(input, null, 1);
        aNotSeen = aSeen = bSeenAfter = null;
        if (a.equals(name)) {
            aSeen = newHistory;
        } else {
            aNotSeen = newHistory;
        }
    }

    @Override
    public <Node extends INode<Node>> void transition(T input,
            List<? extends ITransition<Node>> transitions) {
        EventType name = input.getEType();

        if (b.equals(name)) {
            bSeenAfter = preferShorter(aSeen, bSeenAfter);
            aSeen = null;
        }
        /*
         * NOTE: there is no else here, because for this invariant, isA and isB
         * can be simultaneously true (A NFby A, eg, A is singleton).
         */
        if (a.equals(name)) {
            aSeen = preferShorter(aNotSeen, aSeen);
            aNotSeen = null;
        }

        // Advance history for all states.
        aNotSeen = extend(input, aNotSeen);
        aSeen = extend(input, aSeen);
        bSeenAfter = extend(input, bSeenAfter);
    }

    @Override
    public HistoryNode failpath() {
        return bSeenAfter;
    }

    @Override
    public NFbyTracingSet<T> copy() {
        NFbyTracingSet<T> result = new NFbyTracingSet<T>(a, b);
        result.aNotSeen = aNotSeen;
        result.aSeen = aSeen;
        result.bSeenAfter = bSeenAfter;
        return result;
    }

    @Override
    public void mergeWith(TracingStateSet<T> other) {
        NFbyTracingSet<T> casted = (NFbyTracingSet<T>) other;
        aNotSeen = preferShorter(aNotSeen, casted.aNotSeen);
        aSeen = preferShorter(aSeen, casted.aSeen);
        bSeenAfter = preferShorter(bSeenAfter, casted.bSeenAfter);
    }

    @Override
    public boolean isSubset(TracingStateSet<T> other) {
        NFbyTracingSet<T> casted = (NFbyTracingSet<T>) other;
        if (casted.aNotSeen == null) {
            if (aNotSeen != null) {
                return false;
            }
        }
        if (casted.aSeen == null) {
            if (aSeen != null) {
                return false;
            }
        }
        if (casted.bSeenAfter == null) {
            if (bSeenAfter != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("NFby: ");
        appendWNull(result, bSeenAfter); // Failure case first.
        result.append(" | ");
        appendWNull(result, aSeen);
        result.append(" | ");
        appendWNull(result, aNotSeen);
        return result.toString();
    }
}
