/*
 * Copyright 2020 Rod MacKenzie
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rodm.teamcity.internal

import com.github.rodm.teamcity.ValidationMode
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Task

import static ValidationSupport.createXmlParser
import static com.github.rodm.teamcity.ValidationMode.FAIL
import static com.github.rodm.teamcity.ValidationMode.IGNORE

class PluginDefinitionValidationAction implements Action<Task> {

    static final String NO_BEAN_CLASS_WARNING_MESSAGE = "%s: Plugin definition file %s defines a bean but the implementation class %s was not found in the jar."
    static final String NO_BEAN_CLASSES_WARNING_MESSAGE = "%s: Plugin definition file %s contains no beans."
    static final String NO_BEAN_CLASSES_NON_PARSED_WARNING_MESSAGE = "%s: Failed to parse plugin definition file %s: %s"
    static final String NO_DEFINITION_WARNING_MESSAGE = "%s: No valid plugin definition files were found in META-INF"

    private final ValidationMode mode
    private final List<PluginDefinition> definitions
    private final Set<String> classes
    private boolean warningShown

    PluginDefinitionValidationAction(ValidationMode mode, List < PluginDefinition > definitions, Set<String> classes) {
        this.mode = mode
        this.definitions = definitions
        this.classes = classes
        this.warningShown = false
    }

    @Override
    void execute(Task task) {
        if (mode == IGNORE) {
            return
        }
        if (definitions.isEmpty()) {
            report(task, String.format(NO_DEFINITION_WARNING_MESSAGE, task.getPath()))
        } else {
            for (PluginDefinition definition : definitions) {
                validateDefinition(definition, task)
            }
        }
        if (mode == FAIL && warningShown) {
            throw new GradleException('Plugin definition validation failed')
        }
    }

    private void validateDefinition(PluginDefinition definition, Task task) {
        boolean offline = task.project.gradle.startParameter.isOffline()
        List<PluginBean> beans
        try {
            beans = definition.getBeans(offline)
        }
        catch (IOException e) {
            report(task, String.format(NO_BEAN_CLASSES_NON_PARSED_WARNING_MESSAGE, task.getPath(), definition.name, e.message), e)
            return
        }
        if (beans.isEmpty()) {
            report(task, String.format(NO_BEAN_CLASSES_WARNING_MESSAGE, task.getPath(), definition.name))
        } else {
            for (PluginBean bean : beans) {
                def fqcn = bean.className.replaceAll('\\.', '/') + '.class'
                if (!classes.contains(fqcn)) {
                    report(task, String.format(NO_BEAN_CLASS_WARNING_MESSAGE, task.getPath(), definition.name, bean.className))
                }
            }
        }
    }

    private void report(Task task, String message, Object... objects) {
        task.logger.warn(message, objects)
        warningShown = true
    }
}

class PluginDefinition {

    private File definitionFile

    PluginDefinition(File file) {
        this.definitionFile = file
    }

    String getName() {
        return this.definitionFile.name
    }

    def getBeans(boolean offline) {
        List<PluginBean> pluginBeans = []
        def parser = createXmlParser(offline)
        def beans = parser.parse(definitionFile)
        beans.bean.each { bean ->
            pluginBeans << new PluginBean(id: bean.attribute('id'), className: bean.attribute('class'))
        }
        return pluginBeans
    }
}

class PluginBean {
    String id
    String className
}
