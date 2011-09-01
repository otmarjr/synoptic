package synoptic.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import junit.framework.Assert;

import synoptic.invariants.miners.TOInvariantMiner;
import synoptic.main.Main;
import synoptic.main.ParseException;
import synoptic.main.TraceParser;
import synoptic.model.ChainsTraceGraph;
import synoptic.model.DAGsTraceGraph;
import synoptic.model.DistEventType;
import synoptic.model.Event;
import synoptic.model.EventNode;
import synoptic.model.EventType;
import synoptic.model.PartitionGraph;
import synoptic.model.StringEventType;
import synoptic.model.TraceGraph;
import synoptic.model.export.DotExportFormatter;
import synoptic.model.interfaces.IGraph;
import synoptic.model.interfaces.INode;
import synoptic.util.InternalSynopticException;

/**
 * Base class for all Synoptic unit tests. Performs common set-up and tear-down
 * tasks, and defines methods used by multiple tests.
 * 
 * @author ivan
 */
public abstract class SynopticTest {
    /**
     * Can be used to derive the current test name (as of JUnit 4.7) via
     * name.getMethodName().
     **/
    @Rule
    public static TestName testName;

    // Set up the state statically.
    static {
        testName = new TestName();
    }

    /**
     * The logger instance to use across all tests.
     */
    protected static Logger logger = Logger.getLogger("SynopticTest Logger");
    /**
     * Default relation used in invariant mining.
     */
    public static final String defRelation = "t";

    /**
     * Sets up the Synoptic state that is necessary for running unit tests in
     * general.
     * 
     * @throws ParseException
     */
    @Before
    public void setUp() throws ParseException {
        Main.recoverFromParseErrors = false;
        Main.ignoreNonMatchingLines = false;
        Main.debugParse = false;
        Main.logLvlVerbose = true;
        Main.logLvlExtraVerbose = false;
        // Main.logLvlExtraVerbose = true;
        Main.setUpLogging();
        Main.randomSeed = System.currentTimeMillis();
        Main.random = new Random(Main.randomSeed);
        Main.graphExportFormatter = new DotExportFormatter();
    }

    // //////////////////////////////////////////////
    // Common routines to simplify testing.
    // //////////////////////////////////////////////

    /**
     * Converts an array of strings into a list of EventType objects. Does not
     * handle INITIAL or TERMINAL events types.
     */
    public List<EventType> stringsToStringEventTypes(String[] types) {
        ArrayList<EventType> ret = new ArrayList<EventType>(types.length);
        for (int i = 0; i < types.length; i++) {
            ret.add(new StringEventType(types[i]));
        }
        return ret;
    }

    /**
     * Converts an array of strings containing event types and an corresponding
     * array of host ids into a list of DistEventType objects. Does not handle
     * INITIAL or TERMINAL events types.
     */
    public List<EventType> stringsToDistEventTypes(String[] types, String[] pids) {
        Assert.assertTrue(types.length == pids.length);
        ArrayList<EventType> ret = new ArrayList<EventType>(types.length);
        for (int i = 0; i < types.length; i++) {
            ret.add(new DistEventType(types[i], pids[i]));
        }
        return ret;
    }

    /**
     * Constructs the default parser used by tests. Note: the parser may not be
     * re-used for parsing different traces (it is stateful).
     */
    public TraceParser genDefParser() {
        TraceParser parser = new TraceParser();
        try {
            parser.addRegex("^(?<TYPE>)$");
        } catch (ParseException e) {
            throw new InternalSynopticException(e);
        }
        parser.addPartitionsSeparator("^--$");
        return parser;
    }

    /**
     * Creates a single string out of an array of strings, joined together and
     * delimited using a newline.
     */
    public static String concatinateWithNewlines(String[] events) {
        StringBuilder sb = new StringBuilder();
        for (String s : events) {
            sb.append(s);
            sb.append('\n');
        }
        // Delete the trailing \n.
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public static ArrayList<EventNode> parseLogEvents(String[] events,
            TraceParser parser) throws InternalSynopticException,
            ParseException {
        String traceStr = concatinateWithNewlines(events);
        ArrayList<EventNode> parsedEvents = parser.parseTraceString(traceStr,
                testName.getMethodName(), -1);
        return parsedEvents;
    }

    /**
     * Generates an initial graph using the supplied parser.
     * 
     * @param events
     *            log of events
     * @return an initial graph corresponding to the log of events
     * @throws ParseException
     * @throws InternalSynopticException
     */
    // public static ChainsTraceGraph genChainsTraceGraph(String[] events,
    public static TraceGraph<?> genChainsTraceGraph(String[] events,
            TraceParser parser) throws ParseException,
            InternalSynopticException {
        ArrayList<EventNode> parsedEvents = parseLogEvents(events, parser);
        // return parser.generateDirectTORelation(parsedEvents);
        return parser.generateDefaultOrderRelation(parsedEvents);
    }

    /**
     * Generates an initial graph using the supplied parser.
     * 
     * @param events
     *            log of events
     * @return an initial graph corresponding to the log of events
     * @throws ParseException
     * @throws InternalSynopticException
     */
    public static DAGsTraceGraph genDAGsTraceGraph(String[] events,
            TraceParser parser) throws ParseException,
            InternalSynopticException {
        ArrayList<EventNode> parsedEvents = parseLogEvents(events, parser);
        return parser.generateDirectPORelation(parsedEvents);
    }

    /**
     * Generates an initial linear graph based on a sequence of log events. Uses
     * the defParser parser for parsing the log of events.
     * 
     * @param events
     *            log of events, each one in the format: (?<TYPE>)
     * @return an initial graph corresponding to the log of events
     * @throws ParseException
     * @throws InternalSynopticException
     */
    public ChainsTraceGraph genInitialLinearGraph(String[] events)
            throws ParseException, InternalSynopticException {
        return (ChainsTraceGraph) genChainsTraceGraph(events, genDefParser());
    }

    /**
     * Parsers events using the supplied parser, generates the initial
     * _partitioning_ graph and returns it to the caller.
     * 
     * @param miner
     * @throws Exception
     */
    public static PartitionGraph genInitialPartitionGraph(String[] events,
            TraceParser parser, TOInvariantMiner miner) throws Exception {
        ChainsTraceGraph inputGraph = (ChainsTraceGraph) genChainsTraceGraph(
                events, parser);
        return new PartitionGraph(inputGraph, true,
                miner.computeInvariants(inputGraph));
    }

    /**
     * Given an array of strings, create a list of corresponding LogEvent
     * instances.
     * 
     * @param strEvents
     *            A string sequence of events.
     * @return A LogEvent sequence of events.
     */
    public static List<EventNode> getLogEventPath(String[] strEvents) {
        ArrayList<EventNode> ret = new ArrayList<EventNode>();
        EventNode prevEvent = null;
        for (String strEvent : strEvents) {
            Event act = new Event(strEvent);
            EventNode logEvent = new EventNode(act);
            ret.add(logEvent);
            if (prevEvent != null) {
                prevEvent.addTransition(logEvent, defRelation);
            }
            prevEvent = logEvent;
        }
        return ret;
    }

    /**
     * Exports a graph to a png file.
     * 
     * @param g
     *            Graph to export
     */
    protected static <T extends INode<T>> void exportTestGraph(IGraph<T> g,
            int index) {
        exportTestGraph(g, Integer.toString(index));
    }

    /**
     * Exports a graph to a png file.
     * 
     * @param g
     *            Graph to export
     */
    protected static <T extends INode<T>> void exportTestGraph(IGraph<T> g,
            String title) {
        // Only export test graphs we were told to be verbose.
        if (!Main.logLvlVerbose && !Main.logLvlExtraVerbose) {
            return;
        }
        // String eGraph = defExporter.export(g);
        // logger.fine(eGraph);
        String path = "test-output" + File.separator + testName.getMethodName()
                + title + ".dot";
        Main.exportInitialGraph(path, g);
    }

}
