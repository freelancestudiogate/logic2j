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
package org.logic2j.core.library.mgmt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;

import org.logic2j.core.api.PLibrary;
import org.logic2j.core.api.SolutionListener;
import org.logic2j.core.api.model.exception.InvalidTermException;
import org.logic2j.core.api.model.exception.RecursionException;
import org.logic2j.core.api.model.symbol.Struct;
import org.logic2j.core.api.model.var.TermBindings;

/**
 * Describe a {@link Primitive}, which is either a:
 * <ul>
 * <li>Directive</li>
 * <li>Predicate</li>
 * <li>Functor</li>
 * </ul>
 * 
 * @note Strangely, this class has ivoke() features so it's not only a description!
 */
public class PrimitiveInfo {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PrimitiveInfo.class);
    private static final boolean isDebug = logger.isDebugEnabled();

    private static final HashSet<String> methodNeedingReflectiveInvocation = new HashSet<String>();

    public static enum PrimitiveType {
        /**
         * Not yet implemented.
         */
        DIRECTIVE,
        /**
         * A predicate implemented as a Java method, will produce solution(s) or not via the {@link SolutionListener} interface.
         */
        PREDICATE,
        /**
         * A functor yields a result, such as +(2,3).
         */
        FUNCTOR
    }

    private final PrimitiveType type;
    private final String name;
    private final String methodName;
    private final PLibrary library; // The library instance on which the method will be invoked (they are not static methods)
    private final Method method; // The method that implements the primitive's logic
    private final boolean isVarargs;

    public PrimitiveInfo(PrimitiveType theType, PLibrary theLibrary, String theName, Method theMethod, boolean theVarargs) {
        super();
        this.type = theType;
        this.library = theLibrary;
        this.name = theName;
        this.method = theMethod;
        this.methodName = theMethod.getName().intern();
        this.isVarargs = theVarargs;
    }

    public Object invoke(Struct theGoalStruct, TermBindings theTermBindings, SolutionListener theListener) {
        final Object result = this.library.dispatch(this.methodName, theGoalStruct, theTermBindings, theListener);
        if (result != PLibrary.NO_DIRECT_INVOCATION_USE_REFLECTION) {
            return result;
        }
        // We did not do a direct invocation - we will have to rely on reflection
        if (!methodNeedingReflectiveInvocation.contains(this.methodName)) {
            logger.warn("Invocation of library primitive \"{}\" uses reflection - consider implementing {}.dispatch()", this.methodName, this.library);
            // Avoid multiple logging
            methodNeedingReflectiveInvocation.add(this.methodName);
        }
        try {
            if (isDebug) {
                logger.debug("PRIMITIVE > invocation of {}", this);
            }
            return invokeReflective(theGoalStruct, theTermBindings, theListener);
        } catch (final IllegalArgumentException e) {
            throw e;
        } catch (final IllegalAccessException e) {
            throw new InvalidTermException("Could not access method " + this.method, e);
        } catch (final InvocationTargetException e) {
            final Throwable targetException = e.getTargetException();
            if (targetException instanceof RecursionException) {
                // If we already have trouble in recursivity, don't add further exceptions - just rethrow.
                throw (RecursionException) targetException;
            }
            if (targetException instanceof StackOverflowError) {
                final RecursionException recursionException = new RecursionException("Stack overflow while executing primitive " + this);
                recursionException.stackOverflow = true;
                throw recursionException;
            }
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            }
            throw new InvalidTermException("Primitive threw an exception: " + this + ": " + targetException, targetException);
        } catch (final Throwable e) {
            throw new InvalidTermException("Invocation of method " + this.method + " went really bad: " + e, e);
        }
    }

    /**
     * @param theGoalStruct
     * @param theTermBindings
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    private Object invokeReflective(Struct theGoalStruct, TermBindings theTermBindings, SolutionListener theListener) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        final int arity = theGoalStruct.getArity();
        final int nbargs = this.isVarargs ? 3 : (2 + arity);
        final Object[] args = new Object[nbargs];
        int i = 0;
        args[i++] = theListener;
        args[i++] = theTermBindings;
        if (this.isVarargs) {
            // All arguments as an array
            final Object[] varargArray = new Object[arity];
            int j = 0;
            while (j < arity) {
                varargArray[j] = theGoalStruct.getArg(j);
                j++;
            }
            args[i++] = varargArray;
        } else {
            // Regular argument passing
            int j = 0;
            while (j < arity) {
                args[i++] = theGoalStruct.getArg(j++);
            }
        }
        final Object result = this.method.invoke(this.library, args);
        if (isDebug) {
            logger.debug("PRIMITIVE < result={}", result);
        }
        return result;

    }

    // ---------------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------------

    public PrimitiveType getType() {
        return this.type;
    }

    // ---------------------------------------------------------------------------
    // Methods of java.lang.Object
    // ---------------------------------------------------------------------------

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{lib=" + this.library + ", type=" + getType() + ", name=" + this.name + ", method=" + this.method.getName() + '}';
    }

}
