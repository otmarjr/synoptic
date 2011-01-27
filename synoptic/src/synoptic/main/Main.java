package synoptic.main;

import java.lang.Integer;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.io.FilenameUtils;

import org.junit.runner.JUnitCore;

import plume.Option;
import plume.Options;
import plume.OptionGroup;
import synoptic.algorithms.bisim.Bisimulation;
import synoptic.model.Graph;
import synoptic.model.MessageEvent;
import synoptic.model.PartitionGraph;
import synoptic.model.export.GraphVizExporter;
import synoptic.model.input.GraphBuilder;
import synoptic.util.BriefLogFormatter;
import synoptic.util.InternalSynopticException;

public class Main implements Callable<Integer> {
	public static Logger logger = null;

	/**
	 * The current Synoptic version.
	 */
	public static final String versionString = "Synoptic version 0.0.3";
	
	/**
	 * Global source of pseudo-random numbers.
	 */
	public static Random random;

	////////////////////////////////////////////////////
	/**
	 * Print the short usage message.  This does not include verbosity
	 * or debugging options.
	 */
	@OptionGroup("General Options")
	@Option(value="-h Print short usage message", aliases={"-help"})
	public static boolean help = false;
	
	/**
	 * Print the extended usage message.  This includes verbosity and
	 * debugging options but not internal options.
	 */
	@Option("-H Print extended usage message (includes debugging options)")
	public static boolean allHelp = false;
	
	/**
	 * Print the current Synoptic version.
	 */
	@Option (value="-V Print program version", aliases={"-version"})
	public static boolean version = false;
	// end option group "General Options"
	
	////////////////////////////////////////////////////
	/**
	 * Be quiet, do not print much information. Sets the log level to WARNING.
	 */
	@OptionGroup("Execution Options")
	@Option (value="-q Be quiet, do not print much information", aliases={"-quiet"})
	public static boolean logLvlQuiet = false;
	
	/**
	 * Be verbose, print extra detailed information. Sets the log level to FINEST.  
	 */
	@Option (value="-v Print extra detailed information", aliases={"-verbose"})
	public static boolean logLvlVerbose = false;
	
	/**
	 * Use the new FSM checker instead of the LTL checker. 
	 */
	@Option (value="-f Use FSM checker instead of the default LTL checker", aliases={"-use-fsm-checker"})
	public static boolean useFSMChecker = false;
	
	/**
	 * Sets the random seed for Synoptic's source of pseudo-random numbers.
	 */
	@Option (value="Use a specific random seed for pseudo-random number generator")
	public static Long randomSeed = null;
	
	
	/**
	 * Use vector time indexes to  
	 */
	//@Option (value="Use FSM checker instead of the default LTL checker", aliases={"-use-fsm-checker"})
	//public static boolean separateVTimeIndexSets = false;
	// end option group "Execution Options"
	
	
	////////////////////////////////////////////////////
	/**
	 * Regular expression separator string.  When lines are found which match this
	 * expression, the lines before and after are considered to be in different
	 * 'traces', each to be considered an individual sample of the behavior of the
	 * system.
	 * 
	 * This is implemented by augmenting the separator expression with an incrementor,
	 * (?<SEPCOUNT++>), and adding \k<SEPCOUNT> to the partitioner.
	 */
	@OptionGroup("Parser Options")
	@Option (value="-s Partitions separator reg-exp: log lines below and above the matching line are placed into different partitions", aliases={"-partition-separator"})
	public static String separator = null;

	/**
	 * Regular expressions used for parsing the trace file.  This parameter may, and
	 * is often repeated, in order to express the different formats of log lines which
	 * should be parsed.  The ordering is significant, and matching is attempted in
	 * the order in which the expressions are given.
	 * 
	 * These 'regular' expressions are a bit specialized, in that they have named
	 * group matches of the form (?<name>regex), in order to extract the significant
	 * components of the log line.  There are a few more variants on this, detailed in
	 * the online documentation.
	 */
	@Option (value="-r Parser reg-exp: extracts event type and event time from a log line", aliases={"-regexp"})
	public static List<String> regExps = null;

	/**
	 * A substitution expression, used to express how to map the trace lines into
	 * partition traces, to be considered as an individual sample of the behavior of the system.
	 */
	@Option (value="-m Partitions mapping reg-exp: maps a log line to a partition", aliases={"-partition-mapping"})
	public static String partitionRegExp = "\\k<FILE>";
	
	/**
	 * This option relieves the user from writing regular expressions to parse lines
	 * that they are not interested in. This also help to avoid parsing of lines
	 * that are corrupted.  
	 */
	@Option (value="-i Ignore lines that do not match any of the passed regular expressions")
	public static boolean ignoreNonMatchingLines = false;
	
	/**
	 * This allows users to get away with sloppy\incorrect regular expressions
	 * that might not fully cover the range of log lines appearing in the log files.
	 */
	@Option (value="Ignore parser warnings and attempt to recover from parse errors if possible", aliases={"-ignore-parse-errors"})
	public static boolean recoverFromParseErrors = false;
	
	/**
	 * Output the fields extracted from each log line and terminate.
	 */
	@Option (value="Debug the parser by printing field values extracted from the log and then terminate.", aliases={"-debugParse"})
	public static boolean debugParse = false;
	// end option group "Parser Options"
	

	////////////////////////////////////////////////////
	/**
	 * Command line arguments input filename to use.
	 */
	@OptionGroup ("Input Options")
	@Option(value="-c Command line arguments input filename", aliases={"-argsfile"})
	public static String argsFilename= null;
	// end option group "Input Options"

	
	////////////////////////////////////////////////////
	/**
	 * Specifies the prefix of where to store the final Synoptic representation output. This
	 * prefix is also used to determine filenames of intermediary files as well, like
	 * corresponding dot file and intermediate stage representations and dot files
	 * (if specified, e.g. with --dumpIntermediateStages).
	 */
	@OptionGroup("Output Options")
	@Option(value="-o Output path prefix for generating Graphviz dot files graphics", aliases={"-output-prefix"})
	public static String outputPathPrefix = null;
	
	/**
	 * The absolute path to the dot command executable to use for outputting
	 * graphical representations of Synoptic models
	 */
	@Option(value="-d Path to the Graphviz dot command executable to use", aliases={"-dot-executable"})
	public static String dotExecutablePath = null;
	
	/**
	 * This sets the output edge labels on graphs that are exported.
	 */
	@Option(value="Output edge labels on graphs to indicate transition probabilities", aliases={"-outputEdgeLabels"})
	public static boolean outputEdgeLabels = true;
	// end option group "Output Options"
	
	////////////////////////////////////////////////////
	/**
	 * Dump the complete list of mined synoptic.invariants for the set of input files
	 * to stdout.
	 * 
	 * This option is <i>unpublicized</i>; it will not appear in the default usage message
	 */
	@OptionGroup (value="Verbosity Options", unpublicized=true)
	@Option("Dump complete list of mined invariant to stdout")
	public static boolean dumpInvariants = false;
	
	/**
	 * Dump the dot representation of the initial graph to file. The file
	 * will have the name <outputPathPrefix>.initial.dot, where 'outputPathPrefix'
	 * is the filename of the final Synoptic output.
	 * 
	 * This option is <i>unpublicized</i>; it will not appear in the default usage message
	 */
	@Option("Dump the initial graph to file <outputPathPrefix>.initial.dot")
	public static boolean dumpInitialGraph = true;
	
	/**
	 * Dump the dot representations for intermediate Synoptic steps to
	 * file. Each of these files will have a name like:
	 * outputPathPrefix.stage-S.round-R.dot where 'outputPathPrefix' is the
	 * filename of the final Synoptic output, 'S' is the name of the stage
	 * (e.g. r for refinement, and c for coarsening), and 'R' is the round number
	 * within the stage.
	 * 
	 * This option is <i>unpublicized</i>; it will not appear in the default usage message
	 */
	@Option("Dump dot files from intermediate Synoptic stages to files of form outputPathPrefix.stage-S.round-R.dot")
	public static boolean dumpIntermediateStages = false;
	// end option group "Verbosity Options"


	////////////////////////////////////////////////////
	/**
	 * Do not perform the coarsening stage in Synoptic, and as final
	 * output use the most refined representation.
	 * 
	 * This option is <i>unpublicized</i>; it will not appear in the default usage message
	 */
	@OptionGroup (value="Debugging Options", unpublicized=true)
	@Option("Do not perform the coarsening stage")
	public static boolean noCoarsening = false;
	
	/**
	 * Perform benchmarking and output benchmark information
	 * 
	 * This option is <i>unpublicized</i>; it will not appear in the default usage message
	 */
	@Option("Perform benchmarking and output benchmark information")
	public static boolean doBenchmarking = false;
	
	/**
	 * Run all tests in synoptic.tests.units -- all the unit tests, and then terminate. 
	 * 
	 * This option is <i>unpublicized</i>; it will not appear in the default usage message
	 */
	@Option("Run all tests in synoptic.tests.units, and then terminate.")
	public static boolean runTests = false;
	
	/**
	 * Run all tests in synoptic.tests -- unit and integration tests, and then terminate. 
	 * 
	 * This option is <i>unpublicized</i>; it will not appear in the default usage message
	 */
	@Option("Run all tests in synoptic.tests, and then terminate.")
	public static boolean runAllTests = false;
		
	/**
	 * Do not perform the refinement (and therefore do not perform
	 * coarsening) and do not produce any representation as
	 * output. This is useful for just printing the list of mined
	 * synoptic.invariants (using the option 'dumpInvariants' above).
	 * 
	 * This option is <i>unpublicized</i>; it will not appear in the default usage message
	 */
	@Option("Do not perform refinement")
	public static boolean noRefinement = false;
	
	/**
	 * Export graphs in a canonical format. Graphs that are identical
	 * will generated the same dot files. The generated Graphviz dot files
	 * may then be diff-ed to check if they represent the same graphs.
	 * 
	 * This option is <i>unpublicized</i>; it will not appear in the default usage message
	 */
	@Option("Export graphs in a canonical Graphviz format (can use diff to tell if two dot files are the same)")
	public static boolean exportCanonically = true;
	// end option group "Debugging Options"

	
	/**
	 * Input log files to run Synoptic on. These should appear without any
	 * options as the final elements in the command line.
	 */
	public static List<String> logFilenames = null;

	
	/** One line synopsis of usage */
	private static String usage_string
		= "synoptic [options] <logfiles-to-analyze>";

   /**
	* The synoptic.main method to perform the inference algorithm.  See user
	* documentation for an explanation of the options.
	*
	* @param args - command-line options
	*/		
	public static void main(String[] args) throws Exception {
		// this directly sets the static member options of the Main class
		Options options = new Options (usage_string, Main.class);
		String[] cmdLineArgs = options.parse_or_usage(args);
		
		if (argsFilename != null) {
			// read program arguments from a file
			InputStream argsStream = new FileInputStream(argsFilename);
			ListedProperties props = new ListedProperties();
			props.load(argsStream);
			String[] cmdLineFileArgs = props.getCmdArgsLine();
			// the file-based args become the default args
			options.parse_or_usage(cmdLineFileArgs);
		}
		
		// Parse the command line args to override any of the above config file args
		options.parse_or_usage(args);

		// The remainder of the command line is treated as a list of log
		// filenames to process 
		logFilenames = Arrays.asList(cmdLineArgs);
		
		SetUpLogging();

		// Display help for all option groups, including unpublicized ones
		if (allHelp) {
			System.out.println("Usage: " + usage_string);
			System.out.println(
				options.usage("General Options",
				              "Execution Options",
				              "Parser Options",
				              "Input Options",
				              "Output Options",
				              "Verbosity Options",
				              "Debugging Options"));
			return;
		}

		// Display help just for the 'publicized' option groups
		if (help) {
			options.print_usage();
			return;
		}

		if (version) {
			System.out.println(Main.versionString);
			return;
		}
		
		if (runAllTests) {
			runTestsInPackage("synoptic.tests.units.");
			runTestsInPackage("synoptic.tests.integration.");
		} else if (runTests) {
			runTestsInPackage("synoptic.tests.units.");
		}
		
		if (logFilenames.size() == 0) {
			logger.severe("No log filenames specified, exiting. Try cmd line option:\n\t" +
					Main.getCmdLineOptDesc("help"));
			return;
		}
		
		Main mainInstance = new Main();
		
		if (logLvlVerbose) {
			mainInstance.printOptions();
		}
		
		if (randomSeed == null) {
			Main.randomSeed = System.currentTimeMillis();
		}
		Main.random = new Random(randomSeed);
		logger.info("Using random seed: " + randomSeed);
		
		Integer ret = mainInstance.call();
		logger.fine("Main.call() returned " + ret.toString());
		System.exit(ret); 
	}
	
	
	/**
	 * Returns a command line option description for an option name
	 * 
	 * @param optName The option variable name
	 * @return a string description of the option
	 * @throws InternalSynopticException if optName cannot be accessed
	 */
	public static String getCmdLineOptDesc(String optName) throws InternalSynopticException {
		Field field;
		try {
			field = Main.class.getField(optName);
		} catch (SecurityException e) {
			throw new InternalSynopticException(e);
		} catch (NoSuchFieldException e) {
			throw new InternalSynopticException(e);
		}
		Option opt = field.getAnnotation(Option.class);
		String desc = opt.value();
		if (desc.length() > 0 && desc.charAt(0) != '-') {
			// For options that do not have a short option form,
			// include the long option trigger in the description. 
			desc = "--" + optName + " " + desc; 
		}
		return desc; 
	}
	
	
	/**
	 * Runs all the synoptic unit tests
	 * 
	 * @throws URISyntaxException if Main.class can't be located
	 */
	public static void runTestsInPackage(String packageName) throws URISyntaxException {
		// If we are running from within a jar then jarName contains the path to the jar
		// otherwise, it contains the path to where Main.class is located on the filesystem 
		String jarName = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
		System.out.println("Looking for tests in: " + jarName);
					
		// We assume that the tests we want to run are classes within
		// packageName, which can be found with the corresponding packagePath filesystem offset
		String packagePath = packageName.replaceAll("\\.", File.separator);
		
		ArrayList<String> testClasses = new ArrayList<String>();
		
		try{
			// Case1: running from within a jar
			// Open the jar file and locate the tests by their path
			JarInputStream jarFile = new JarInputStream(new FileInputStream(jarName));
			JarEntry jarEntry;
			while (true) {
				jarEntry = jarFile.getNextJarEntry();
				if (jarEntry == null){
					break;
				}
				String className = jarEntry.getName();
				if ((className.startsWith(packagePath)) &&
				    (className.endsWith(".class")) ) {
					int endIndex = className.lastIndexOf(".class");
					className = className.substring(0, endIndex);
					testClasses.add(className.replaceAll("/", "\\."));
				}
			}
		}
		catch (java.io.FileNotFoundException e) {
			// Case2: not running from within a jar
			// Find the tests by walking through the directory structure
			File folder = new File(jarName + packagePath);
			File[] listOfFiles = folder.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				String className = listOfFiles[i].getName();
				if (listOfFiles[i].isFile() && className.endsWith(".class")) {
					int endIndex = className.lastIndexOf(".class");
					className = className.substring(0, endIndex);
					testClasses.add(packageName + className);
				}
			}
		}
		catch( Exception e){
			e.printStackTrace ();
			return;
		}
		
		System.out.println("Running tests: " + testClasses);
		String[] testClassesAr = new String[testClasses.size()];
		testClassesAr = testClasses.toArray(testClassesAr);
		JUnitCore.main(testClassesAr);
		return; 
	}
	

	/**
	 * Sets up and configures the Main.logger object based on command line
	 * arguments
	 */
	public static void SetUpLogging() {
		// Get the top Logger instance
		logger = Logger.getLogger("");
		
		// Handler for console (reuse it if it already exists)
		Handler consoleHandler = null;
		
		// See if there is already a console handler
		for (Handler handler : logger.getHandlers()) {
			if (handler instanceof ConsoleHandler) {
				consoleHandler = handler;
				break;
			}
		}
		
		if (consoleHandler == null) {
			// No console handler found, create a new one
			consoleHandler = new ConsoleHandler();
			logger.addHandler(consoleHandler);
		}
		
		// The consoleHandler will write out anything the logger gives it
		consoleHandler.setLevel(Level.ALL);

		// consoleHandler.setFormatter(new CustomFormatter());
		
		// Set the logger's log level based on command line arguments
		if (logLvlQuiet) {
			logger.setLevel(Level.WARNING);
		} else if (logLvlVerbose) {
			logger.setLevel(Level.FINEST);
		} else {
			logger.setLevel(Level.INFO);
		}

		consoleHandler.setFormatter(new BriefLogFormatter());
		
		return;
	}
	
	/**
	 * Given a potentially wild-carded file path, finds all those which match.
	 * 
	 * TODO: make sure that the same file doesn't appear twice in the returned list
	 * 
	 * @param fileArg The file path which may potentially contain wildcards.
	 * @return An array of File handles which match.
	 * @throws Exception 
	 */
	public static File[] getFiles(String fileArg) throws Exception {
		int wildix = fileArg.indexOf("*");
		if (wildix == -1) {
			return new File[]{ new File(fileArg) };
		} else {
			String uptoWild = fileArg.substring(0, wildix);
			String path = FilenameUtils.getFullPath(uptoWild);
			String filter = FilenameUtils.getName(uptoWild) + fileArg.substring(wildix);
			File dir = new File(path).getAbsoluteFile();
			//TODO: check that listFiles is working properly recursively here.
			File[] results = dir.listFiles((FileFilter)new WildcardFileFilter(filter));
			if (results == null) {
				throw new Exception("Wildcard match failed: " + ( dir.isDirectory() ?
						dir.toString() + " not a directory" : " for unknown reason"));
			}
			return results;
		}
	}
	
	/**
	 * Returns the filename for an intermediate dot file based on the given
	 * stage name and round number. Adheres to the convention specified above
	 * in usage, namely that the filename is of the format:
	 * outputPathPrefix.stage-S.round-R.dot
	 * 
	 * @param stageName Stage name string, e.g. "r" for refinement
	 * @param roundNum Round number within the stage
	 * @return
	 */
	public static String getIntermediateDumpFilename(String stageName, int roundNum) {
		return new String(outputPathPrefix + ".stage-" + stageName + ".round-"+ roundNum + ".dot");
	}

	/***********************************************************/
	
	public Main() {
		// TODO: can set up graphical state here
	}
	

	/**
	 * Prints the values of all the options for this -- instance of Main class
	 * 
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public void printOptions() throws IllegalArgumentException, IllegalAccessException {
		String optsString = "Synoptic options:\n";
		for (Field field : this.getClass().getDeclaredFields()) {
			if (field.getAnnotation(Option.class) != null) {
				optsString += "\t" + field.getName() + ": "; 
				if (field.get(this) != null) {
					optsString += field.get(this).toString() + "\n";
				} else {
					optsString += "null\n";
				}
			}
		}
		System.out.println(optsString);
	}

	
	/**
	 *  The workhorse method, which uses ITraceParser to parse the input files, and calls
	 *  the primary Synoptic functions to perform refinement\coarsening and
	 *  finally outputs the final graph to the output file (specified as a
	 *  command line option).
	 */
	@Override
	public Integer call() throws Exception {
		TraceParser parser = new TraceParser();
		
		logger.fine("Setting up the log file parser.");
		
		if (!Main.regExps.isEmpty()) {
			// User provided regular expressions - parse them!
			for (String exp : Main.regExps) {
				logger.fine("\taddRegex with exp:" + exp);
				parser.addRegex(exp);
			}
			
			parser.setPartitioner(Main.partitionRegExp);
		} else {
			// No expressions provided - use the default regex.
			parser.addRegex("^\\s*$(?<SEPCOUNT++>)");
			parser.addRegex("(?<TYPE>.*)");
			parser.setPartitioner(Main.partitionRegExp);
		}

		if (Main.separator != null) {
			parser.addSeparator(Main.separator);
		}

		// Parses all the log filenames, constructing the parsedEvents List.
		List<TraceParser.Occurrence> parsedEvents = new ArrayList<TraceParser.Occurrence>();
		
		logger.info("Parsing input files..");
		
		for (String fileArg : Main.logFilenames) {
			logger.fine("\tprocessing fileArg: " + fileArg);
			File[] files = getFiles(fileArg);
			for (File file : files) {
				logger.fine("\tcalling parseTraceFile with file: " + file.getAbsolutePath());
				try {
					parsedEvents.addAll(parser.parseTraceFile(file, -1));
				} catch (ParseException e) {
					logger.severe("Caught ParseException -- unable to continue, exiting. Try cmd line option:\n\t" +
							Main.getCmdLineOptDesc("help"));
					logger.severe(e.toString());
					return new Integer(1);
				}
			}
		}
		
		if (Main.debugParse) {
			// Terminate since the user is interested in debugging the parser.
			logger.info("Terminating. To continue further, re-run without the debugParse option.");
			return new Integer(0);
		}
		
		// If we parsed any events, then run Synoptic.
		logger.info("Mining invariants..");
		parser.generateDirectTemporalRelation(parsedEvents, true);
		Graph<MessageEvent> inputGraph = ((GraphBuilder) parser.builder).getRawGraph();
		
		if (dumpInitialGraph) {
			// If we were given an output filename then export the resulting graph 
			// into outputPathPrefix.initial.dot
			if (Main.outputPathPrefix != null) {
				logger.info("Exporting initial graph [" + inputGraph.getNodes().size() + " nodes]..");
				GraphVizExporter exporter = new GraphVizExporter();
				exporter.exportAsDotAndPngFast(Main.outputPathPrefix + ".initial.dot", inputGraph);
			} else {
				logger.warning("Cannot output initial graph. Specify output path prefix using:\n\t" +
					Main.getCmdLineOptDesc("outputPathPrefix"));
			}
		}
		
		logger.info("Running Synoptic...");
		
		PartitionGraph result = new PartitionGraph(inputGraph, true);
		/* TemporalInvariantSet synoptic.invariants = result.getInvariants();
		FsmModelChecker<MessageEvent> checker = new FsmModelChecker<MessageEvent>(synoptic.invariants, inputGraph);
		List<RelationPath<MessageEvent>> paths = checker.getCounterexamples();
		if (paths.isEmpty()) {
			System.out.println("synoptic.model checker ok.");
		} */		
		
		if (Main.logLvlVerbose) {
			System.out.println("");
			System.out.println("");
		}
		logger.fine("Refining (Splitting)...");
		Bisimulation.splitPartitions(result);
		
		if (Main.logLvlVerbose) {
			System.out.println("");
			System.out.println("");
		}
		logger.fine("Coarsening (Merging)..");
		Bisimulation.mergePartitions(result);

		// TODO: check that none of the initially mined synoptic.invariants are unsatisfied in the result		
		
		// export the resulting graph
		if (Main.outputPathPrefix != null) {
			logger.info("Exporting final graph [" + result.getNodes().size() + " nodes]..");
			GraphVizExporter exporter = new GraphVizExporter();
			exporter.exportAsDotAndPngFast(Main.outputPathPrefix + ".dot", result);
		} else {
			logger.warning("Cannot output final graph. Specify output path prefix using:\n\t" +
					Main.getCmdLineOptDesc("outputPathPrefix"));
		}
		
		return new Integer(0);
	}
}
