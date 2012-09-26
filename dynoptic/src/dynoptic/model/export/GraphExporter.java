package dynoptic.model.export;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;

import dynoptic.model.fifosys.cfsm.CFSM;
import dynoptic.model.fifosys.cfsm.fsm.FSM;
import dynoptic.model.fifosys.cfsm.fsm.FSMState;

import synoptic.main.SynopticMain;
import synoptic.model.event.DistEventType;
import synoptic.util.InternalSynopticException;

/**
 * Used to export a graph object to a file.
 * 
 * <pre>
 * Currently supports:
 * - GraphViz dot file format
 * - GML file format
 * </pre>
 */
public class GraphExporter {
	static Logger logger = Logger.getLogger("GraphExporter");

    /**
     * A list of common paths to try when searching for the dot executable.
     * Directory paths to the dot executable should be added here.
     */
    static final String[] dotCommands = { "/usr/bin/dot", "/usr/local/bin/dot",
            "C:\\Programme\\Graphviz2.26\\bin\\dot.exe",
            "C:\\Program Files (x86)\\Graphviz2.26.3\\bin\\dot.exe",
            "C:\\Program Files\\Graphviz 2.28\\bin\\dot.exe" };

    /**
     * @return Returns the dot command executable or null on error
     * @throws InternalSynopticException
     *             problem looking up a command line option description
     */
    private static String getDotCommand() {
        for (String dotCommand : dotCommands) {
            File f = new File(dotCommand);
            if (f.exists()) {
                return dotCommand;
            }
        }
        SynopticMain syn = SynopticMain.getInstanceWithExistenceCheck();
        if (syn.options.dotExecutablePath == null) {
            logger.severe("Unable to locate the dot command executable, use cmd line option:\n\t"
                    + syn.options.getOptDesc("dotExecutablePath"));
        }
        return syn.options.dotExecutablePath;
    }
    
    /**
     * Converts a dot file as a png image file using dot. The png file will be
     * created in the same place as the dot file.
     * 
     * @param dotFile
     *            dot file filename
     */
    public static void generatePngFileFromDotFile(String fileName) {
        File dotFile = new File(fileName);

        String dotCommand = getDotCommand();
        if (dotCommand == null) {
            // could not locate a dot executable
            return;
        }

        String imageExt = "png";

        String execCommand = dotCommand + " -O -T" + imageExt + " "
                + dotFile.getAbsolutePath();

        logger.info("Exporting graph to: " + dotFile.toString() + "."
                + imageExt);

        Process dotProcess;
        try {
            dotProcess = Runtime.getRuntime().exec(execCommand);
        } catch (IOException e) {
            logger.severe("Could not run dotCommand '" + execCommand + "': "
                    + e.getMessage());
            return;
        }
        try {
            dotProcess.waitFor();
        } catch (InterruptedException e) {
            logger.severe("Waiting for dot process interrupted '" + execCommand
                    + "': " + e.getMessage());
        }
    }
    
    /**
     * Exports the CFSM to a format determined by GraphExportFormatter,
     * writing the resulting string to a file specified by fileName.
     * Each FSM in CFSM is exported as one graph.
     */
    public static void exportCFSM(String fileName, CFSM cfsm,
    		boolean outputEdgeLabels) throws IOException {
    	File f = new File(fileName);
        logger.info("Exporting graph to: " + fileName);
        final PrintWriter writer;
        try {
            writer = new PrintWriter(f);
        } catch (final IOException e) {
            throw new RuntimeException("Error opening file for graph export: "
                    + e.getMessage(), e);
        }
        // /////////////
        exportCFSM(writer, cfsm, outputEdgeLabels);
        // /////////////
        writer.close();
    }
    
    /**
     * Exports the CFSM to a format determined by Main.graphExportFormatter,
     * writing the resulting string to writer. Each FSM in CFSM is exported
     * as one graph. The export is done canonically --
     * two isomorphic graphs will have equivalent outputs. The generated dot/gml
     * files may then be diff-ed to check if they represent the same graphs.
     * 
     * @param writer
     *            The writer to use for dot output
     * @param cfsmGraph
     *            The CFSM graph to export
     * @param outputEdgeLabels
     *            Whether or not to output edge labels
     * @throws IOException
     *             In case there is a problem using the writer
     */
    public static void exportCFSM(Writer writer, CFSM cfsmGraph,
    		boolean outputEdgeLabels) throws IOException {        
        // Write out each FSM in CFSM as one graph.
        for (FSM fsmGraph : cfsmGraph.getFSMs()) {
        	exportFSMGraph(writer, fsmGraph, outputEdgeLabels);
        }
    }
    
    /**
     * Exports an FSM in the CFSM as one graph.
     */
    public static void exportFSMGraph(Writer writer, FSM fsmGraph,
    		boolean outputEdgeLabels) {
    	GraphExportFormatter formatter = new DotExportFormatter();
    	
    	try {
    		// Begin graph.
    		writer.write(formatter.beginGraphString());
    		
    		// A mapping between nodes in the graph and the their integer
    		// identifiers in the dot output.
    		LinkedHashMap<FSMState, Integer> nodeToInt = 
    			new LinkedHashMap<FSMState, Integer>();
    		
    		// A unique identifier used to represent nodes in the exported file.
    		int nodeCnt = 0;
    		
    		// NOTE: we must create a new collection so that we do not modify
    		// the set maintained by the graph!
    		Set<FSMState> nodes = new LinkedHashSet<FSMState>(fsmGraph.getStates());
    		
	        // /////////////////////
	        // EXPORT NODES:
            for (FSMState node : nodes) {
            	// Output the node record -- its id along with its attributes.
                writer.write(formatter.nodeToString(nodeCnt,
                        node, node.isInitial(), node.isAccept()));
                // Remember the identifier assigned to this node (used for
                // outputting transitions between nodes).
                nodeToInt.put(node, nodeCnt);
                nodeCnt += 1;
            }
            
	        // /////////////////////
	        // EXPORT EDGES:
	        // Export all the edges corresponding to the nodes in the graph.
            for (FSMState node : nodes) {
            	int nodeSrc = nodeToInt.get(node);
            	Set<DistEventType> transitions = node.getTransitioningEvents();
            	
            	for (DistEventType trans : transitions) {
            		Set<FSMState> nextNodes = node.getNextStates(trans);
            		
                    for (FSMState nextNode : nextNodes) {
                    	int nodeDst = nodeToInt.get(nextNode);
                    	
                    	String s = formatter.edgeToStringWithDistEvent(nodeSrc, nodeDst, trans, null);
                    	writer.write(s);
                    }
            	}
            }
            // End graph.
            writer.write(formatter.endGraphString());
    	} catch (IOException e) {
            throw new RuntimeException(
                    "Error writing to file during graph export: "
                            + e.getMessage(), e);
        }
    }
}