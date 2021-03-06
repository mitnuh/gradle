/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.javadoc

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.TestResources
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition
import org.junit.Rule
import spock.lang.Issue
import spock.lang.Unroll

class JavadocIntegrationTest extends AbstractIntegrationSpec {
    @Rule TestResources testResources = new TestResources(temporaryFolder)

    @Issue("GRADLE-1563")
    def handlesTagsAndTaglets() {
        when:
        run("javadoc")

        then:
        def javadoc = testResources.dir.file("build/docs/javadoc/Person.html")
        javadoc.text =~ /(?ms)This is the Person class.*Author.*author value.*Deprecated.*deprecated value.*Custom Tag.*custom tag value/
        // we can't currently control the order between tags and taglets (limitation on our side)
        javadoc.text =~ /(?ms)Custom Taglet.*custom taglet value/
    }

    @Issue("GRADLE-2520")
    def canCombineLocalOptionWithOtherOptions() {
        when:
        run("javadoc")

        then:
        def javadoc = testResources.dir.file("build/docs/javadoc/Person.html")
        javadoc.text =~ /(?ms)USED LOCALE=de_DE/
        javadoc.text =~ /(?ms)Serial no. is valid javadoc!/
    }

    def "writes header"() {
        buildFile << """
            apply plugin: "java"
            javadoc.options.header = "<!-- Hey Joe! -->"
        """

        file("src/main/java/Foo.java") << "public class Foo {}"

        when: run("javadoc", "-i")
        then:
        file("build/docs/javadoc/Foo.html").text.contains("""Hey Joe!""")
    }

    @Issue("gradle/gradle#1090")
    def "allow single quote characters in options"() {
        buildFile << """
            apply plugin: "java"
            javadoc.options.header = "\\"'header text'\\""
        """

        file("src/main/java/Foo.java") << "public class Foo {}"

        when:
        succeeds 'javadoc'

        then:
        file("build/docs/javadoc/Foo.html").text.contains("\"'header text'\"")
    }

    def "can configure options with an Action"() {
        given:
        buildFile << '''
            apply plugin: "java"
            javadoc.options({ MinimalJavadocOptions options ->
                options.header = 'myHeader'
            } as Action<MinimalJavadocOptions>)
        '''.stripIndent()
        file("src/main/java/Foo.java") << "public class Foo {}"

        when:
        run 'javadoc'

        then:
        file('build/docs/javadoc/Foo.html').text.contains('myHeader')
    }

    @Requires(TestPrecondition.NOT_WINDOWS)
    @Issue("GRADLE-3099")
    def "writes multiline header"() {
        buildFile << """
            apply plugin: "java"
            javadoc.options.header = \"\"\"
                <!-- Hey
Joe! -->
            \"\"\"
        """

        file("src/main/java/Foo.java") << "public class Foo {}"

        when: run("javadoc", "-i")
        then:
        file("build/docs/javadoc/Foo.html").text.contains("""Hey
Joe!""")
    }

    @Issue("GRADLE-3152")
    def "can use the task without applying java-base plugin"() {
        buildFile << """
            task javadoc(type: Javadoc) {
                destinationDir = file("build/javadoc")
                source "src/main/java"
            }
        """

        file("src/main/java/Foo.java") << "public class Foo {}"

        when:
        run("javadoc")

        then:
        file("build/javadoc/Foo.html").exists()
    }

    def "changing standard doclet options makes task out-of-date"() {
        buildFile << """
            task javadoc(type: Javadoc) {
                destinationDir = file("build/javadoc")
                source "src/main/java"
                options {
                    windowTitle = "Window title"
                }
            }
        """

        file("src/main/java/Foo.java") << "public class Foo {}"

        when:
        run "javadoc"
        then:
        nonSkippedTasks == [":javadoc"]

        when:
        run "javadoc"
        then:
        skippedTasks as List == [":javadoc"]

        when:
        buildFile.text = """
            task javadoc(type: Javadoc) {
                destinationDir = file("build/javadoc")
                source "src/main/java"
                options {
                    windowTitle = "Window title changed"
                }
            }
        """
        run "javadoc"

        then:
        nonSkippedTasks == [":javadoc"]
    }
}
