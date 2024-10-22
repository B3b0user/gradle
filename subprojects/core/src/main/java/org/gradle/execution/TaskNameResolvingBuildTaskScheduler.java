/*
 * Copyright 2010 the original author or authors.
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
package org.gradle.execution;

import org.gradle.TaskExecutionRequest;
import org.gradle.api.NonNullApi;
import org.gradle.api.Task;
import org.gradle.api.internal.GradleInternal;
import org.gradle.execution.commandline.CommandLineTaskParser;
import org.gradle.execution.plan.ExecutionPlan;
import org.gradle.execution.selection.BuildTaskSelector;
import org.gradle.internal.deprecation.DeprecationLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * A {@link BuildTaskScheduler} which selects tasks which match the provided names. For each name, selects all tasks in all
 * projects whose name is the given name.
 */
public class TaskNameResolvingBuildTaskScheduler implements BuildTaskScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskNameResolvingBuildTaskScheduler.class);
    private final CommandLineTaskParser commandLineTaskParser;
    private final BuildTaskSelector.BuildSpecificSelector taskSelector;

    public TaskNameResolvingBuildTaskScheduler(CommandLineTaskParser commandLineTaskParser, BuildTaskSelector.BuildSpecificSelector taskSelector) {
        this.commandLineTaskParser = commandLineTaskParser;
        this.taskSelector = taskSelector;
    }

    @Override
    public void scheduleRequestedTasks(GradleInternal gradle, @Nullable EntryTaskSelector selector, ExecutionPlan plan) {
        if (selector != null) {
            selector.applyTasksTo(new EntryTaskSelectorContext(gradle), plan);
        }
        List<TaskExecutionRequest> taskParameters = gradle.getStartParameter().getTaskRequests();
        for (TaskExecutionRequest taskParameter : taskParameters) {
            List<TaskSelection> taskSelections = commandLineTaskParser.parseTasks(taskParameter);
            for (TaskSelection taskSelection : taskSelections) {
                LOGGER.info("Selected primary task '{}' from project {}", taskSelection.getTaskName(), taskSelection.getProjectPath());
                plan.addEntryTasks(taskSelection.getTasks());
            }
        }
        validateCompatibleTasksRequested(plan);
    }

    /**
     * Validates the tasks to be run are mutually compatible.
     * <p>
     * Currently, this checks that {@code init} is not run along with any other tasks.
     *
     * @param plan execution plan containing requested tasks to validate
     */
    private void validateCompatibleTasksRequested(ExecutionPlan plan) {
        Set<Task> requestedTasks = plan.getContents().getRequestedTasks();
        if (requestedTasks.size() > 1 && requestedTasks.stream().anyMatch(t -> t.getName().equals("init"))) { // TODO: Consider moving the InitBuiltInCommand (and help) to core, as they are not Software Platform-specific
            DeprecationLogger.deprecateAction("Executing other tasks along with the 'init' task")
                .withAdvice("The init task should be run by itself.")
                .willBecomeAnErrorInGradle9()
                .withUpgradeGuideSection(8, "init_must_run_alone")
                .nagUser();
        }
    }

    @NonNullApi
    private class EntryTaskSelectorContext implements EntryTaskSelector.Context {
        final GradleInternal gradle;

        public EntryTaskSelectorContext(GradleInternal gradle) {
            this.gradle = gradle;
        }

        @Override
        public TaskSelection getSelection(String taskPath) {
            return taskSelector.resolveTaskName(taskPath);
        }

        @Override
        public GradleInternal getGradle() {
            return gradle;
        }
    }
}
