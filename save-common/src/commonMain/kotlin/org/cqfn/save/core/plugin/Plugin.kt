package org.cqfn.save.core.plugin

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.result.TestResult

import okio.Path

/**
 * Plugin that can be injected into SAVE during execution. Plugins accept contents of configuration file and then perform some work.
 * @property testConfig
 */
abstract class Plugin(open val testConfig: TestConfig) {
    /**
     * Perform plugin's work.
     *
     * @return a sequence of [TestResult]s for each group of test resources
     */
    abstract fun execute(): Sequence<TestResult>

    /**
     * Discover groups of resource files which will be used to run tests.
     *
     * @param root root [Path], from where discovering should be started
     * @return a sequence of files, grouped by test
     */
    abstract fun discoverTestFiles(root: Path): Sequence<List<Path>>
}
