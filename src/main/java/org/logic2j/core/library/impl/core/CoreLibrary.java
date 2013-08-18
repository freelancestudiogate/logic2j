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
package org.logic2j.core.library.impl.core;

import java.util.ArrayList;

import org.logic2j.core.ClauseProvider;
import org.logic2j.core.impl.PrologImplementation;
import org.logic2j.core.library.impl.LibraryBase;
import org.logic2j.core.library.mgmt.Primitive;
import org.logic2j.core.model.Clause;
import org.logic2j.core.model.exception.InvalidTermException;
import org.logic2j.core.model.exception.PrologNonSpecificError;
import org.logic2j.core.model.symbol.Struct;
import org.logic2j.core.model.symbol.TDouble;
import org.logic2j.core.model.symbol.TLong;
import org.logic2j.core.model.symbol.TNumber;
import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.model.symbol.Var;
import org.logic2j.core.model.var.Binding;
import org.logic2j.core.model.var.Bindings;
import org.logic2j.core.solver.GoalFrame;
import org.logic2j.core.solver.listener.SolutionListener;
import org.logic2j.core.solver.listener.SolutionListenerBase;
import org.logic2j.core.util.ReflectUtils;

public class CoreLibrary extends LibraryBase {
    static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CoreLibrary.class);

    public CoreLibrary(PrologImplementation theProlog) {
        super(theProlog);
    }

    @Primitive(name = Struct.FUNCTOR_TRUE)
    // We can't name the method "true" it's a Java reserved word...
    public void trueFunctor(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings) {
        notifySolution(theGoalFrame, theListener);
    }

    @Primitive
    public void fail(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings) {
        // Do not propagate a solution - that's all
    }

    @Primitive
    public void var(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term t1) {
        if (t1 instanceof Var) {
            final Var var = (Var) t1;
            if (var.isAnonymous()) {
                notifySolution(theGoalFrame, theListener);
            } else {
                final Binding binding = var.bindingWithin(theBindings).followLinks();
                if (!binding.isLiteral()) {
                    // Not ending on a literal, we end up on a free var!
                    notifySolution(theGoalFrame, theListener);
                }
            }
        }
    }

    @Primitive
    public void atomic(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term theTerm) {
        final Bindings b = theBindings.focus(theTerm, Term.class);
        assertValidBindings(b, "atomic/1");
        final Term effectiveTerm = b.getReferrer();
        if (effectiveTerm instanceof Struct || effectiveTerm instanceof TNumber) {
            notifySolution(theGoalFrame, theListener);
        }
    }

    @Primitive
    public void number(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term theTerm) {
        final Bindings b = theBindings.focus(theTerm, Term.class);
        assertValidBindings(b, "number/1");
        final Term effectiveTerm = b.getReferrer();
        if (effectiveTerm instanceof TNumber) {
            notifySolution(theGoalFrame, theListener);
        }
    }

    @Primitive(name = Struct.FUNCTOR_CUT)
    public void cut(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings) {
        // This is a complex behaviour - read on DefaultSolver
        theGoalFrame.signalCut();
        // Cut is a "true" solution to a goal, just notify one as such
        notifySolution(theGoalFrame, theListener);
    }

    @Primitive(name = "=")
    public void unify(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term t1, Term t2) {
        final boolean unified = unify(t1, theBindings, t2, theBindings, theGoalFrame);
        notifyIfUnified(unified, theGoalFrame, theListener);
    }

    @Primitive(name = "\\=")
    public void notUnify(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term t1, Term t2) {
        final boolean unified = unify(t1, theBindings, t2, theBindings, theGoalFrame);
        if (!unified) {
            notifySolution(theGoalFrame, theListener);
        }
        if (unified) {
            deunify(theGoalFrame);
        }
    }

    @Primitive
    public void atom_length(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term theAtom, Term theLength) {
        final Bindings atomBindings = theBindings.focus(theAtom, Struct.class);
        assertValidBindings(atomBindings, "atom_length/2");
        final Struct atom = (Struct) atomBindings.getReferrer();

        final TLong atomLength = createTLong(atom.getName().length());
        final boolean unified = unify(atomLength, atomBindings, theLength, theBindings, theGoalFrame);
        notifyIfUnified(unified, theGoalFrame, theListener);
    }

    @Primitive(synonyms = "\\+")
    // Surprisingly enough the operator \+ means "not provable".
    public void not(final SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term theGoal) {
        final Bindings subGoalBindings = theBindings.focus(theGoal, Struct.class);
        assertValidBindings(subGoalBindings, "\\+/1");

        // final Term target = resolveNonVar(theGoal, theBindings, "not");
        final class NegationListener implements SolutionListener {
            boolean found = false;

            @Override
            public Continuation onSolution() {
                // Do NOT relay the solution further, just remember there was one
                this.found = true;
                return Continuation.USER_ABORT; // No need to seek for further solutions
            }
        }
        final NegationListener callListener = new NegationListener();
        getProlog().getSolver().solveGoal(subGoalBindings, theGoalFrame, callListener);
        if (!callListener.found) {
            theListener.onSolution();
        }
    }

    @Primitive
    public void findall(SolutionListener theListener, GoalFrame theGoalFrame, final Bindings theBindings, final Term theTemplate, final Term theGoal, final Term theResult) {
        final Bindings subGoalBindings = theBindings.focus(theGoal, Term.class);
        assertValidBindings(subGoalBindings, "findall/3");

        // Define a listener to collect all solutions for the goal specified
        final ArrayList<Term> javaResults = new ArrayList<Term>(); // Our internal collection of results
        final SolutionListenerBase adHocListener = new SolutionListenerBase() {

            @Override
            public Continuation onSolution() {
                // Calculate the substituted goal value (resolve bindings)
                @SuppressWarnings("synthetic-access")
                // FIXME This is most certainly wrong: how can we call substitute on a variable expressed in a different bindings?????
                // The case is : findall(X, Expr, Result) where Expr -> something -> expr(a,b,X,c)
                final Term substitute = TERM_API.substitute(theTemplate, subGoalBindings, null);
                // Map<String, Term> explicitBindings = goalBindings.explicitBindings(FreeVarRepresentation.FREE);
                // And add as extra solution
                javaResults.add(substitute);
                return Continuation.CONTINUE;
            }

        };

        // Now solve the target goal, this may find several values of course
        // final Term effectiveGoal = subGoalBindings.getReferrer();
        // getProlog().getSolver().solveGoalRecursive(effectiveGoal, subGoalBindings, new GoalFrame(), adHocListener);
        getProlog().getSolver().solveGoal(subGoalBindings, new GoalFrame(), adHocListener);

        // Convert all results into a prolog list structure
        // Note on var indexes: all variables present in the projection term will be
        // copied into the resulting plist, so there's no need to reindex.
        // However, the root level Struct that makes up the list does contain a bogus
        // index value but -1.
        final Struct plist = Struct.createPList(javaResults);

        // And unify with result
        final boolean unified = unify(theResult, theBindings, plist, theBindings, theGoalFrame);
        notifyIfUnified(unified, theGoalFrame, theListener);
    }

    @Primitive
    public void clause(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term theHead, Term theBody) {
        final Binding dereferencedBinding = dereferencedBinding(theHead, theBindings);
        final Struct realHead = ReflectUtils.safeCastNotNull("dereferencing argumnent for clause/2", dereferencedBinding.getTerm(), Struct.class);
        for (final ClauseProvider cp : getProlog().getTheoryManager().getClauseProviders()) {
            for (final Clause clause : cp.listMatchingClauses(realHead, theBindings)) {
                // Clone the clause so that we can unify against its bindings
                final Clause clauseToUnify = new Clause(clause);
                final boolean headUnified = unify(clauseToUnify.getHead(), clauseToUnify.getBindings(), realHead, dereferencedBinding.getLiteralBindings(), theGoalFrame);
                if (headUnified) {
                    final boolean bodyUnified = unify(clauseToUnify.getBody(), clauseToUnify.getBindings(), theBody, theBindings, theGoalFrame);
                    if (bodyUnified) {
                        notifySolution(theGoalFrame, theListener);
                        deunify(theGoalFrame);
                    }
                    deunify(theGoalFrame);
                }
            }
        }
    }

    @Primitive(name = "=..")
    public void predicate2PList(final SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term thePredicate, Term theList) {
        Bindings resolvedBindings = theBindings.focus(thePredicate, Term.class);

        if (resolvedBindings.isFreeReferrer()) {
            // thePredicate is still free, going ot match from theList
            resolvedBindings = theBindings.focus(theList, Term.class);
            assertValidBindings(resolvedBindings, "=../2");
            if (resolvedBindings.isFreeReferrer()) {
                throw new PrologNonSpecificError("Predicate =.. does not accept both arguments as free variable");
            }
            final Struct lst2 = (Struct) resolvedBindings.getReferrer();
            final Struct flattened = lst2.predicateFromPList();
            final boolean unified = unify(thePredicate, theBindings, flattened, resolvedBindings, theGoalFrame);
            notifyIfUnified(unified, theGoalFrame, theListener);
        } else {
            final Term predResolved = resolvedBindings.getReferrer();
            if (predResolved instanceof Struct) {
                final Struct struct = (Struct) predResolved;
                final ArrayList<Term> elems = new ArrayList<Term>();
                elems.add(new Struct(struct.getName())); // Only copying the functor as an atom, not a deep copy of the struct!
                final int arity = struct.getArity();
                for (int i = 0; i < arity; i++) {
                    elems.add(struct.getArg(i));
                }
                final Struct plist = Struct.createPList(elems);
                final boolean unified = unify(theList, theBindings, plist, resolvedBindings, theGoalFrame);
                notifyIfUnified(unified, theGoalFrame, theListener);
            }
        }
    }

    @Primitive
    public void is(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term t1, Term t2) {
        final Term evaluated = evaluateFunctor(theBindings, t2);
        if (evaluated == null) {
            return;
        }
        final boolean unified = unify(t1, theBindings, evaluated, theBindings, theGoalFrame);
        notifyIfUnified(unified, theGoalFrame, theListener);
    }

    @Primitive(name = ">")
    public void expression_greater_than(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term t1, Term t2) {
        t1 = evaluateFunctor(theBindings, t1);
        t2 = evaluateFunctor(theBindings, t2);
        if (t1 instanceof TNumber && t2 instanceof TNumber) {
            final TNumber val0n = (TNumber) t1;
            final TNumber val1n = (TNumber) t2;
            if (val0n.longValue() > val1n.longValue()) {
                notifySolution(theGoalFrame, theListener);
            }
        }
    }

    @Primitive(name = "<")
    public void expression_lower_than(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term t1, Term t2) {
        t1 = evaluateFunctor(theBindings, t1);
        t2 = evaluateFunctor(theBindings, t2);
        if (t1 instanceof TNumber && t2 instanceof TNumber) {
            final TNumber val0n = (TNumber) t1;
            final TNumber val1n = (TNumber) t2;
            if (val0n.longValue() < val1n.longValue()) {
                notifySolution(theGoalFrame, theListener);
            }
        }
    }

    @Primitive(name = "+")
    public Term plus(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term t1, Term t2) {
        t1 = evaluateFunctor(theBindings, t1);
        t2 = evaluateFunctor(theBindings, t2);
        if (t1 instanceof TNumber && t2 instanceof TNumber) {
            final TNumber val0n = (TNumber) t1;
            final TNumber val1n = (TNumber) t2;
            if (val0n instanceof TLong && val1n instanceof TLong) {
                return createTLong(val0n.longValue() + val1n.longValue());
            }
            return new TDouble(val0n.doubleValue() + val1n.doubleValue());
        }
        throw new InvalidTermException("Could not add because 2 terms are not Numbers: " + t1 + " and " + t2);
    }

    /**
     * @param theGoalFrame
     * @param theBindings
     * @param t1
     * @param t2
     * @return Binary minus (subtract)
     */
    @Primitive(name = "-")
    public Term minus(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term t1, Term t2) {
        t1 = evaluateFunctor(theBindings, t1);
        t2 = evaluateFunctor(theBindings, t2);
        if (t1 instanceof TNumber && t2 instanceof TNumber) {
            final TNumber val0n = (TNumber) t1;
            final TNumber val1n = (TNumber) t2;
            if (val0n instanceof TLong && val1n instanceof TLong) {
                return createTLong(val0n.longValue() - val1n.longValue());
            }
            return new TDouble(val0n.doubleValue() - val1n.doubleValue());
        }
        throw new InvalidTermException("Could not subtract because 2 terms are not Numbers: " + t1 + " and " + t2);
    }

    /**
     * @param theGoalFrame
     * @param theBindings
     * @param t1
     * @param t2
     * @return Binary multiply
     */
    @Primitive(name = "*")
    public Term multiply(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term t1, Term t2) {
        t1 = evaluateFunctor(theBindings, t1);
        t2 = evaluateFunctor(theBindings, t2);
        if (t1 instanceof TNumber && t2 instanceof TNumber) {
            final TNumber val0n = (TNumber) t1;
            final TNumber val1n = (TNumber) t2;
            if (val0n instanceof TLong && val1n instanceof TLong) {
                return createTLong(val0n.longValue() * val1n.longValue());
            }
            return new TDouble(val0n.doubleValue() * val1n.doubleValue());
        }
        throw new InvalidTermException("Could not multiply because 2 terms are not Numbers: " + t1 + " and " + t2);
    }

    /**
     * @param theGoalFrame
     * @param theBindings
     * @param t1
     * @return Unary minus (negate)
     */
    @Primitive(name = "-")
    public Term minus(SolutionListener theListener, GoalFrame theGoalFrame, Bindings theBindings, Term t1) {
        t1 = evaluateFunctor(theBindings, t1);
        if (t1 instanceof TNumber) {
            final TNumber val0n = (TNumber) t1;
            if (val0n instanceof TDouble) {
                return new TDouble(val0n.doubleValue() * -1);
            } else if (val0n instanceof TLong) {
                return new TLong(val0n.longValue() * -1);
            }
        }
        throw new InvalidTermException("Could not negate because argument " + t1 + " is not TNumber but " + t1.getClass());
    }

    private TLong createTLong(long num) {
        return new TLong(num);
    }

}
