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

import java.util.Collection;
import java.util.IdentityHashMap;

import org.logic2j.core.model.TermVisitor;
import org.logic2j.core.model.exception.InvalidTermException;
import org.logic2j.core.model.var.Binding;
import org.logic2j.core.model.var.Bindings;

/**
 * This class represents a variable term.
 * Variables are identified by a name (which must starts with
 * an upper case letter) or the anonymous ('_') name.
 */
public class Var extends Term {
  private static final long serialVersionUID = 1L;

  public static final String ANONYMOUS_VAR_NAME = "_".intern(); // TODO Document this
  public static final Term ANONYMOUS_VAR = new Var();

  /**
   * The name of the variable, usually starting with uppercase when this Var
   * was instantiated by the default parser, but when instantiated by {@link #Var(String)}
   * it may actually be anything (although it may not be the smartest idea).<br/>
   * A value of null means it's the anonymous variable (even though the anonymous
   * variable is formatted as "_")<br/>
   * Note: all variable names are internalized, i.e. it is legal to compare them with ==.
   */
  private String name;

  /**
   * Creates a variable identified by a name.
   *
   * The name must starts with an upper case letter or the underscore. If an underscore is
   * specified as a name, the variable is anonymous.
   *
   * @param theName is the name
   * @throws InvalidTermException if n is not a valid Prolog variable name
   */
  public Var(String theName) {
    this.name = theName.intern();
  }

  /**
   * Create an anonymous variable.
   */
  private Var() {
    this.name = ANONYMOUS_VAR_NAME;
  }

  /**
   * Gets the name of the variable.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Tests if this variable is anonymous.
   */
  public boolean isAnonymous() {
    return this.name == ANONYMOUS_VAR_NAME;
  }

  /**
   * Obtain the current {@link Binding} of this Var from the {@link Bindings}.
   * Notice that the variable index must have been assigned, and this var must NOT
   * be the anonymous variable (that cannot be bound to anything).
   * @param theBindings
   * @return The current binding of this Var.
   */
  public Binding bindingWithin(Bindings theBindings) {
    if (this.index < 0) {
      // An error situation
      if (this.index == NO_INDEX) {
        throw new IllegalStateException("Cannot dereference variable whose offset was not initialized");
      }
      if (this.index == ANON_INDEX) {
        throw new IllegalStateException("Cannot dereference the anonymous variable");
      }
    }
    if (this.index >= theBindings.getSize()) {
      throw new IllegalStateException("Bindings " + theBindings + " has space for " + theBindings.getSize()
          + " bindings, trying to dereference " + this + " at index " + this.index);
    }
    return theBindings.getBinding(this.index);
  }


  
  //---------------------------------------------------------------------------
  // Template methods defined in abstract class Term
  //---------------------------------------------------------------------------

  @Override
  public boolean isAtom() {
    return false;
  }

  @Override
  public boolean isList() {
    return false;
  }

  @Override
  public Var findVar(String theVariableName) {
    if (ANONYMOUS_VAR_NAME.equals(theVariableName)) {
      throw new IllegalArgumentException("Cannot find the anonymous variable");
    }
    if (theVariableName.equals(getName())) {
      return this;
    }
    return null;
  }

  @Override
  protected Term substitute(Bindings theBindings, IdentityHashMap<Binding, Var> theBindingsToVars) {
    if (isAnonymous()) {
      // Anonymous variable is never bound - won't substitute
      return this;
    }
    final Binding binding = bindingWithin(theBindings).followLinks();
    switch (binding.getType()) {
      case LIT:
        // For a literal, we have a reference to the literal term and to its own variables,
        // so recurse further
        return binding.getTerm().substitute(binding.getLiteralBindings(), theBindingsToVars);
      case FREE:
        // Free variable has no value, so substitution ends up on the last 
        // variable of the chain
        if (theBindingsToVars != null) {
          final Var originVar = theBindingsToVars.get(binding);
          if (originVar != null) {
            return originVar;
          }
          return ANONYMOUS_VAR;
        }
        // Return the free variable
        return this;
      default:
        // In case of LINK: that's impossible since we have followed the complete linked chain
        throw new IllegalStateException("substitute() internal error");
    }
  }

  @Override
  protected void flattenTerms(Collection<Term> theFlatTerms) {
    this.index = NO_INDEX;
    theFlatTerms.add(this);
  }

  @Override
  protected Term compact(Collection<Term> theFlatTerms) {
    // If this term already has an equivalent in the provided collection, return that one
    final Term alreadyThere = findStaticallyEqual(theFlatTerms);
    if (alreadyThere != null) {
      return alreadyThere;
    }
    for (Term term : theFlatTerms) {
      if (term instanceof Var) {
        final Var var = (Var) term;
        if (this.getName().equals(var.getName())) {
          return var;
        }
      }
    }
    return this;
  }

  @Override
  public boolean staticallyEquals(Term theOther) {
    if (theOther == this) {
      return true; // Same reference
    }
    return false;
  }

  @Override
  protected short assignVarOffsets(short theIndexOfNextUnindexedVar) {
    if (this.index != NO_INDEX) {
      // Already assigned, do nothing and return same index since we did not assign a new var
      return theIndexOfNextUnindexedVar;
    }
    if (isAnonymous()) {
      // Anonymous variable is not a var, don't count it, but assign an index that 
      // is different from NO_INDEX but that won't be ever used
      this.index = ANON_INDEX;
      return theIndexOfNextUnindexedVar;
    }
    // Index this var
    this.index = theIndexOfNextUnindexedVar;
    return ++theIndexOfNextUnindexedVar;
  }

  @Override
  public <T> T accept(TermVisitor<T> theVisitor) {
    return theVisitor.visit(this);
  }

  //---------------------------------------------------------------------------
  // Core java.lang.Object methods
  //---------------------------------------------------------------------------

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Var)) {
      return false;
    }
    final Var that = (Var) other;
    return this.name == that.name;
  }

  @Override
  public int hashCode() {
    if (this.name == null) {
      // Anonymous var
      return 0;
    }
    return this.name.hashCode();
  }

}
