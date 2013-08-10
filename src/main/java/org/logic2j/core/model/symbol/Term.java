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
package org.logic2j.core.model.symbol;

import java.io.Serializable;
import java.util.Collection;
import java.util.IdentityHashMap;

import org.logic2j.core.Formatter;
import org.logic2j.core.io.format.DefaultFormatter;
import org.logic2j.core.model.TermVisitor;
import org.logic2j.core.model.exception.InvalidTermException;
import org.logic2j.core.model.var.Binding;
import org.logic2j.core.model.var.Bindings;

/**
 * Term class is the root abstract class for all Prolog data types. The following notions apply on terms, see also the {@link TermApi} class for methods to manage {@link Term}s.
 * <ul>
 * <li>Structural equality, see {@link #structurallyEquals(Term)}</li>
 * <li>Compaction, see {@link #compact(Collection)}</li>
 * <li>Initialization of {@link Var} indexes, see {@link #assignIndexes(short)}</li>
 * </ul>
 * 
 * @see Struct
 * @see Var
 * @see TNumber
 */
public abstract class Term implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    /**
     * A value of index=={@value} means it was not initialized.
     */
    public static final int NO_INDEX = -1;

    /**
     * A value of index=={@value} means this is the anonymous variable.
     */
    public static final int ANON_INDEX = -2;

    /**
     * For {@link Var}s: defines the position index within {@link Bindings} where the {@link Binding} of this variable can be found.<br/>
     * For a {@link Struct}: defines the maximal index of any variables that can be found, recursively, under all arguments.<br/>
     * For anyghing else: value is 0 to indicate it was processed (non-default).<br/>
     * The default value is NO_INDEX.
     */
    protected short index = NO_INDEX;

    /**
     * A {@link Formatter} for the default's {@link #toString()} to render this {@link Term}. Calling {@link #toString()} should be avoided from caller code, use your preferred instance of
     * {@link Formatter} instead. Yet we need {@link #toString()} to work for the cases of logging and while debugging, so let's use a fixed (static) one.
     */
    private static final Formatter formatter = new DefaultFormatter();

    // Use the following line instead to really debug the display of terms:
    // private static final Formatter formatter = new
    // org.logic2j.core.optional.io.format.DetailedFormatter();

    // ---------------------------------------------------------------------------
    // Template methods (abstract here, implemented in all derived classes)
    // ---------------------------------------------------------------------------

    /**
     * @return true if this Term is an atom.
     */
    // TODO Remove this it's only used from two places! Have a separate place
    // for such methods.
    public abstract boolean isAtom();

    /**
     * @return true if this Term denotes a Prolog list.
     */
    // TODO Remove this it's only used from two places! Have a separate place
    // for such methods.
    public abstract boolean isList();


    /**
     * Check structural equality, this means that the names of atoms, functors, arity and numeric values are all equal, that the same variables are referred to, but irrelevant of the bound values of
     * those variables.
     * 
     * @param theOther
     * @return true when theOther is structurally equal to this. Same references (==) will always yield true.
     */
    public abstract boolean structurallyEquals(Term theOther);


    public abstract <T> T accept(TermVisitor<T> theVisitor);

    // ---------------------------------------------------------------------------
    // Graph traversal methods
    // Some traversal are implemented by the Visitor design pattern and the #accept() method
    // ---------------------------------------------------------------------------

    /**
     * Recursively collect all terms and add them to the collectedTerms collection, and also initialize the {@link #index} to {@link #NO_INDEX}.
     * 
     * @param collectedTerms Recipient collection, {@link Term}s add here.
     */
    protected abstract void collectTermsInto(Collection<Term> theCollectedTerms);

    /**
     * Compacting will either return a new {@link Term} or this {@link Term} depending if it already exists in the supplied Collection.
     * This will factorize duplicated atoms, numbers, variables, or even structures that are statically equal.
     * @param theCollectedTerms The reference Terms to search for.
     * @return Either this, or a new equivalent but compacted Term.
     */
    protected abstract Term compact(Collection<Term> theCollectedTerms);

    /**
     * Assign the {@link Term#index} value for {@link Var} and {@link Struct}s.
     * @param theIndexOfNextNonIndexedVar
     * @return The next value for theIndexOfNextNonIndexedVar, allow successive calls to increment.
     */
    protected abstract short assignIndexes(short theIndexOfNextNonIndexedVar);

    /**
     * Internal template method; the public API entry point is {@link TermApi#substitute(Term, Bindings, IdentityHashMap)}.
     * 
     * @param theBindings
     * @param theBindingsToVars
     * @return A possibly new (cloned) term with all non-free bindings resolved.
     */
    protected abstract Term substitute(Bindings theBindings, IdentityHashMap<Binding, Var> theBindingsToVars);

    /**
     * Find the first instance of {@link Var} by name inside a Term, most often a {@link Struct}.
     * 
     * @param theVariableName
     * @return A {@link Var} with the specified name, or null when not found.
     */
    public abstract Var findVar(String theVariableName);

    /**
     * Find the first {@link Term} that is either same, or structurally equal to this.
     * 
     * @param findWithin
     * @return The {@link Term} found or null when none found.
     */
    protected Term findStructurallyEqualWithin(Collection<Term> findWithin) {
        for (final Term term : findWithin) {
            if (term != this && term.structurallyEquals(this)) {
                return term;
            }
        }
        return null;
    }

    /**
     * Format using a specific {@link Formatter}.
     * 
     * @note Generally, avoid {@link #toString()} without an argument, prefer this method.
     * 
     * @param theFormatter The Formatter to use to format this Term (and of course any subclass).
     * @return The formatted Term
     */
    public String toString(Formatter theFormatter) {
        return accept(theFormatter);
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    public short getIndex() {
        return this.index;
    }

    // ---------------------------------------------------------------------------
    // Core java.lang.Object methods
    // ---------------------------------------------------------------------------

    /**
     * An equivalent to {@link #clone()} that does not throw the checked exception {@link CloneNotSupportedException}.
     * 
     * @return A deep copy of this Term.
     */
    @SuppressWarnings("unchecked")
    // TODO LT: unclear why we get a warning. When calling clone() the compiler
    // seems to know the return is Term!?
    public <T extends Term> T cloneIt() {
        try {
            // This must always work since all children of Term are Cloneable!
            return (T) clone();
        } catch (CloneNotSupportedException e) {
            // There's no reason to end up here
            throw new InvalidTermException("Could not clone: " + e, e);
        }
    }

    @Override
    public abstract boolean equals(Object that);

    @Override
    public abstract int hashCode();

    /**
     * Delegate formatting to the {@link DefaultFormatter}.
     */
    @Override
    public String toString() {
        return accept(formatter);
    }

}
