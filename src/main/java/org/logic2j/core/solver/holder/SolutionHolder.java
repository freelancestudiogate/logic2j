/*
 * logic2j - "Bring Logic to your Java" - Copyright (C) 2011 Laurent.Tettoni@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.logic2j.core.solver.holder;

import java.util.Iterator;

import org.logic2j.core.PrologImplementor;
import org.logic2j.core.model.exception.PrologNonSpecificError;
import org.logic2j.core.model.symbol.TermApi;
import org.logic2j.core.model.var.Bindings;
import org.logic2j.core.solver.GoalFrame;
import org.logic2j.core.solver.listener.IterableSolutionListener;
import org.logic2j.core.solver.listener.Solution;
import org.logic2j.core.solver.listener.UniqueSolutionListener;

/**
 * Holds state necessary to describe the unique solution or multiple solutions to a goal; this object lies between the expression of a goal
 * (a request or query) and the extraction of all aspects of the solution (a response or results). <br/>
 * IMPORTANT: obtaining a {@link SolutionHolder} usually means that the execution has not yet started - state is held ready to execute,
 * until you tell what you want! <br/>
 * This object exposes strongly-typed, templated methods to extract results depending on how the calling code expects them (unique or
 * multiple solutions), and the type of data needed (just the number, resolved-term solutions or single variable bindings).<br/>
 * Depending on the case, the actual calculation of the goal may be performed immediately (then results are stored and returned as needed),
 * or delayed until access methods are called.
 *
 * TODO Maybe have a way to allow limiting "all" to a reasonable number.
 *
 * <p/>
 * This type of API for extracting results from a data layer should further be analyzed and compared to other APIs such as JDBC, JNDI,
 * SAX/DOM, or more exotic ones such as JSon (MongoDB/Apache CouchDB), Neo4j and Prot�g�. Also RDF frameworks APIs may be considered.
 */
public class SolutionHolder {
    static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SolutionHolder.class);

    static final TermApi TERM_API = new TermApi();

    final PrologImplementor prolog;
    final Bindings bindings;

    public SolutionHolder(PrologImplementor theProlog, Bindings theBindings) {
        this.prolog = theProlog;
        this.bindings = theBindings;
    }

    /**
     * Number of solutions - without their content.
     *
     * @return The value of {@link #all()}.{@link MultipleSolutionsHolder#number()}.
     */
    public int number() {
        return all().number();
    }

    /**
     * Indicate you are interested in all results - calling this method does NOT start solving.
     *
     * @return A relay object to access all solutions.
     */
    public MultipleSolutionsHolder all() {
        return new MultipleSolutionsHolder(this);
    }

    /**
     * Solves the goal, and holds the solution for further dereferencing.
     *
     * @return A relay object to access the unique solution.
     */
    public UniqueSolutionHolder unique() {
        final UniqueSolutionListener listener = new UniqueSolutionListener(this.bindings);
        this.prolog.getSolver().solveGoal(this.bindings, new GoalFrame(), listener);
        return new UniqueSolutionHolder(listener.getSolution());
    }

    /**
     * Launch the prolog engine in a separate thread to produce solutions while the main caller can consume {@link Solution}s from this
     * {@link Iterator} at its own pace. This uses the {@link IterableSolutionListener}.
     *
     * @return An iterator for all solutions.
     */
    public Iterator<Solution> iterator() {
        final IterableSolutionListener listener = new IterableSolutionListener(SolutionHolder.this.bindings);

        final Runnable prologSolverThread = new Runnable() {

            @Override
            public void run() {
                logger.debug("Started producer (prolog solver engine) thread");
                // Start solving in a parallel thread, and rush to first solution (that will be called back in the listener)
                // and will wait for the main thread to extract it
                SolutionHolder.this.prolog.getSolver().solveGoal(SolutionHolder.this.bindings, new GoalFrame(), listener);
                logger.debug("Producer (prolog solver engine) thread finishes");
                // Last solution was extracted. Producer's callback won't now be called any more - so to
                // prevent the consumer for listening forever for the next solution that won't come...
                // We wait from a last notify from our client
                listener.clientToEngineInterface().waitUntilAvailable();
                // And we tell it we are aborting. No solution transferred for this last "hang up" message
                listener.engineToClientInterface().wakeUp();
                // Notice the 2 lines above are exactly the sames as those in the listener's onSolution()
            }
        };
        new Thread(prologSolverThread).start();

        return new Iterator<Solution>() {

            private Solution solution;

            @Override
            public boolean hasNext() {
                // Now ask engine to run...
                listener.clientToEngineInterface().wakeUp();
                // And wait for a solution. Store it in any case we need it in next()
                this.solution = listener.engineToClientInterface().waitUntilAvailable();
                // Did it get one?
                return this.solution != null;
            }

            @Override
            public Solution next() {
                if (this.solution == null) {
                    throw new PrologNonSpecificError("Program error: next() called when either hasNext() did not return true previously, or next() was called more than once");
                }
                final Solution toReturn = this.solution;
                // Indicate that we have just "consumed" the solution, and any subsequent call to next() without first calling hasNext()
                // will fail.
                this.solution = null;
                return toReturn;
            }

            @Override
            public void remove() {
                throw new PrologNonSpecificError("iterator() provides a read-only Term interator, cannot remove elements");
            }

        };
    }

}
