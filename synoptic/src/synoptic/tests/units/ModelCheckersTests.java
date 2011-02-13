package synoptic.tests.units;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import synoptic.invariants.AlwaysFollowedInvariant;
import synoptic.invariants.AlwaysPrecedesInvariant;
import synoptic.invariants.ITemporalInvariant;
import synoptic.invariants.NeverFollowedInvariant;
import synoptic.invariants.RelationPath;
import synoptic.invariants.TemporalInvariantSet;
import synoptic.main.Main;
import synoptic.main.ParseException;
import synoptic.model.Graph;
import synoptic.model.LogEvent;
import synoptic.tests.SynopticTest;
import synoptic.util.InternalSynopticException;

/**
 * Checks the FSM model checker against the NASA model checker to compare their
 * results for generating counter examples of temporal invariants on graphs.
 * This is a parameterized JUnit test -- tests in this class are run with
 * parameters generated by method annotated with @Parameters.
 * 
 * @author ivan
 */
@RunWith(value = Parameterized.class)
public class ModelCheckersTests extends SynopticTest {

    /**
     * Generates parameters for this unit test. The first instance of this test
     * (using first set of parameters) will run using the FSM checker, while the
     * second instance (using the second set of parameters) will run using the
     * NASA model checker.
     * 
     * @return The set of parameters to pass to the constructor the unit test.
     */
    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] { { true }, { false } };
        return Arrays.asList(data);
    }

    public ModelCheckersTests(boolean useFSMChecker) {
        Main.useFSMChecker = useFSMChecker;
    }

    /**
     * Test that the list of events representing a linear graph generates or not
     * (cExampleExists) a counter-example for invariant inv.
     */
    private static void testLinearGraphCExample(String[] events,
            ITemporalInvariant inv, boolean cExampleExists)
            throws InternalSynopticException, ParseException {

        Graph<LogEvent> g = SynopticTest.genInitialLinearGraph(events);
        TemporalInvariantSet invs = new TemporalInvariantSet();
        invs.add(inv);
        List<RelationPath<LogEvent>> cexamples = invs.getAllCounterExamples(g);

        if (!cExampleExists) {
            assertTrue(cexamples == null);
            return;
        }

        // Else, there should be just one counter-example -- a path that is the
        // entire graph g.
        assertTrue(cexamples.size() == 1);
        List<LogEvent> cexamplePath = cexamples.get(0).path;
        Set<LogEvent> initNodes = g.getInitialNodes();
        LogEvent nextNode = initNodes.iterator().next();
        for (int i = 0; i < cexamplePath.size(); i++) {
            assertTrue(cexamplePath.get(i) == nextNode);
            if (i + 1 != cexamplePath.size()) {
                // The c-example and g must be of the same length.
                assertTrue(nextNode.getTransitions().size() == 1);
                nextNode = nextNode.getTransitions().get(0).getTarget();
            }
        }
    }

    /**
     * Tests that a linear graph does not generate an AFby c-example.
     * 
     * @throws InternalSynopticException
     * @throws ParseException
     */
    @Test
    public void NoAFbyLinearGraphTest() throws InternalSynopticException,
            ParseException {
        // logger.info("Using the FSMChecker: " + Main.useFSMChecker);
        String[] events = new String[] { "a", "x", "y", "b" };
        ITemporalInvariant inv = new AlwaysFollowedInvariant("a", "b",
                SynopticTest.defRelation);
        testLinearGraphCExample(events, inv, false);
    }

    /**
     * Tests that a linear graph does generate an AFby c-example.
     * 
     * @throws InternalSynopticException
     * @throws ParseException
     */
    @Test
    public void AFbyLinearGraphTest() throws InternalSynopticException,
            ParseException {
        // logger.info("Using the FSMChecker: " + Main.useFSMChecker);
        String[] events = new String[] { "a", "x", "y", "z" };
        ITemporalInvariant inv = new AlwaysFollowedInvariant("a", "b",
                SynopticTest.defRelation);
        testLinearGraphCExample(events, inv, true);
    }

    /**
     * Tests that a linear graph does not generate an NFby c-example.
     * 
     * @throws InternalSynopticException
     * @throws ParseException
     */
    @Test
    public void NoNFbyLinearGraphTest() throws InternalSynopticException,
            ParseException {
        // logger.info("Using the FSMChecker: " + Main.useFSMChecker);
        String[] events = new String[] { "a", "x", "y", "z" };
        ITemporalInvariant inv = new NeverFollowedInvariant("a", "b",
                SynopticTest.defRelation);
        testLinearGraphCExample(events, inv, false);
    }

    /**
     * Tests that a linear graph does generate an NFby c-example.
     * 
     * @throws InternalSynopticException
     * @throws ParseException
     */
    @Test
    public void NFbyLinearGraphTest() throws InternalSynopticException,
            ParseException {
        // logger.info("Using the FSMChecker: " + Main.useFSMChecker);
        String[] events = new String[] { "a", "x", "y", "z", "b" };
        ITemporalInvariant inv = new NeverFollowedInvariant("a", "b",
                SynopticTest.defRelation);
        testLinearGraphCExample(events, inv, true);
    }

    /**
     * Tests that a linear graph does not generate an AP c-example.
     * 
     * @throws InternalSynopticException
     * @throws ParseException
     */
    @Test
    public void NoAPLinearGraphTest() throws InternalSynopticException,
            ParseException {
        // logger.info("Using the FSMChecker: " + Main.useFSMChecker);
        String[] events = new String[] { "x", "a", "x", "y", "b" };
        ITemporalInvariant inv = new AlwaysPrecedesInvariant("a", "b",
                SynopticTest.defRelation);
        testLinearGraphCExample(events, inv, false);
    }

    /**
     * Tests that a linear graph does generate an AP c-example.
     * 
     * @throws InternalSynopticException
     * @throws ParseException
     */
    @Test
    public void APLinearGraphTest() throws InternalSynopticException,
            ParseException {
        // logger.info("Using the FSMChecker: " + Main.useFSMChecker);
        String[] events = new String[] { "x", "y", "z", "b", "a" };
        ITemporalInvariant inv = new AlwaysPrecedesInvariant("a", "b",
                SynopticTest.defRelation);
        testLinearGraphCExample(events, inv, true);
    }
}
