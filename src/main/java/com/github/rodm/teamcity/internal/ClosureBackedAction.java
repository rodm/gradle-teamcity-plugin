/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rodm.teamcity.internal;

import groovy.lang.Closure;
import groovy.lang.MissingMethodException;
import org.gradle.api.Action;
import org.gradle.api.InvalidActionClosureException;
import org.gradle.util.Configurable;

import java.util.Objects;

/**
 * Copy of deprecated org.gradle.util.ClosureBackedAction
 */
@SuppressWarnings("rawtypes")
public class ClosureBackedAction<T> implements Action<T> {

    private final Closure closure;
    private final int resolveStrategy;
    private final boolean configurableAware;

    public static <T> ClosureBackedAction<T> of(Closure<?> closure) {
        return new ClosureBackedAction<>(closure);
    }

    public ClosureBackedAction(Closure closure) {
        this(closure, Closure.DELEGATE_FIRST, true);
    }

    public ClosureBackedAction(Closure closure, int resolveStrategy, boolean configurableAware) {
        this.closure = closure;
        this.resolveStrategy = resolveStrategy;
        this.configurableAware = configurableAware;
    }

    @Override
    public void execute(T delegate) {
        if (closure == null) {
            return;
        }

        try {
            if (configurableAware && delegate instanceof Configurable) {
                ((Configurable) delegate).configure(closure);
            } else {
                Closure copy = (Closure) closure.clone();
                copy.setResolveStrategy(resolveStrategy);
                copy.setDelegate(delegate);
                if (copy.getMaximumNumberOfParameters() == 0) {
                    copy.call();
                } else {
                    copy.call(delegate);
                }
            }
        }
        catch (MissingMethodException e) {
            if (Objects.equals(e.getType(), closure.getClass()) && Objects.equals(e.getMethod(), "doCall")) {
                throw new InvalidActionClosureException(closure, delegate);
            }
            throw e;
        }
    }
}
