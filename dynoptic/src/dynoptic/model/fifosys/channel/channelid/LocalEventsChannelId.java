package dynoptic.model.fifosys.channel.channelid;

import java.util.LinkedHashMap;
import java.util.Map;

import dynoptic.model.alphabet.EventType;

/**
 * Represent a channel that simulates local events in a CFSM. This channel is
 * synthetic -- that is, it is used internally by dynoptic due to limitations of
 * the model checker. This channel is not part of the system being modeled by
 * dynoptic.
 */
public class LocalEventsChannelId extends ChannelId {

    /**
     * This map allows us to look up the local event types based on their string
     * representations in the McScM counter-example output.
     */
    private Map<String, EventType> eventStrToEventType;

    public LocalEventsChannelId(int scmId) {
        super(Integer.MAX_VALUE, Integer.MAX_VALUE, scmId, "ch-locals");
        eventStrToEventType = new LinkedHashMap<String, EventType>();
    }

    // //////////////////////////////////////////////////////////////////

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) {
            return false;
        }

        if (!(other instanceof LocalEventsChannelId)) {
            return false;
        }

        LocalEventsChannelId leCid = (LocalEventsChannelId) other;
        if (!eventStrToEventType.equals(leCid.eventStrToEventType)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        return (31 * result) + eventStrToEventType.hashCode();
    }

    // //////////////////////////////////////////////////////////////////

    public void addLocalEventString(EventType eventType, String eventStr) {
        assert eventType.isLocalEvent();
        assert !eventStrToEventType.containsKey(eventStr);

        eventStrToEventType.put(eventStr, eventType);
    }

    public EventType getEventType(String eventStr) {
        assert eventStrToEventType.containsKey(eventStr);

        return eventStrToEventType.get(eventStr);
    }

}
