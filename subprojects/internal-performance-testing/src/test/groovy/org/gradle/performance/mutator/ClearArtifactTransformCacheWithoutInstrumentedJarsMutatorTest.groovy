/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.performance.mutator

import org.gradle.util.GradleVersion
import spock.lang.Specification
import spock.lang.TempDir

import static org.gradle.api.internal.initialization.transform.utils.InstrumentationTransformUtils.ANALYSIS_OUTPUT_DIR
import static org.gradle.api.internal.initialization.transform.utils.InstrumentationTransformUtils.MERGE_OUTPUT_DIR
import static org.gradle.internal.classpath.TransformedClassPath.FileMarker.INSTRUMENTATION_CLASSPATH_MARKER
import static org.gradle.profiler.mutations.AbstractCleanupMutator.CleanupSchedule.BUILD

class ClearArtifactTransformCacheWithoutInstrumentedJarsMutatorTest extends Specification {

    @TempDir
    File gradleUserHome

    def "should cleanup all transforms-X folders except the ones with instrumented jars"() {
        given:
        def cachesDir = new File(gradleUserHome, "caches")
        createFile(new File(cachesDir, "transforms-1/first/transformed/file"))
        createFile(new File(cachesDir, "transforms-1/first/metadata.bin"))
        createFile(new File(cachesDir, "transforms-1/first/transformed/instrumented/file"))
        createFile(new File(cachesDir, "transforms-1/second/transformed/original/file"))
        createFile(new File(cachesDir, "transforms-2/first/transformed/instrumented/file"))
        createFile(new File(cachesDir, "transforms-2/first/transformed/original/file"))
        createFile(new File(cachesDir, "transforms-2/second/metadata.bin"))
        createFile(new File(cachesDir, "transforms-2/second/transformed/${INSTRUMENTATION_CLASSPATH_MARKER.fileName}"))
        createFile(new File(cachesDir, "transforms-2/second/transformed/instrumented/file"))
        createFile(new File(cachesDir, "transforms-2/second/transformed/original/file"))
        createFile(new File(cachesDir, "transforms-2/third/metadata.bin"))
        createFile(new File(cachesDir, "transforms-2/third/transformed/$ANALYSIS_OUTPUT_DIR/${INSTRUMENTATION_CLASSPATH_MARKER.fileName}"))
        createFile(new File(cachesDir, "transforms-2/third/transformed/original/file"))
        createFile(new File(cachesDir, "transforms-2/fourth/metadata.bin"))
        createFile(new File(cachesDir, "transforms-2/fourth/transformed/$MERGE_OUTPUT_DIR/${INSTRUMENTATION_CLASSPATH_MARKER.fileName}"))
        createFile(new File(cachesDir, "transforms-2/fourth/transformed/original/file"))

        def mutator = ClearArtifactTransformCacheWithoutInstrumentedJarsMutator.create(gradleUserHome, BUILD)

        when:
        mutator.beforeBuild(null)

        then:
        !new File(cachesDir, "transforms-1/").exists()
        !new File(cachesDir, "transforms-2/first").exists()
        new File(cachesDir, "transforms-2/second/metadata.bin").exists()
        new File(cachesDir, "transforms-2/second/transformed/${INSTRUMENTATION_CLASSPATH_MARKER.fileName}").exists()
        new File(cachesDir, "transforms-2/second/transformed/instrumented/file").exists()
        new File(cachesDir, "transforms-2/second/transformed/original/file").exists()
        new File(cachesDir, "transforms-2/third/metadata.bin").exists()
        new File(cachesDir, "transforms-2/third/transformed/$ANALYSIS_OUTPUT_DIR/${INSTRUMENTATION_CLASSPATH_MARKER.fileName}").exists()
        new File(cachesDir, "transforms-2/third/transformed/original/file").exists()
        new File(cachesDir, "transforms-2/fourth/metadata.bin").exists()
        new File(cachesDir, "transforms-2/fourth/transformed/$MERGE_OUTPUT_DIR/${INSTRUMENTATION_CLASSPATH_MARKER.fileName}").exists()
        new File(cachesDir, "transforms-2/fourth/transformed/original/file").exists()
    }

    def "should cleanup all version-bound transform folders except the ones with instrumented jars"() {
        given:
        def cachesDir = new File(gradleUserHome, "caches/${GradleVersion.current().version}")
        createFile(new File(cachesDir, "transforms/first/transformed/instrumented/file"))
        createFile(new File(cachesDir, "transforms/first/transformed/original/file"))
        createFile(new File(cachesDir, "transforms/second/metadata.bin"))
        createFile(new File(cachesDir, "transforms/second/transformed/${INSTRUMENTATION_CLASSPATH_MARKER.fileName}"))
        createFile(new File(cachesDir, "transforms/second/transformed/instrumented/file"))
        createFile(new File(cachesDir, "transforms/second/transformed/original/file"))
        createFile(new File(cachesDir, "transforms/third/metadata.bin"))
        createFile(new File(cachesDir, "transforms/third/transformed/$ANALYSIS_OUTPUT_DIR/${INSTRUMENTATION_CLASSPATH_MARKER.fileName}"))
        createFile(new File(cachesDir, "transforms/third/transformed/original/file"))
        createFile(new File(cachesDir, "transforms/fourth/metadata.bin"))
        createFile(new File(cachesDir, "transforms/fourth/transformed/$MERGE_OUTPUT_DIR/${INSTRUMENTATION_CLASSPATH_MARKER.fileName}"))
        createFile(new File(cachesDir, "transforms/fourth/transformed/original/file"))

        def mutator = ClearArtifactTransformCacheWithoutInstrumentedJarsMutator.create(gradleUserHome, BUILD)

        when:
        mutator.beforeBuild(null)

        then:
        !new File(cachesDir, "transforms/first").exists()
        new File(cachesDir, "transforms/second/metadata.bin").exists()
        new File(cachesDir, "transforms/second/transformed/${INSTRUMENTATION_CLASSPATH_MARKER.fileName}").exists()
        new File(cachesDir, "transforms/second/transformed/instrumented/file").exists()
        new File(cachesDir, "transforms/second/transformed/original/file").exists()
        new File(cachesDir, "transforms/third/metadata.bin").exists()
        new File(cachesDir, "transforms/third/transformed/$ANALYSIS_OUTPUT_DIR/${INSTRUMENTATION_CLASSPATH_MARKER.fileName}").exists()
        new File(cachesDir, "transforms/third/transformed/original/file").exists()
        new File(cachesDir, "transforms/fourth/metadata.bin").exists()
        new File(cachesDir, "transforms/fourth/transformed/$MERGE_OUTPUT_DIR/${INSTRUMENTATION_CLASSPATH_MARKER.fileName}").exists()
        new File(cachesDir, "transforms/fourth/transformed/original/file").exists()
    }

    private static void createFile(File file) {
        file.parentFile.mkdirs()
        file.createNewFile()
    }
}
