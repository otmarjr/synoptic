package dynoptic.model.fifosys.cfsm.fsm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import dynoptic.DynopticTest;
import dynoptic.model.alphabet.EventType;
import dynoptic.model.fifosys.channel.ChannelId;

public class FSMStateTests extends DynopticTest {

    // Non-accepting state
    FSMState init;
    // Accepting state.
    FSMState accept;

    // cid: 1->2
    ChannelId cid;
    // cid!m
    EventType e_pid1;
    // e_2
    EventType e2_pid1;
    // e_3
    EventType e3_pid2;

    @Override
    public void setUp() {
        init = new FSMState(false, true, 1, 0);
        accept = new FSMState(true, false, 1, 1);
        cid = new ChannelId(1, 2, 0);
        e_pid1 = EventType.SendEvent("m", cid);
        e2_pid1 = EventType.LocalEvent("e", 1);
        e3_pid2 = EventType.LocalEvent("e", 2);
    }

    @Test
    public void checkInitAcceptPid() {
        assertTrue(init.isInitial());
        assertFalse(accept.isInitial());

        assertFalse(init.isAccept());
        assertTrue(accept.isAccept());

        assertEquals(init.getPid(), 1);
        assertEquals(accept.getPid(), 1);

        logger.info(init.toString());
        logger.info(accept.toString());
    }

    @Test
    public void oneTransition() {
        init.addTransition(e_pid1, accept);
        assertTrue(init.getTransitioningEvents().size() == 1);
        assertTrue(init.getTransitioningEvents().contains(e_pid1));
        assertTrue(init.getNextStates(e_pid1).size() == 1);
        assertTrue(init.getNextStates(e_pid1).contains(accept));
    }

    @Test
    public void twoTransitions() {
        init.addTransition(e_pid1, accept);
        init.addTransition(e2_pid1, accept);

        assertTrue(init.getTransitioningEvents().size() == 2);
        assertTrue(init.getTransitioningEvents().contains(e_pid1));
        assertTrue(init.getTransitioningEvents().contains(e2_pid1));
        assertTrue(init.getNextStates(e_pid1).size() == 1);
        assertTrue(init.getNextStates(e_pid1).contains(accept));

        assertTrue(init.getNextStates(e2_pid1).size() == 1);
        assertTrue(init.getNextStates(e2_pid1).contains(accept));
    }

    @Test
    public void scmString() {
        // Without transitions:
        logger.info(init.toScmString());

        init.addTransition(e_pid1, accept);
        init.addTransition(e2_pid1, accept);

        // With transitions:
        logger.info(init.toScmString());
    }

    @Test(expected = AssertionError.class)
    public void addIdenticalTransition() {
        init.addTransition(e_pid1, accept);
        init.addTransition(e_pid1, accept);
    }

    @Test(expected = AssertionError.class)
    public void wrongEventPid() {
        init.addTransition(e3_pid2, accept);
    }

}
