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
package org.logic2j.core.api.model.symbol;

import org.logic2j.core.api.model.TermVisitor;

/**
 * TDouble class represents the double Prolog data type.
 */
public final class TDouble extends TNumber {
    private static final long serialVersionUID = 1L;

    private final double value;

    public TDouble(double v) {
        this.value = v;
    }

    public TDouble(float v) {
        this.value = v;
    }

    @Override
    public Number toNumber() {
        return this.value;
    }

    @Override
    final public double doubleValue() {
        return this.value;
    }

    @Override
    final public long longValue() {
        return (long) this.value;
    }

    // ---------------------------------------------------------------------------
    // Template methods defined in abstract class Term
    // ---------------------------------------------------------------------------

    <T> T accept(TermVisitor<T> theVisitor) {
        return theVisitor.visit(this);
    }

    // ---------------------------------------------------------------------------
    // Methods of java.lang.Object
    // ---------------------------------------------------------------------------

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TDouble)) {
            return false;
        }
        final TDouble that = (TDouble) other;
        return this.value == that.value;
    }

    @Override
    public int hashCode() {
        return Double.valueOf(this.value).hashCode();
    }

    // ---------------------------------------------------------------------------
    // Interface Comparable
    // ---------------------------------------------------------------------------

    @Override
    public int compareTo(TNumber that) {
        return Double.valueOf(this.value).compareTo(that.doubleValue());
    }
}
