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
package org.logic2j.core;

import org.logic2j.core.model.TermVisitor;
import org.logic2j.core.model.symbol.Term;

/**
 * Convert Prolog {@link Term} hierarchies to {@link String}s or other streamable representations.
 * We are using the Visitor design pattern, so a {@link Formatter} is actually a {@link TermVisitor} for Strings.
 * TODO: unless this interface is needed in root package, move this in subpackage
 */
public interface Formatter extends TermVisitor<String> {

  public String format(Term theTerm);

}
