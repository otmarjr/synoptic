package tests.units;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import synoptic.invariants.AlwaysFollowedInvariant;
import synoptic.invariants.AlwaysPrecedesInvariant;
import synoptic.invariants.NeverFollowedInvariant;

/**
 * Uses the Automaton class to test if the getRegex methods for Synoptic's temporal invariants
 * are correct.
 * 
 * @author Jenny
 *
 */
public class InvariantRegexTests {
	
	@Test
	public void testNFbyRegex() {
	    NeverFollowedInvariant nfby = new NeverFollowedInvariant("x", "y", "t");
	    Automaton model = new RegExp(nfby.getRegex('x', 'y')).toAutomaton();
	    model.minimize();
	    
	    assertTrue(model.run("ax"));
	    assertTrue(model.run("axabc"));
	    assertTrue(model.run("ayyyyfsfsfx"));
	    
	    assertFalse(model.run("axabcy"));
	    assertFalse(model.run("axabcyx"));
	}
    
	@Test
	public void testAFbyRegex() {
	    AlwaysFollowedInvariant afby = new AlwaysFollowedInvariant("x", "y", "t");
	    Automaton model = new RegExp(afby.getRegex('x', 'y')).toAutomaton();
	    model.minimize();

	    assertTrue(model.run("axy"));
	    assertTrue(model.run("axabcy"));
	    assertTrue(model.run("axabcyabd"));
	    
	    assertFalse(model.run("axabc"));
	    assertFalse(model.run("ayyyyfsfsfx"));
	    assertFalse(model.run("axabcyx"));
	}
   
    @Test
    public void testAPRegex() {
	    AlwaysPrecedesInvariant ap = new AlwaysPrecedesInvariant("x", "y", "t");
	    Automaton model = new RegExp(ap.getRegex('x', 'y')).toAutomaton();
	    model.minimize();
	    
	    assertTrue(model.run("axy"));
	    assertTrue(model.run("axabcy"));
	    assertTrue(model.run("axabc"));
	    assertTrue(model.run("axabcyx"));
	    assertTrue(model.run("axabcyabd"));
	    assertTrue(model.run("xyyyyyyy"));
	    
	    assertFalse(model.run("ayyyyfsfsfx"));
	    assertFalse(model.run("fsyxyx"));
    }
}
