package synopticgwt.client.invariants;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import synopticgwt.client.ISynopticServiceAsync;
import synopticgwt.client.SynopticGWT;
import synopticgwt.client.Tab;
import synopticgwt.client.util.ProgressWheel;
import synopticgwt.shared.GWTInvariant;
import synopticgwt.shared.GWTInvariantSet;

/**
 * Represents the invariants tab. This tab visualizes the mined invariants, and
 * allows the users to select a subset of the invariants that they would like to
 * be satisfied by the model.
 */
public class InvariantsTab extends Tab<VerticalPanel> {
    public Set<Integer> activeInvsHashes = new LinkedHashSet<Integer>();
    
    /** Relate GWTInvariants to InvariantGridLabels */
    public Map<GWTInvariant, InvariantGridLabel> gwtInvToGridLabel;
    
    public InvariantsGraph iGraph = new InvariantsGraph();

    public InvariantsTab(ISynopticServiceAsync synopticService,
            ProgressWheel pWheel) {
        super(synopticService, pWheel);
        panel = new VerticalPanel();
        gwtInvToGridLabel = new HashMap<GWTInvariant, InvariantGridLabel>();
    }

    /**
     * Shows the invariant graphic on the screen in the invariantsPanel.
     * 
     * @param gwtInvs
     *            The invariants mined from the log.
     */
    public void showInvariants(GWTInvariantSet gwtInvs) {
        // Clear the invariants panel of any widgets it might have.
        panel.clear();

        // Create and populate the panel with the invariants grid.
        HorizontalPanel tableAndGraphicPanel = new HorizontalPanel();
        panel.add(tableAndGraphicPanel);

        // Iterate through all invariants to add them to the grid / table.
        for (String invType : gwtInvs.getInvTypes()) {
            List<GWTInvariant> invs = gwtInvs.getInvs(invType);

            // This loop creates a grid for each Invariant type with one column
            // and as many rows necessary to contain all of the invariants
            Grid grid = new Grid(invs.size() + 1, 1);
            grid.setStyleName("invariantsGrid grid");
            String longType = "Unknown Invariant Type";
            String unicodeType = GWTInvariant.getUnicodeTransitionType(invType);

            if (invType.equals("AFby")) {
                longType = "AlwaysFollowedBy ( " + unicodeType + " )";
            } else if (invType.equals("AP")) {
                longType = "AlwaysPrecedes ( " + unicodeType + " )";
            } else if (invType.equals("NFby")) {
                longType = "NeverFollowedBy ( " + unicodeType + " )";
            } else if (invType.equals("NCwith")) {
                longType = "NeverConcurrentWith ( " + unicodeType + " )";
            } else if (invType.equals("ACwith")) {
                longType = "AlwaysConcurrentWith ( " + unicodeType + " )";
            }
            grid.setWidget(0, 0, new Label(longType));
            grid.getCellFormatter().setStyleName(0, 0, "tableCellTopRow");
            tableAndGraphicPanel.add(grid);

            // Activated and deactivated grid cells are uniquely styled
            for (int i = 0; i < invs.size(); i++) {
                GWTInvariant inv = invs.get(i);
                InvariantGridLabel iGridLabel = new InvariantGridLabel(inv);
                gwtInvToGridLabel.put(inv, iGridLabel);
                grid.setWidget(i + 1, 0, iGridLabel);
                grid.getCellFormatter().setStyleName(i + 1, 0,
                        "tableCellInvActivated");
            }

            // Add a click handler to the grid that allows users to
            // include/exclude invariants for use by Synoptic.
            grid.addClickHandler(new InvGridClickHandler(grid, invs));
        }

        // Show the TO invariant graphic only if there are no concurrency
        // invariants in the set of invariants.
        if (!gwtInvs.containsConcurrencyInvs) {
            String invCanvasId = "invCanvasId";
            HorizontalPanel invGraphicPanel = new HorizontalPanel();
            invGraphicPanel.getElement().setId(invCanvasId);
            invGraphicPanel.setStylePrimaryName("modelCanvas");
            tableAndGraphicPanel.add(invGraphicPanel);
            iGraph.createInvariantsGraphic(gwtInvs, invCanvasId, gwtInvToGridLabel);
        }
    }

    /**
     * Handler for clicks on the grid showing mined invariants. On click, the
     * grid's cell data will be looked up in the client-side set of invariants.
     * This client-side invariant then contains the server-side hashcode for the
     * corresponding server-side invariant. If the cell contains an "activated"
     * invariant (an invariant that is used to constrain the model), then an RPC
     * to deactivate the invariant is invoked. Otherwise, the RPC call to
     * activate the invariant is invoked.
     * 
     * @param invs
     *            The set of client-side invariants
     * @param grid
     *            The grid which will become clickable.
     */
    class InvGridClickHandler implements ClickHandler {
        List<GWTInvariant> invs;
        Grid grid;

        public InvGridClickHandler(Grid grid, List<GWTInvariant> invs) {
            this.invs = invs;
            this.grid = grid;
        }

        @Override
        public void onClick(ClickEvent event) {
            // The clicked cell.
            Cell cell = ((Grid) event.getSource()).getCellForEvent(event);

            // Cell's row index.
            int cIndex = cell.getRowIndex();

            // Ignore title cells.
            if (cIndex == 0) {
                return;
            }

            // Invariant label corresponding to the cell.
            InvariantGridLabel invLabel = ((InvariantGridLabel) grid.getWidget(
                    cIndex, 0));

            int invID = invLabel.getInvariant().getID();

            SynopticGWT.entryPoint.invSetChanged();

            CellFormatter cFormatter = grid.getCellFormatter();

            // Corresponding invariant is activated => deactive it.
            if (invLabel.getActive()) {
                activeInvsHashes.remove(invID);
                invLabel.setActive(false);
                cFormatter.setStyleName(cIndex, 0, "tableCellInvDeactivated");
            } else { // Corresponding invariant is deactivated => activate it.
                activeInvsHashes.add(invID);
                invLabel.setActive(true);
                cFormatter.setStyleName(cIndex, 0, "tableCellInvActivated");
            }
        }
    }
}
