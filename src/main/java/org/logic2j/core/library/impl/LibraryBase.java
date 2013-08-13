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
package org.logic2j.core.library.impl;

import org.logic2j.core.PrologImplementor;
import org.logic2j.core.TermFactory.FactoryMode;
import org.logic2j.core.library.PLibrary;
import org.logic2j.core.library.mgmt.PrimitiveInfo;
import org.logic2j.core.library.mgmt.PrimitiveInfo.PrimitiveType;
import org.logic2j.core.model.exception.PrologNonSpecificError;
import org.logic2j.core.model.symbol.Struct;
import org.logic2j.core.model.symbol.TNumber;
import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.model.symbol.TermApi;
import org.logic2j.core.model.symbol.Var;
import org.logic2j.core.model.var.Binding;
import org.logic2j.core.model.var.Bindings;
import org.logic2j.core.solver.GoalFrame;
import org.logic2j.core.solver.listener.SolutionListener;

/**
 * Base class for libraries.
 */
public class LibraryBase implements PLibrary {
    protected static final TermApi TERM_API = new TermApi();
    private final PrologImplementor prolog;

    public LibraryBase(PrologImplementor theProlog) {
        this.prolog = theProlog;
    }

    /**
     * Convenience shortcut to have the current Prolog engine unifying 2 terms.
     * 
     * @param t1
     * @param theBindings1
     * @param t2
     * @param theBindings2
     * @param theGoalFrame
     * @return The result of unification.
     */
    protected boolean unify(Term t1, Bindings theBindings1, Term t2, Bindings theBindings2, GoalFrame theGoalFrame) {
        return getProlog().getUnifyer().unify(t1, theBindings1, t2, theBindings2, theGoalFrame);
    }

    protected void deunify(GoalFrame theGoalFrame) {
        getProlog().getUnifyer().deunify(theGoalFrame);
    }

    /**
     * Notify theSolutionListener that a solution has been found.
     * 
     * @param theSolutionListener
     */
    protected void notifySolution(GoalFrame theGoalFrame, SolutionListener theSolutionListener) {
        final boolean userRequestsAbort = theSolutionListener.onSolution().isUserAbort();
        if (userRequestsAbort) {
            theGoalFrame.raiseUserCanceled();
        }
    }

    /**
     * When unified is true, call {@link #notifySolution(GoalFrame, SolutionListener)}, and then call {@link #deunify(GoalFrame)}. Otherwise nothing is done.
     * 
     * @param unified
     * @param theGoalFrame
     * @param theListener
     */
    protected void notifyIfUnified(boolean unified, GoalFrame theGoalFrame, SolutionListener theListener) {
        if (unified) {
            notifySolution(theGoalFrame, theListener);
            deunify(theGoalFrame);
        }
    }

    /**
     * @param theBindings
     * @param thePrimitive
     */
    protected void assertValidBindings(Bindings theBindings, String thePrimitive) {
        if (theBindings.isFreeReferrer()) {
            // TODO should be sort of an InvalidGoalException?
            throw new PrologNonSpecificError("Cannot call primitive " + thePrimitive + " with a free variable goal");
        }
    }

    // TODO assess if needed - used only once
    protected Binding dereferencedBinding(Term theTerm, Bindings theBindings) {
        if (theTerm instanceof Var) {
            return ((Var) theTerm).bindingWithin(theBindings).followLinks();
        }
        return Binding.createLiteralBinding(theTerm, theBindings);
    }

    /**
     * Evaluates an expression. Returns null value if the argument is not an evaluable expression
     */
    protected Term evaluateFunctor(Bindings theBindings, Term theTerm) {
        if (theTerm == null) {
            return null;
        }
        // TODO are the lines below this exactly as in resolve() / substitute() method?
        if (theTerm instanceof Var && !((Var) theTerm).isAnonymous()) {
            final Binding binding = ((Var) theTerm).bindingWithin(theBindings).followLinks();
            if (!binding.isLiteral()) {
                return null;
            }
            theTerm = binding.getTerm();
        }

        if (theTerm instanceof TNumber) {
            return theTerm;
        }
        if (theTerm instanceof Struct) {
            final Struct struct = (Struct) theTerm;
            final PrimitiveInfo desc = struct.getPrimitiveInfo();
            if (desc == null) {
                // throw new IllegalArgumentException("Predicate's functor " + struct.getName() + " is not a primitive");
                return null;
            }
            if (desc.getType() != PrimitiveType.FUNCTOR) {
                // throw new IllegalArgumentException("Predicate's functor " + struct.getName() + " is a primitive, but not a functor");
                return null;
            }
            return desc.invokeFunctor(struct, theBindings);
        }
        return null;
    }

    protected Term createConstantTerm(Object anyObject) {
        if (anyObject == null) {
            return Var.ANONYMOUS_VAR;
        }
        return getProlog().getTermFactory().create(anyObject, FactoryMode.ATOM);
    }

    protected void unifyAndNotify(Var[] theVariables, Object[] theValues, Bindings theBindings, GoalFrame theGoalFrame, SolutionListener theListener) {
        final Term[] values = new Term[theValues.length];
        for (int i = 0; i < theValues.length; i++) {
            values[i] = createConstantTerm(theValues[i]);
        }
        boolean unified = unify(new Struct("group", theVariables), theBindings, new Struct("group", values), theBindings, theGoalFrame);
        notifyIfUnified(unified, theGoalFrame, theListener);
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    /**
     * @return the prolog
     */
    protected PrologImplementor getProlog() {
        return this.prolog;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

}
