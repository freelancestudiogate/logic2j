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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.logic2j.core.PrologTestBase;
import org.logic2j.core.api.model.var.TermBindings;

public class DefaultTermMarshallerTest extends PrologTestBase {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultTermMarshallerTest.class);

    private static final String REFERENCE_EXPRESSION = "a,b;c,d;e";
    private static final String EXPECTED_TOSTRING = "';'(','(a, b), ';'(','(c, d), e))";

    @Test
    public void simpleToString() {
        Object term = unmarshall(REFERENCE_EXPRESSION);
        String formatted = term.toString();
        logger.info("toString: {}", formatted);
        assertEquals(EXPECTED_TOSTRING, formatted);
    }

    @Test
    public void defaultMarshallerUninitialized() {
        Object term = unmarshall(REFERENCE_EXPRESSION);
        CharSequence formatted = new DefaultTermMarshaller().marshall(term);
        logger.info("uninitialized marshaller: {}", formatted);
        assertEquals(EXPECTED_TOSTRING, formatted.toString());
    }

    @Test
    public void defaultMarshaller() {
        Object term = unmarshall(REFERENCE_EXPRESSION);
        CharSequence formatted = getProlog().getTermMarshaller().marshall(term);
        logger.info("prolog initialized marshaller: {}", formatted);
        assertEquals("a , b ; c , d ; e", formatted);
    }

    @Test
    public void xBoundToY() {
        Object term = unmarshall("f(X, Y)");
        TermBindings bindings = new TermBindings(term);
        bindings.getBinding(0).bindTo(bindings.getBinding(1).getReferrer(), bindings);
        CharSequence formatted = getProlog().getTermMarshaller().marshall(term);
        assertEquals("f(X, Y)", formatted);
    }

    @Test
    public void yBoundToX() {
        Object term = unmarshall("f(X, Y)");
        TermBindings bindings = new TermBindings(term);
        bindings.getBinding(1).bindTo(bindings.getBinding(0).getReferrer(), bindings);
        CharSequence formatted = getProlog().getTermMarshaller().marshall(term);
        assertEquals("f(X, Y)", formatted);
    }

}
