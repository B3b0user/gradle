/*
 * Copyright 2024 the original author or authors.
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

package org.gradle.process.internal;

import org.gradle.process.CommandLineArgumentProvider;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface ClientExecHandleBuilder extends BaseExecHandleBuilder {
    ClientExecHandleBuilder commandLine(Iterable<?> args);

    ClientExecHandleBuilder commandLine(Object... args);

    ClientExecHandleBuilder setWorkingDir(File dir);

    ClientExecHandleBuilder setStandardInput(InputStream inputStream);

    @Override
    ClientExecHandleBuilder setStandardOutput(OutputStream outputStream);

    @Override
    ClientExecHandleBuilder setErrorOutput(OutputStream outputStream);

    ClientExecHandleBuilder redirectErrorStream();

    @Override
    ClientExecHandleBuilder setDisplayName(String displayName);

    ClientExecHandleBuilder setDaemon(boolean daemon);

    ClientExecHandleBuilder streamsHandler(StreamsHandler streamsHandler);

    ClientExecHandleBuilder setTimeout(int timeoutMillis);

    Map<String, Object> getEnvironment();

    ClientExecHandleBuilder environment(String key, Object value);

    ClientExecHandleBuilder args(Object... args);

    ClientExecHandleBuilder args(Iterable<?> args);

    List<String> getArgs();

    ClientExecHandleBuilder setArgs(Iterable<?> args);

    ClientExecHandleBuilder setExecutable(String executable);

    String getExecutable();

    File getWorkingDir();

    OutputStream getErrorOutput();

    List<String> getCommandLine();

    OutputStream getStandardOutput();

    List<String> getAllArguments();

    List<CommandLineArgumentProvider> getArgumentProviders();

    void setEnvironment(Map<String,?> environmentVariables);

    void environment(Map<String,?> environmentVariables);

    InputStream getStandardInput();

    ClientExecHandleBuilder setWorkingDir(Object dir);

    ExecHandle buildWithEffectiveArguments(List<String> effectiveArguments);
}
