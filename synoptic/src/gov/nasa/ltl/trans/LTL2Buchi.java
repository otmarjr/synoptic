//
// Copyright (C) 2005 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA). All Rights Reserved.
//
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3. The NOSA has been approved by the Open Source
// Initiative. See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
//
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//

// Written by Dimitra and Flavio (2001)
// Some modifications by: Roby Joehanes
package gov.nasa.ltl.trans;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import gov.nasa.ltl.graph.Degeneralize;
import gov.nasa.ltl.graph.Graph;
import gov.nasa.ltl.graph.SCCReduction;
import gov.nasa.ltl.graph.SFSReduction;
import gov.nasa.ltl.graph.Simplify;
import gov.nasa.ltl.graph.SuperSetReduction;

/**
 * DOCUMENT ME!
 */
public class LTL2Buchi {
    private static Logger logger = Logger.getLogger("LTL2Buchi Logger");
    static {
        logger.setLevel(Level.INFO);
    }

    public static void main(String[] args) {
        String ltl = null;
        boolean rewrite = true;
        boolean bisim = true;
        boolean fairSim = true;
        boolean file_provided = false;
        int format = Graph.FSP_FORMAT;

        System.out
                .println("\nAuthors Dimitra Giannakopoulou & Flavio Lerda, \n(c) 2001,2003 NASA Ames Research Center\n");

        Translator.set_algorithm(Translator.LTL2BUCHI);

        if (args.length != 0) {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("usage"))
                    usage_warning();
                if (args[i].equals("-a")) {
                    i++;

                    if (i < args.length) {
                        if (args[i].equals("ltl2buchi")) {
                            Translator.set_algorithm(Translator.LTL2BUCHI);
                        } else if (args[i].equals("ltl2aut")) {
                            Translator.set_algorithm(Translator.LTL2AUT);
                        } else {
                            usage_warning();

                            return;
                        }
                    } else {
                        usage_warning();

                        return;
                    }
                } else if (args[i].equals("-norw")) {
                    rewrite = false;
                } else if (args[i].equals("-nobisim")) {
                    bisim = false;
                } else if (args[i].equals("-nofsim")) {
                    fairSim = false;
                } else if (args[i].equals("-o")) {
                    i++;

                    if (i < args.length) {
                        if (args[i].equals("fsp"))
                            format = Graph.FSP_FORMAT;
                        else if (args[i].equals("promela"))
                            format = Graph.SPIN_FORMAT;
                        else if (args[i].equals("xml"))
                            format = Graph.XML_FORMAT;
                    }

                } else if (args[i].equals("-f")) {
                    i++;

                    if (i < args.length) {
                        ltl = args[i];

                        if (ltl.endsWith(".ltl")) {
                            ltl = loadLTL(ltl);
                            file_provided = true;
                        } else if (ltl.equals("-")) {
                            // ignore "-"
                        } else {
                            usage_warning();

                            return;
                        }
                    } else {
                        usage_warning();

                        return;
                    }
                } else {
                    usage_warning();

                    return;
                }
            }
        }

        if (!file_provided) {
            ltl = readLTL();
        }

        try {
            final Graph g = translate(ltl, rewrite, bisim, fairSim);
            g.save(format);

            logger.fine("\n***********************\n");

        } catch (final ParseErrorException ex) {
            logger.fine("Error: " + ex);
        }
    }

    public static void reset_all_static() {
        Node.reset_static();
        Formula.reset_static();
        Pool.reset_static();
    }

    public static Graph translate(String formulaIn, boolean rewrite,
            boolean bisim, boolean fair_sim) throws ParseErrorException {
        logger.fine("Translating formula: " + formulaIn);
        String formula = "";
        final boolean superset = true;
        final boolean scc = true;

        if (rewrite) {
            try {
                formula = Rewriter.rewrite(formulaIn);
            } catch (final ParseErrorException e) {
                throw new ParseErrorException(e.getMessage());
            }

            logger.fine("Rewritten as       : " + formula);
        }

        if (formula == null) {
            logger.fine("Unexpected null formula");
        }

        Graph gba = Translator.translate(formula);

        // gba.save("gba.sm");
        logger.fine("\n***********************");
        logger.fine("\nGeneralized buchi automaton generated");
        logger.fine("\t" + gba.getNodeCount() + " states " + gba.getEdgeCount()
                + " transitions");

        // gba.save(Graph.FSP_FORMAT);
        // System.out.println("***********************\n\n");

        /*
         * // Omitted - does not seem to always at this stage, for example !(aU
         * (bUc))
         * 
         * if (scc) { gba = SCCReduction.reduce(gba); if (debug) { //
         * gba.save("scc-gba.sm");
         * System.out.println("Strongly connected component reduction");
         * System.out.println("\t" + gba.getNodeCount() + " states " +
         * gba.getEdgeCount() + " transitions"); System.out.println();
         * gba.save(Graph.FSP_FORMAT); } }
         */
        if (superset) {
            gba = SuperSetReduction.reduce(gba);

            // gba.save("ssr-gba.sm");
            logger.fine("\n***********************");
            logger.fine("Superset reduction");
            logger.fine("\t" + gba.getNodeCount() + " states "
                    + gba.getEdgeCount() + " transitions");

            // System.out.println();
            // gba.save(Graph.FSP_FORMAT);
        }

        Graph ba = Degeneralize.degeneralize(gba);

        // ba.save("ba.sm");
        logger.fine("\n***********************");
        logger.fine("Degeneralized buchi automaton generated");
        logger.fine("\t" + ba.getNodeCount() + " states " + ba.getEdgeCount()
                + " transitions");

        // System.out.println();
        // ba.save(Graph.FSP_FORMAT);

        if (scc) {
            ba = SCCReduction.reduce(ba);

            // ba.save("scc-ba.sm");
            logger.fine("\n***********************");
            logger.fine("Strongly connected component reduction");
            logger.fine("\t" + ba.getNodeCount() + " states "
                    + ba.getEdgeCount() + " transitions");

            // System.out.println();
            // ba.save(Graph.FSP_FORMAT);
        }

        if (bisim) {
            ba = Simplify.simplify(ba);

            // ba.save("bisim-final.sm");
            logger.fine("\n***********************");
            logger.fine("Bisimulation applied");
            logger.fine("\t" + ba.getNodeCount() + " states "
                    + ba.getEdgeCount() + " transitions");

            // System.out.println();
            // ba.save(Graph.FSP_FORMAT);
        }

        if (fair_sim) {
            ba = SFSReduction.reduce(ba);

            // ba.save("fairSim-final.sm");
            logger.fine("\n***********************");
            logger.fine("Fair simulation applied");
            logger.fine("\t" + ba.getNodeCount() + " states "
                    + ba.getEdgeCount() + " transitions");

            // System.out.println();
            // ba.save(Graph.FSP_FORMAT);
        }

        // logger.fine("***********************\n");

        reset_all_static();

        return ba;
    }

    public static Graph translate(String formula) throws ParseErrorException {
        // To work with Bandera and JPF
        return translate(formula, true, true, true);
    }

    public static Graph translate(File file) throws ParseErrorException {
        String formula = "";

        try {
            final LineNumberReader f = new LineNumberReader(
                    new FileReader(file));
            formula = f.readLine().trim();
            f.close();
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        return translate(formula, true, true, true);
    }

    /**
     * Commented out on 8/7/03 by Dimitra - apparently redundant now since JPF
     * not tied with Bandera any longer public static Graph translate(String
     * formula) throws ParseErrorException { // To work with Bandera and JPF
     * boolean rewrite = true; boolean superset = true; boolean scc = true;
     * boolean bisim = true; boolean fair_sim = true; if (rewrite) { try {
     * formula = Rewriter.rewrite(formula); } catch (ParseErrorException e) {
     * throw new ParseErrorException(e.getMessage()); }
     * logger.fine("Rewritten as       : " + formula); System.out.println(); }
     * Graph gba = Translator.translate(formula); //#ifdef BANDERA try {
     * gba.save(System.getProperty("user.dir") + File.separator + "gba.sm"); }
     * catch (IOException ex) { } //#else BANDERA //#endif BANDERA if (superset)
     * { gba = SuperSetReduction.reduce(gba); //#ifdef BANDERA try { gba.save(
     * System.getProperty("user.dir") + File.separator + "ssr-gba.sm"); } catch
     * (IOException ex) { } //#else BANDERA //#endif BANDERA } Graph ba =
     * Degeneralize.degeneralize(gba); //#ifdef BANDERA try {
     * ba.save(System.getProperty("user.dir") + File.separator + "ba.sm"); }
     * catch (IOException ex) { } //#else BANDERA //#endif BANDERA if (scc) { ba
     * = SCCReduction.reduce(ba); //#ifdef BANDERA try {
     * ba.save(System.getProperty("user.dir") + File.separator + "scc-ba.sm"); }
     * catch (IOException ex) { } //#else BANDERA //#endif BANDERA } if (bisim)
     * { ba = Simplify.simplify(ba); //#ifdef BANDERA try {
     * ba.save(System.getProperty("user.dir") + File.separator + "bisim.sm"); }
     * catch (IOException ex) { } //#else BANDERA //#endif BANDERA } if
     * (fair_sim) { ba = SFSReduction.reduce(ba); //#ifdef BANDERA try {
     * ba.save( System.getProperty("user.dir") + File.separator +
     * "fairSim-ba.sm"); } catch (IOException ex) { } //#else BANDERA //#endif
     * BANDERA } logger.fine("***********************\n"); reset_all_static();
     * return ba; }
     */
    public static void usage_warning() {
        System.out.println("\n*******  USAGE *******");
        System.out.println("java gov.nasa.ltl.trans.LTL2Buchi <options>");
        System.out.println("\toptions can be (in any order):");
        System.out
                .println("\t\t \"-f <filename.ltl>\" (read formula from file)");
        System.out
                .println("\t\t \"-a [ltl2buchi|ltl2aut]\" (set algorithm to be used)");
        System.out.println("\t\t \"-norw\" (no rewriting)");
        System.out.println("\t\t \"-nobisim\" (no bisimulation reduction)");
        System.out.println("\t\t \"-nofsim\" (no fair simulation reduction)");
        System.out
                .println("\t\t \"-o [fsp|promela|xml>\" (format of output; default is fsp)");

        return;
    }

    private static String loadLTL(String fname) {
        try {
            final BufferedReader in = new BufferedReader(new FileReader(fname));
            String ltl = in.readLine();
            in.close();
            return ltl;
        } catch (final FileNotFoundException e) {
            throw new LTLErrorException("Can't load LTL formula: " + fname);
        } catch (final IOException e) {
            throw new LTLErrorException("Error read on LTL formula: " + fname);
        }
    }

    private static String readLTL() {
        try {
            final BufferedReader in = new BufferedReader(new InputStreamReader(
                    System.in));

            System.out.print("\nInsert LTL formula: ");

            return in.readLine();
        } catch (final IOException e) {
            throw new LTLErrorException("Invalid LTL formula");
        }
    }
}
