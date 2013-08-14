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
package org.logic2j.core.unify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.model.symbol.TermApi;
import org.logic2j.core.model.var.Bindings;
import org.logic2j.core.model.var.Bindings.FreeVarRepresentation;
import org.logic2j.core.solver.GoalFrame;

/**
 * Support the thorough testing of {@link Unifier} implementations, this is not a TestCase.
 * TODO replace this class by hamctest 1.3 high-level assertions that can be reused
 */
class UnificationTester {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UnificationTester.class);

  private static final TermApi TERM_API = new TermApi();

  private final Unifier unifier;

  public Term left;
  public Term right;
  public Bindings leftVars;
  public Bindings rightVars;
  private GoalFrame frame;
  private Boolean expectedResult = null; // TODO Good candidates for hamctest 1.3
  private Boolean result = null; // TODO Good candidates for hamctest 1.3

  private Integer expectedNbBindings = null;

  /**
   * @param theUnifier Implementation to test
   */
  public UnificationTester(Unifier theUnifier) {
    this.unifier = theUnifier;
  }

  /**
   * Every term has its own {@link Bindings}.
   * @param theLeft
   * @param theRight
   */
  private void init2(Term theLeft, Term theRight) {
    this.left = TERM_API.normalize(theLeft, null);
    this.right = TERM_API.normalize(theRight, null);
    this.leftVars = new Bindings(this.left);
    this.rightVars = new Bindings(this.right);
    this.frame = new GoalFrame();
  }

  /**
   * Share same {@link Bindings} for both terms.
   * @param theLeft
   * @param theRight
   * @param bindings
   */
  private void init1(Term theLeft, Term theRight, Bindings bindings) {
    this.left = TERM_API.normalize(theLeft, null);
    this.right = TERM_API.normalize(theRight, null);
    this.leftVars = bindings;
    this.rightVars = bindings;
    this.frame = new GoalFrame();
  }

  /**
   * Execute the unification and do some state checking if expected results have been defined.
   * @return The unification result
   */
  private boolean unifyLR(StringBuilder theSignature) {
    logger.info("Unifying {} to {}", this.left, this.right);
    boolean unified = this.unifier.unify(this.left, this.leftVars, this.right, this.rightVars, this.frame);
    logger.debug(" result={}, trailFrame={}", unified, this.frame);
    logger.debug(" leftVars={}", this.leftVars);
    logger.debug(" rightVars={}", this.rightVars);
    logger.debug(" left={}   bindings={}", TERM_API.substitute(this.left, this.leftVars, null),
        this.leftVars.explicitBindings(FreeVarRepresentation.SKIPPED));
    logger.debug(" right={}  bindings={}", TERM_API.substitute(this.right, this.rightVars, null),
        this.rightVars.explicitBindings(FreeVarRepresentation.SKIPPED));
    appendSignature(theSignature, this.leftVars, this.rightVars, this.frame);
    this.result = unified;
    if (this.expectedResult != null) {
      assertUnificationResult(unified);
    }
    if (unified) {
      if (this.expectedNbBindings != null) {
        assertNbBindings(this.expectedNbBindings);
      }
    } else {
      assertNbBindings(0);
    }
    return unified;
  }

  private boolean unifyRL(StringBuilder theSignature) {
    logger.info("Unifying {} to {}", this.right, this.left);
    boolean unified = this.unifier.unify(this.right, this.rightVars, this.left, this.leftVars, this.frame);
    logger.debug(" result={}, trailFrame={}", unified, this.frame);
    logger.debug(" left={}   bindings={}", TERM_API.substitute(this.left, this.leftVars, null),
        this.leftVars.explicitBindings(FreeVarRepresentation.SKIPPED));
    logger.debug(" right={}  bindings={}", TERM_API.substitute(this.right, this.rightVars, null),
        this.rightVars.explicitBindings(FreeVarRepresentation.SKIPPED));
    appendSignature(theSignature, this.leftVars, this.rightVars, this.frame);
    this.result = unified;
    if (this.expectedResult != null) {
      assertUnificationResult(unified);
    }
    if (unified) {
      if (this.expectedNbBindings != null) {
        assertNbBindings(this.expectedNbBindings);
      }
    } else {
      assertNbBindings(0);
    }
    return unified;
  }

  public void unify2ways(Term theLeft, Term theRight) {
    init2(theLeft, theRight);
    StringBuilder signatureLR = new StringBuilder();
    boolean unifyLR = unifyLR(signatureLR);
    if (unifyLR) {
      deunify();
    }
    StringBuilder signatureRL = new StringBuilder();
    boolean unifyRL = unifyRL(signatureRL);
    if (unifyRL) {
      deunify();
    }
    assertEquals("Same unification results between LR and RL", unifyLR, unifyRL);
    assertEquals("Same post-unification state signatures", signatureLR.toString(), signatureRL.toString());
  }

  public void unify2ways(Term theLeft, Term theRight, Bindings bindings) {
    init1(theLeft, theRight, bindings);
    StringBuilder signatureLR = new StringBuilder();
    boolean unifyLR = unifyLR(signatureLR);
    if (unifyLR) {
      deunify();
    }
    StringBuilder signatureRL = new StringBuilder();
    boolean unifyRL = unifyRL(signatureRL);
    if (unifyRL) {
      deunify();
    }
    assertEquals("Same unification results between LR and RL", unifyLR, unifyRL);
    assertEquals("Same post-unification state signatures", signatureLR.toString(), signatureRL.toString());
  }

  /**
   * @param theSignature
   * @param theBindings1
   * @param theBindings2
   * @param theFrame
   */
  private void appendSignature(StringBuilder theSignature, Bindings theBindings1, Bindings theBindings2, GoalFrame theFrame) {
    theSignature.append(theBindings1.toString());
    theSignature.append("  ");
    theSignature.append(theBindings2.toString());
    theSignature.append("  ");
    theSignature.append(theFrame.toString());
  }

  /**
   * Deunify and do some state checking.
   */
  private void deunify() {
    this.unifier.deunify(this.frame);
    logger.debug("Deunify, trailFrame={}", this.frame);
    // No more bindings expected
    assertNbBindings(0);
  }

  /**
   * Assert on expected unification result.
   * @param theExpectedResult
   */
  private void assertUnificationResult(boolean theExpectedResult) {
    assertEquals("unification result", theExpectedResult, this.result.booleanValue());
  }

  /**
   * @param theExpectedNbBindings 
   */
  @SuppressWarnings("deprecation")
  private void assertNbBindings(int theExpectedNbBindings) {
    assertNotNull(this.frame);
    assertEquals("Number of var bindings", theExpectedNbBindings, this.frame.nbBindings());
  }

  //---------------------------------------------------------------------------
  // Accessors
  //---------------------------------------------------------------------------

  public void setExpectedUnificationResult(boolean theExpectedResult) {
    this.expectedResult = theExpectedResult;
  }

  /**
   * @param theExpectedNbBindings TNumber of effective bindings created by unification, and remembered in the
   * trailing bindings to be deunified.
   */
  public void setExpectedNbBindings(int theExpectedNbBindings) {
    this.expectedNbBindings = theExpectedNbBindings;
  }

}
