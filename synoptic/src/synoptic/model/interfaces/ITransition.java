package synoptic.model.interfaces;

import synoptic.util.time.ITime;
import synoptic.util.time.ITimeSeries;

/**
 * A generic interface for a transition. This interface provides method to
 * modify the transition.
 * 
 * @author sigurd
 * @param <NodeType>
 *            the type of the nodes which are connected by this transition
 */
public interface ITransition<NodeType> extends
        Comparable<ITransition<NodeType>> {
    /**
     * Get the target node.
     * 
     * @return the target node
     */
    NodeType getTarget();

    /**
     * Get the source node.
     * 
     * @return the source node
     */
    NodeType getSource();

    /**
     * get the label of the transition (i.e. the relation)
     * 
     * @return the name of the relation
     */
    String getRelation();

    /**
     * Set the target node.
     * 
     * @param target
     *            the new target node
     */
    void setTarget(NodeType target);

    /**
     * Set the source node.
     * 
     * @param source
     *            the new source node
     */
    void setSource(NodeType source);

    /**
     * Get the delta time of the transition
     * 
     * @return delta time
     */
    ITime getDelta();

    /**
     * Set the delta.
     * 
     * @param d
     *            the new delta time
     */
    void setDelta(ITime d);

    /**
     * Get all the delta times for transition. This erases the individual delta
     * value and insert it into a series.
     * 
     * @return all delta times
     */
    ITimeSeries<ITime> getDeltaSeries();

    /**
     * Adds a delta to the series. If the series of deltas is missing, then one
     * will be created. If a delta has already been set by setDelta(), it will
     * be added to the series along with the new delta "d".
     * 
     * @param d
     *            The delta to be added to the series.
     */
    void addDelta(ITime d);

    /**
     * Get a short description of the transition
     * 
     * @return a short description
     */
    String toStringConcise();
}
