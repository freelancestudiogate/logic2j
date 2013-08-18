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
package org.logic2j.core.impl;

import org.logic2j.core.Formatter;
import org.logic2j.core.LibraryManager;
import org.logic2j.core.Prolog;
import org.logic2j.core.io.operator.OperatorManager;
import org.logic2j.core.model.symbol.Term;
import org.logic2j.core.solver.Solver;
import org.logic2j.core.unify.Unifier;

/**
 * An interface that Prolog implementations must provide; this goes beyond the lighter facade interface {@link Prolog} intended for client
 * use. This one exposes accessors to the internal state of the effective implementation.
 */
public interface PrologImplementation extends Prolog {

    // ---------------------------------------------------------------------------
    // Accessors to the sub-features of the Prolog engine
    // ---------------------------------------------------------------------------

    /**
     * @return The implementation for managing libraries.
     */
    LibraryManager getLibraryManager();

    /**
     * @return The implementation of unification logic.
     */
    Unifier getUnifier();

    /**
     * @return The implementation of inference logic.
     */
    Solver getSolver();

    /**
     * @return The implementation for managing operators.
     */
    OperatorManager getOperatorManager();

    /**
     * @return The implementation for marshalling {@link Term} hierarchies to various formats.
     */
    Formatter getFormatter();

}