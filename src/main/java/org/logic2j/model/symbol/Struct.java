/*
 * tuProlog - Copyright (C) 2001-2007 aliCE team at deis.unibo.it
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.logic2j.model.symbol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;

import org.logic2j.LibraryManager;
import org.logic2j.TermFactory.FactoryMode;
import org.logic2j.library.mgmt.LibraryContent;
import org.logic2j.model.InvalidTermException;
import org.logic2j.model.TermVisitor;
import org.logic2j.model.prim.PrimitiveInfo;
import org.logic2j.model.var.Binding;
import org.logic2j.model.var.VarBindings;
import org.logic2j.util.ReflectUtils;

/**
 * Struct class represents both compound prolog term
 * and atom term (considered as 0-arity compound).
 */
public class Struct extends Term {
  private static final long serialVersionUID = 1L;

  /**
   * Terminates a vararg predicate description: write/N
   */
  private static final String VARARG_PREDICATE_TRAILER = "/" + LibraryManager.VARARG_ARITY_INDICATOR;

  private static final TermApi TERM_API = new TermApi();

  // TODO Rove these constants to a common place?
  // TODO Replace all calls to intern() by some factory to initialize our constants
  public static final String FUNCTOR_COMMA = ",".intern();
  public static final String FUNCTOR_SEMICOLON = ";".intern();
  public static final String LIST_SEPARATOR = ",".intern(); // In notations [a,b,c]

  public static final String FUNCTOR_LIST = ".".intern();
  public static final String FUNCTOR_LIST_EMPTY = "[]".intern(); // The functor representing an empty list
  public static final String FUNCTOR_CLAUSE = ":-".intern();
  public static final String FUNCTOR_CLAUSE_QUOTED = ("'" + FUNCTOR_CLAUSE + "'").intern();

  public static final String FUNCTOR_TRUE = "true";
  public static final String FUNCTOR_FALSE = "false".intern();
  public static final String FUNCTOR_CUT = "!";
  public static final String FUNCTOR_CALL = "call";

  // true and false constants
  public static final Struct ATOM_FALSE = new Struct(FUNCTOR_FALSE);
  public static final Struct ATOM_TRUE = new Struct(FUNCTOR_TRUE);
  public static final Struct ATOM_CUT = new Struct(FUNCTOR_CUT);

  private String name; // Always "internalized" with String.intern(), you can compare with == !
  private int arity;
  private Term[] arg;

  private PrimitiveInfo primitiveInfo;

  /**
   * Builds a compound, with any number of arguments.
   */
  public Struct(String theFunctor, Object... argList) throws InvalidTermException {
    this(theFunctor, argList.length);
    int i = 0;
    for (Object element : argList) {
      this.arg[i++] = TERM_API.valueOf(element, FactoryMode.ANY_TERM);
    }
  }

  public Struct(String theFunctor, Term... argList) throws InvalidTermException {
    this(theFunctor, argList.length);
    int i = 0;
    for (Term element : argList) {
      if (element == null) {
        throw new InvalidTermException("Cannot create Term from with null argument");
      }
      this.arg[i++] = element;
    }
  }

  /**
   * Static factory (instead of constructor).
   * @return A structure representing an empty list
   */
  public static Struct createEmptyPList() {
    return new Struct(FUNCTOR_LIST_EMPTY, 0);
  }

  /**
   * Static factory (instead of constructor).
   * @return A prolog list providing head and tail
   */
  public static Struct createPList(Term h, Term t) {
    Struct result = new Struct(FUNCTOR_LIST, 2);
    result.arg[0] = h;
    result.arg[1] = t;
    return result;
  }

  /**
   * Static factory to create a Prolog List structure from a Java List.
   * @param theJavaList
   */
  public static Struct createPList(List<Term> theJavaList) {
    int size = theJavaList.size();
    if (size == 0) {
      return Struct.createEmptyPList();
    } 
    Struct pList;
    pList = Struct.createEmptyPList();
    for (int i = size - 1; i >= 0; i--) {
      pList = Struct.createPList(theJavaList.get(i), pList);
    }
    return pList;
  }

  /**
   * Builds a list specifying the elements
   */
  public Struct(Term[] argList) {
    this(argList, 0);
  }

  private Struct(Term[] argList, int theIndex) {
    this(FUNCTOR_LIST, 2);
    if (theIndex < argList.length) {
      this.arg[0] = argList[theIndex];
      this.arg[1] = new Struct(argList, theIndex + 1);
    } else {
      // build an empty list
      setNameAndArity(FUNCTOR_LIST_EMPTY, 0);
      this.arg = null;
    }
  }

  /**
   * Builds a compound, with a linked list of arguments
   */
  public Struct(String theFunctor, Collection<Term> elements) {
    int ary = elements.size();
    setNameAndArity(theFunctor, ary);
    this.arg = new Term[ary];
    int i = 0;
    for (Term element : elements) {
      this.arg[i++] = element;
    }
  }

  private Struct(String theFunctor, int theArity) {
    if (theFunctor == null) {
      throw new InvalidTermException("The functor of a Struct cannot be null");
    }
    if (theFunctor.length() == 0 && theArity > 0) {
      throw new InvalidTermException("The functor of a non-atom Struct cannot be an empty string");
    }
    setNameAndArity(theFunctor, theArity);
    if (this.arity > 0) {
      this.arg = new Term[this.arity];
    }
  }

  /**
   * Write major properties of the Struct, and also store read-only fields for efficient access.
   * @param theName
   * @param theArity
   */
  private void setNameAndArity(String theName, int theArity) {
    this.name = theName.intern();
    this.arity = theArity;
  }

  /**
   * Gets the number of elements of
   * this structure
   */
  public int getArity() {
    return this.arity;
  }

  /**
   * Gets the functor name  of this structure
   */
  public String getName() {
    return this.name;
  }

  /**
   * Gets the i-th element of this structure
   *
   * No bound check is done
   */
  public Term getArg(int theIndex) {
    return this.arg[theIndex];
  }

  /**
   * @return Left-hand-side term, this is, {@link #getArg(int)} at index 0.
   * It is assumed that the term MUST have an arity of 2, because when there's
   * a LHS, there's also a RHS!
   */
  public Term getLHS() {
    if (this.arity != 2) {
      throw new IllegalArgumentException("Can't get the left-hand-side argument of " + this + " (not a binary predicate)");
    }
    return this.arg[0];
  }

  /**
   * @return Right-hand-side term, this is, {@link #getArg(int)} at index 1.
   * It is assumed that the term MUST have an arity of 2
   */
  public Term getRHS() {
    if (this.arity != 2) {
      throw new IllegalArgumentException("Can't get the left-hand-side argument of " + this + " (not a binary predicate)");
    }
    return this.arg[1];
  }

  public String getPredicateIndicator() {
    return this.name + '/' + this.arity;
  }

  public String getVarargsPredicateIndicator() {
    return this.name + VARARG_PREDICATE_TRAILER;
  }

  /**
   * Sets the i-th element of this structure
   *
   * (Only for internal service)
   */
  @Deprecated
  public void setArg(int theIndex, Term argument) {
    this.arg[theIndex] = argument;
  }

  @Override
  public boolean isAtom() {
    return (this.arity == 0 || isEmptyList());
  }

  public PrimitiveInfo getPrimitiveInfo() {
    return this.primitiveInfo;
  }

  //

  /**
   * Gets a copy of this structure
   */
  @Override
  public Term cloneIt() {
    Struct t;
    try {
      t = (Struct) this.clone();
    } catch (CloneNotSupportedException e) {
      throw new InvalidTermException("Could not clone: " + e, e);
    }
    t.setNameAndArity(this.name, this.arity);
    t.arg = new Term[this.arity];
    t.primitiveInfo = this.primitiveInfo;
    for (int c = 0; c < this.arity; c++) {
      t.arg[c] = this.arg[c].cloneIt();
    }
    return t;
  }

  @Override
  protected void flattenTerms(Collection<Term> theFlatTerms) {
    this.index = NO_INDEX;
    for (int c = 0; c < this.arity; c++) {
      Term term = this.arg[c];
      term.flattenTerms(theFlatTerms);
    }
    theFlatTerms.add(this);
  }

  @Override
  protected Term compact(Collection<Term> theFlatTerms) {
    // Recursively compact all subelements
    Term[] newArgs = new Term[this.arity];
    boolean anyChange = false;
    for (int c = 0; c < this.arity; c++) {
      newArgs[c] = this.arg[c].compact(theFlatTerms);
      anyChange |= (newArgs[c] != this.arg[c]);
    }
    final Struct compacted;
    if (anyChange) {
      compacted = (Struct) this.cloneIt();
      compacted.arg = newArgs;
    } else {
      compacted = this;
    }
    // If this term already has an equivalent in the provided collection, return that one
    Term betterEquivalent = compacted.findStaticallyEqual(theFlatTerms);
    if (betterEquivalent != null) {
      return betterEquivalent;
    }
    return compacted;
  }

  @Override
  public Var findVar(String theVariableName) {
    for (int c = 0; c < this.arity; c++) {
      final Term term = this.arg[c];
      final Var found = term.findVar(theVariableName);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  /**
   * If any argument appears to have been cloned, then the complete structure will be cloned.
   */
  @Override
  protected Term substitute(VarBindings theBindings, IdentityHashMap<Binding, Var> theBindingsToVars) {
    Term[] substArgs = new Term[this.arity]; // Will all arguments after substitution
    boolean anyChange = false;
    for (int i = 0; i < this.arity; i++) {
      substArgs[i] = this.arg[i].substitute(theBindings, theBindingsToVars);
      anyChange |= (substArgs[i] != this.arg[i]);
    }
    final Struct substituted;
    if (anyChange) {
      // New cloned structure
      substituted = new Struct(getName(), substArgs);
    } else {
      // Original unchanged - same reference
      substituted = this;
    }
    return substituted;
  }

  @Override
  public boolean staticallyEquals(Term theOther) {
    if (theOther == this) {
      return true; // Same reference
    }
    if (!(theOther instanceof Struct)) {
      return false;
    }
    final Struct that = (Struct) theOther;
    if (this.arity == that.arity && this.name == that.name) {
      for (int c = 0; c < this.arity; c++) {
        if (!this.arg[c].staticallyEquals(that.arg[c])) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  protected short assignVarOffsets(short theIndexOfNextUnindexedVar) {
    if (this.index != NO_INDEX) {
      // Already assigned, do nothing and return same index since we did not assigned a new var
      return theIndexOfNextUnindexedVar;
    }

    short runningCounter = theIndexOfNextUnindexedVar;
    for (int c = 0; c < this.arity; c++) {
      runningCounter = this.arg[c].assignVarOffsets(runningCounter);
    }
    this.index = runningCounter;
    return runningCounter;
  }

  /**
   * @param theLib2Content
   */
  public void assignPrimitiveInfo(LibraryContent theLib2Content) {
    // Find by exact arity match
    this.primitiveInfo = theLib2Content.primitiveMap.get(getPredicateIndicator());
    if (this.primitiveInfo == null) {
      // Alternate find by wildcard (varargs signature)
      this.primitiveInfo = theLib2Content.primitiveMap.get(getVarargsPredicateIndicator());
    }
    for (int c = 0; c < this.arity; c++) {
      final Term sub = this.arg[c];
      if (sub instanceof Struct) {
        ((Struct) sub).assignPrimitiveInfo(theLib2Content);
      }
    }
  }

  public void avoidCycle(List<Term> visited) {
    for (Term term : visited) {
      if (term == this) {
        throw new IllegalStateException("Cycle detected");
      }
    }
    visited.add(this);
    for (Term term : this.arg) {
      if (term instanceof Struct) {
        ((Struct) term).avoidCycle(visited);
      }
    }
  }

  //---------------------------------------------------------------------------
  // Methods for Prolog list structures (named "PList" hereafter)
  //---------------------------------------------------------------------------

  /**
   * Is this structure an empty list?
   */
  public boolean isEmptyList() {
    return this.name.equals(FUNCTOR_LIST_EMPTY) && this.arity == 0;
  }

  @Override
  public boolean isList() {
    return (this.name.equals(FUNCTOR_LIST) && this.arity == 2 && this.arg[1].isList()) || isEmptyList();
  }

  protected void assertPList(Term thePList) {
    if (!thePList.isList()) {
      throw new UnsupportedOperationException("The structure \"" + thePList + "\" is not a Prolog list.");
    }
  }

  /**
   * Gets the head of this structure, which is supposed to be a list.
   * 
   * <p>
   * Gets the head of this structure, which is supposed to be a list.
   * If the callee structure is not a list, throws an <code>UnsupportedOperationException</code>
   * </p>
   */
  public Term listHead() {
    assertPList(this);
    return getLHS();
  }

  /**
   * Gets the tail of this structure, which is supposed to be a list.
   * 
   * <p>
   * Gets the tail of this structure, which is supposed to be a list.
   * If the callee structure is not a list, throws an <code>UnsupportedOperationException</code>
   * </p>
   */
  public Struct listTail() {
    assertPList(this);
    return (Struct) getRHS();
  }

  /**
   * Gets the number of elements of this structure, which is supposed to be a list.
   * 
   * <p>
   * Gets the number of elements of this structure, which is supposed to be a list.
   * If the callee structure is not a list, throws an <code>UnsupportedOperationException</code>
   * </p>
   */
  public int listSize() {
    assertPList(this);
    Struct t = this;
    int count = 0;
    while (!t.isEmptyList()) {
      count++;
      t = (Struct) getRHS();
    }
    return count;
  }

  //  /**
  //   * Gets a list Struct representation, with the functor as first element.
  //   */
  //  public Struct toList() {
  //    Struct t = Struct.createEmptyPList();
  //    for (int c = this.arity - 1; c >= 0; c--) {
  //      t = Struct.createPList(this.arg[c], t);
  //    }
  //    return new Struct(new Struct(this.name), t);
  //  }

  /**
   * From a Prolog List, obtain a Struct with the first list element as functor,
   * and all other elements as arguments.
   * This returns a(b,c,d) form [a,b,c,d]. This is the =.. predicate. 
   *
   * If this structure is not a list, null object is returned
   */
  // TODO Clarify how it works
  public Struct predicateFromPList() {
    assertPList(this);
    final Term functor = getLHS();
    if (!functor.isAtom()) {
      return null;
    }
    Struct runningElement = (Struct) getRHS();
    final ArrayList<Term> elements = new ArrayList<Term>();
    while (!runningElement.isEmptyList()) {
      if (!runningElement.isList()) {
        return null;
      }
      elements.add(runningElement.getLHS());
      runningElement = (Struct) runningElement.getRHS();
    }
    return new Struct(((Struct) functor).name, elements);
  }

  @SuppressWarnings("unchecked")
  public <Q extends Term, T extends Collection<Q>> T javaListFromPList(T theCollectionToFillOrNull, Class<Q> theElementClassOrNull) {
    if (theElementClassOrNull == null) {
      theElementClassOrNull = (Class<Q>) Term.class;
    }
    final T result;
    if (theCollectionToFillOrNull == null) {
      result = (T) new ArrayList<Q>();
    } else {
      result = theCollectionToFillOrNull;
    }
    Struct runningElement = this;
    int idx = 0;
    while (!runningElement.isEmptyList()) {
      assertPList(runningElement);
      final Q term = ReflectUtils.safeCastNotNull("obtaining element " + idx + " of PList " + this, runningElement.getLHS(),
          theElementClassOrNull);
      result.add(term);
      runningElement = (Struct) runningElement.getRHS();
      idx++;
    }
    return result;
  }

  /**
   * Appends an element to this structure (supposed to be a list)
   */
  public void append(Term t) {
    assertPList(this);
    if (isEmptyList()) {
      setNameAndArity(FUNCTOR_LIST, 2);
      this.arg = new Term[this.arity];
      this.arg[0] = t;
      this.arg[1] = Struct.createEmptyPList();
    } else if (this.arg[1].isList()) {
      ((Struct) this.arg[1]).append(t);
    } else {
      this.arg[1] = t;
    }
  }

  /**
   * Inserts (at the head) an element to this structure (supposed to be a list)
   */
  void insert(Term t) {
    assertPList(this);
    Struct co = Struct.createEmptyPList();
    co.arg[0] = getLHS();
    co.arg[1] = getRHS();
    this.arg[0] = t;
    this.arg[1] = co;
  }

  @Override
  public <T> T accept(TermVisitor<T> theVisitor) {
    return theVisitor.visit(this);
  }

  /**
   * Base requirement to unify 2 structures: matching names and arities.
   * @param that
   * @return True if this and that Struct have the same name and arity.
   */
  public boolean nameAndArityMatch(Struct that) {
    return this.arity==that.arity && this.name==that.name;
  }
  //---------------------------------------------------------------------------
  // Core
  //---------------------------------------------------------------------------

  /**
   * Test if a term is equal to other
   */
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof Struct)) {
      return false;
    }
    final Struct that = (Struct) other;
    if (!(this.arity == that.arity && this.name == that.name)) {
      return false;
    }
    for (int c = 0; c < this.arity; c++) {
      if (!this.arg[c].equals(that.arg[c])) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = this.name.hashCode();
    result ^= this.arity << 8;
    for (int c = 0; c < this.arity; c++) {
      result ^= this.arg[c].hashCode();
    }
    return result;
  }

}
