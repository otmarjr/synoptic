package algorithms;

import java.io.IOException;
import java.util.logging.Logger;

import model.InvModel;
import model.InvsModel;

import synoptic.invariants.ITemporalInvariant;
import synoptic.invariants.TemporalInvariantSet;

public class InvComposition {

    public static Logger logger;
    static {
        logger = Logger.getLogger("InvComposition");
    }

    /**
     * Constructs an InvsModel by intersecting InvModels for each of the given
     * temporal invariants.
     * 
     * @param invariants
     *            a set of TemporalInvariants
     * @param minimize
     *            whether or not to minimize the model before returning.
     * @return the intersected InvsModel
     * @throws IOException
     */
    public static InvsModel intersectModelWithInvs(
            TemporalInvariantSet invariants, boolean minimizeDFAIntersections,
            InvsModel model) throws IOException {

        // Intersect invariants into model.
        int remaining = invariants.numInvariants();
        for (ITemporalInvariant invariant : invariants) {
            logger.info("Create new invDFA instance, remaining" + remaining);
            InvModel invDFA = new InvModel(invariant, model.getEventEncodings());
            model.intersectWith(invDFA);
            invDFA = null;

            if (minimizeDFAIntersections) {
                logger.info("Minimizing intersection");
                // Optimize by minimizing the model.
                model.minimize();
            }

            if (remaining % 100 == 0) {
                logger.info("Minimizing intersection % 100");
                // Optimize by minimizing the model.
                model.minimize();
            }
            remaining -= 1;

        }

        if (minimizeDFAIntersections) {
            // Optimize by minimizing the model.
            model.minimize();
        }

        return model;
    }

}
