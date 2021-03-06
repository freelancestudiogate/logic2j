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
package org.logic2j.core.api.solver.listener;

import org.logic2j.core.api.SolutionListener;
import org.logic2j.core.api.model.Continuation;

/**
 * A base implementation of {@link SolutionListener} that holds a counter of the number of solutions reached. The {@link #onSolution()}
 * method always returns true (dangerously allowing for potential infinite generation). Derive from this class to ease the programming of
 * {@link SolutionListener}s in application code.
 */
public class SolutionListenerBase implements SolutionListener {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SolutionListenerBase.class);

    private static final boolean DEBUG_ENABLED = logger.isDebugEnabled();

    /**
     * Number of solutions (so far).
     * TODO Should it deserve a "long"?
     */
    private int counter = 0;

    @Override
    public Continuation onSolution() {
        if (DEBUG_ENABLED) {
            logger.debug(" onSolution(#{})", this.counter);
        }
        this.counter++;
        return Continuation.CONTINUE;
    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    public int getCounter() {
        return this.counter;
    }

}
