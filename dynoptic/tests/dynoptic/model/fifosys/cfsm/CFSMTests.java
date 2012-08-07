package dynoptic.model.fifosys.cfsm;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import dynoptic.invariants.AlwaysFollowedBy;
import dynoptic.invariants.AlwaysPrecedes;
import dynoptic.invariants.NeverFollowedBy;

public class CFSMTests extends CFSMTesting {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @SuppressWarnings("unused")
    @Test
    public void createEmptyCFSM() {
        CFSM cNew = new CFSM(2, channels);
    }

    @Test
    public void scmString() {
        logger.info(cfsm.toScmString());
    }

    @Test
    public void initsAccepts() {
        assertEquals(cfsm.getInitStates().size(), 1);
        assertEquals(cfsm.getInitStates().iterator().next().getFSMState(0),
                p0Init);
        assertEquals(cfsm.getInitStates().iterator().next().getFSMState(1),
                p1Init);

        assertEquals(cfsm.getAcceptStates().size(), 1);
        assertEquals(cfsm.getAcceptStates().iterator().next().getFSMState(0),
                p0Accept);
        assertEquals(cfsm.getAcceptStates().iterator().next().getFSMState(1),
                p1Accept);

    }

    @Test
    public void augmentWithAFby() {
        AlwaysFollowedBy inv = new AlwaysFollowedBy(p0Sm, p1Rm);
        logger.info(inv.toString());

        cfsm.augmentWithInvTracing(inv);

        List<BadState> badStates = cfsm.getBadStates();
        assertEquals(badStates.size(), 1);
        logger.info(badStates.get(0).toScmString());

        logger.info(cfsm.toScmString());
    }

    @Test
    public void augmentWithNFby() {
        NeverFollowedBy inv = new NeverFollowedBy(p0Sm, p1Rm);
        logger.info(inv.toString());

        cfsm.augmentWithInvTracing(inv);

        List<BadState> badStates = cfsm.getBadStates();
        assertEquals(badStates.size(), 1);
        logger.info(badStates.get(0).toScmString());

        logger.info(cfsm.toScmString());
    }

    @Test
    public void augmentWithAP() {
        AlwaysPrecedes inv = new AlwaysPrecedes(p0Sm, p1Rm);
        logger.info(inv.toString());

        cfsm.augmentWithInvTracing(inv);

        List<BadState> badStates = cfsm.getBadStates();
        assertEquals(1, badStates.size());
        logger.info(badStates.get(0).toScmString());

        logger.info(cfsm.toScmString());
    }
}
