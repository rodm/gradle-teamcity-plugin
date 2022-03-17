/*
 * Copyright 2017 Rod MacKenzie
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rodm.teamcity

import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskContainer
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeDiagnosingMatcher

class GradleMatchers {

    static Matcher<Configuration> hasDependency(String group, String name, String version) {
        return new HasDependency(group, name, version)
    }

    static Matcher<Configuration> hasDefaultDependency(String group, String name, String version) {
        return new HasDefaultDependency(group, name, version)
    }

    static Matcher<TaskContainer> hasTask(String name) {
        return new TypeSafeDiagnosingMatcher<TaskContainer>() {
            @Override
            void describeTo(final Description description) {
                description.appendText("TaskContainer should contain task ").appendValue(name)
            }

            @Override
            protected boolean matchesSafely(final TaskContainer item, final Description mismatchDescription) {
                mismatchDescription.appendText(" was ").appendValue(item)
                return item.findByName(name)
            }
        }
    }

    static Matcher<Task> hasAction(Class<?> type) {
        return new TypeSafeDiagnosingMatcher<Task>() {
            @Override
            protected boolean matchesSafely(final Task task, Description mismatchDescription) {
                return task.taskActions
                    .collect { it.action.getClass().name }
                    .contains(type.name)
            }

            @Override
            void describeTo(Description description) {
                description.appendText("Task should have an action of type ").appendValue(type.simpleName)
            }
        }
    }

    static class HasDependency extends TypeSafeDiagnosingMatcher<Configuration> {

        private String group
        private String name
        private String version

        HasDependency(String group, String name, String version) {
            this.group = group
            this.name = name
            this.version = version
        }

        @Override
        void describeTo(Description description) {
            String dependency = "${group}:${name}:${version}"
            description.appendText("Configuration should contain dependency ").appendValue(dependency)
        }

        @Override
        protected boolean matchesSafely(Configuration item, Description mismatchDescription) {
            List<String> dependencies = getDependencies(item)
            mismatchDescription.appendText(" was ").appendValue(dependencies)
            return dependencies.contains("${group}:${name}:${version}".toString())
        }

        List<String> getDependencies(Configuration configuration) {
            configuration.dependencies.collect {dependency ->
                "${dependency.group}:${dependency.name}:${dependency.version}".toString()
            }
        }
    }

    static class HasDefaultDependency extends HasDependency {
        HasDefaultDependency(String group, String name, String version) {
            super(group, name, version)
        }

        List<String> getDependencies(Configuration configuration) {
            configuration.incoming.dependencies.collect {dependency ->
                "${dependency.group}:${dependency.name}:${dependency.version}".toString()
            }
        }
    }
}
