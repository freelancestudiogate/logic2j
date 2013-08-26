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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.logic2j.core.model.exception.InvalidTermException;
import org.logic2j.core.model.exception.PrologNonSpecificError;
import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.model.symbol.Var;
import org.logic2j.core.model.var.Bindings;
import org.logic2j.core.model.var.Bindings.FreeVarRepresentation;
import org.logic2j.core.solver.Continuation;
import org.logic2j.core.solver.listener.SolutionListenerBase;

/**
 * A relay object to provide access to the results of all the solutions to a goal.
 */
public class MultipleSolutionsHolder {

    private final SolutionHolder solutionHolder;

    MultipleSolutionsHolder(SolutionHolder theSolutionHolder) {
        this.solutionHolder = theSolutionHolder;
    }

    private Integer lowest = null; // FIXME to document
    private Integer highest = null; // FIXME to document

    /**
     * Start solving but indicating you are not interested by the actual solutions nor bindings, only their number.
     * 
     * @return The number of solutions
     */
    public int number() {
        final SolutionListenerBase listener = new SolutionListenerBase();
        this.solutionHolder.prolog.getSolver().solveGoal(this.solutionHolder.bindings, listener);
        final int counter = listener.getCounter();
        checkBounds(counter);
        return counter;
    }

    private void checkBounds(int counter) {
        if (this.lowest != null && counter < this.lowest) {
            throw new PrologNonSpecificError("Number of solutions was expected to be at least " + this.lowest + " but was " + counter);
        }
        if (this.highest != null && counter > this.highest) {
            throw new PrologNonSpecificError("Number of solutions was expected to be at most " + this.highest + " but was " + counter);
        }
    }

    /**
     * Solves the goal and extract, for every solution, the value for a given variable by name.
     * 
     * @param theVariableName
     */
    public List<Term> binding(final String theVariableName) {
        final List<Term> results = new ArrayList<Term>();

        final Bindings bindings = this.solutionHolder.bindings;
        final SolutionListenerBase listener = new SolutionListenerBase() {

            @Override
            public Continuation onSolution() {
                final Bindings bnd = bindings;
                final Term term = bnd.getReferrer();
                final Var var = term.findVar(theVariableName);
                if (var == null) {
                    throw new InvalidTermException("No variable named \"" + theVariableName + "\" in " + term);
                }
                final Term substituted = SolutionHolder.TERM_API.substitute(var, bnd, null);
                results.add(substituted);
                return super.onSolution();
            }

        };
        this.solutionHolder.prolog.getSolver().solveGoal(bindings, listener);
        final int size = results.size();
        checkBounds(size);
        return results;
    }

    /**
     * Solves the goal and extract, for every solution, all bindings for all variables.
     * 
     * @result An ordered list of bindings
     */
    public List<Map<String, Term>> bindings() {
        final List<Map<String, Term>> results = new ArrayList<Map<String, Term>>();
        final SolutionListenerBase listener = new SolutionListenerBase() {

            @Override
            public Continuation onSolution() {
                results.add(MultipleSolutionsHolder.this.solutionHolder.bindings.explicitBindings(FreeVarRepresentation.FREE));
                return super.onSolution();
            }

        };
        this.solutionHolder.prolog.getSolver().solveGoal(this.solutionHolder.bindings, listener);
        final int size = results.size();
        checkBounds(size);
        return results;
    }

    /**
     * Set internal bounds to make sure the next goal to solve has exactly the specified number of solutions.
     * 
     * @param theExpectedExactNumber
     * @return this
     */
    public MultipleSolutionsHolder ensureNumber(int theExpectedExactNumber) {
        this.lowest = this.highest = theExpectedExactNumber;
        return this;
    }

    /**
     * Set internal bounds to make sure the next goal to solve has a number of solutions between the specified bounds, inclusive.
     * 
     * @param thePermissibleLowest
     * @param thePermissibleHighest
     * @return this
     */
    public MultipleSolutionsHolder ensureRange(int thePermissibleLowest, int thePermissibleHighest) {
        this.lowest = thePermissibleLowest;
        this.highest = thePermissibleHighest;
        return this;
    }

}