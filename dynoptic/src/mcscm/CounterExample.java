package mcscm;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dynoptic.model.alphabet.EventType;
import dynoptic.model.fifosys.channel.channelid.ChannelId;
import dynoptic.model.fifosys.channel.channelid.InvChannelId;
import dynoptic.model.fifosys.channel.channelid.LocalEventsChannelId;

/**
 * Represents a counter-example generated by McScM. This is a list of events,
 * which when executed by the corresponding CFSM bring the system into a bad
 * state/configuration.
 */
public class CounterExample {

    static Pattern eventTypeRecvPat, eventTypeSendPat;

    static {
        eventTypeRecvPat = Pattern.compile("^(\\d+) \\? (.+)");
        eventTypeSendPat = Pattern.compile("^(\\d+) ! (.+)");
    }

    /**
     * Parses a string that represents an event in a counter-example produced by
     * McScM and uses the ordered list of channel Ids to create the
     * corresponding EventType instance.
     * 
     * <pre>
     * TODO: assert that the returned EventType is in the alphabet for the CFSM that we are checking.
     * </pre>
     */
    public static EventType parseScmEventStr(String event, List<ChannelId> cids) {
        int cIndex;
        ChannelId chId;

        // Try to parse event as a receive event.
        Matcher m = eventTypeRecvPat.matcher(event);
        if (m.find()) {
            assert m.groupCount() == 2;
            cIndex = Integer.parseInt(m.group(1));
            assert cIndex >= 0 && cIndex < cids.size();

            chId = cids.get(cIndex);
            return EventType.RecvEvent(m.group(2), chId);
        }

        // Try to parse event as a send event.
        m = eventTypeSendPat.matcher(event);
        if (!m.find()) {
            throw new VerifyOutputParseException(
                    "Could not parse event in an McScm counter-example: "
                            + event);
        }
        assert m.groupCount() == 2;
        cIndex = Integer.parseInt(m.group(1));
        assert cIndex >= 0 && cIndex < cids.size();

        chId = cids.get(cIndex);

        if (chId instanceof InvChannelId) {
            // The event is a synthetic event added for tracking
            // invariant-relevant
            // event types.
            return null;
        } else if (chId instanceof LocalEventsChannelId) {
            // The event is a local event, though we simulate it as a send with
            // McScM.
            return ((LocalEventsChannelId) chId).getEventType(m.group(2));
        }

        return EventType.SendEvent(m.group(2), chId);
    }

    // //////////////////////////////////////////////////////////////////

    final List<ChannelId> cids;

    // The sequence of events that makes up the counter-example.
    List<EventType> events;

    public CounterExample(List<ChannelId> cids) {
        this.cids = cids;
        events = new ArrayList<EventType>();
    }

    // //////////////////////////////////////////////////////////////////

    public void addEventStr(String event) {
        EventType e = parseScmEventStr(event, cids);
        if (e != null) {
            events.add(e);
        }
    }

    public List<EventType> getEvents() {
        return events;
    }

    @Override
    public String toString() {
        String ret = "[\n";
        for (EventType e : events) {
            ret += "  " + e.toString() + "\n";
        }
        return ret + "]";
    }
}
