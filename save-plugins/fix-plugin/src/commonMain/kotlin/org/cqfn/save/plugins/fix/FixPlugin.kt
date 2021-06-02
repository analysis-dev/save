package org.cqfn.save.plugins.fix

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.files.readLines
import org.cqfn.save.core.logging.logInfo
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.result.DebugInfo
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Pass
import org.cqfn.save.core.result.TestResult
import org.cqfn.save.core.utils.ProcessBuilder

import io.github.petertrr.diffutils.diff
import io.github.petertrr.diffutils.patch.ChangeDelta
import io.github.petertrr.diffutils.patch.Patch
import io.github.petertrr.diffutils.text.DiffRowGenerator
import okio.FileSystem
import okio.Path

/**
 * A plugin that runs an executable on a file and compares output with expected output.
 * @property testConfig
 */
@Suppress("INLINE_CLASS_CAN_BE_USED")
class FixPlugin(testConfig: TestConfig, testFiles: List<String> = emptyList()) : Plugin(testConfig, testFiles) {
    private val fs = FileSystem.SYSTEM
    private val pb = ProcessBuilder()
    private val diffGenerator = DiffRowGenerator.create()
        .showInlineDiffs(true)
        .mergeOriginalRevised(false)
        .inlineDiffByWord(false)
        .oldTag { start -> if (start) "[" else "]" }
        .newTag { start -> if (start) "<" else ">" }
        .build()

    override fun handleFiles(files: Sequence<List<Path>>): Sequence<TestResult> {
        val fixPluginConfig = testConfig.pluginConfigs.filterIsInstance<FixPluginConfig>().single()
        logInfo("Discovered the following file pairs for comparison: $files")
        return files
            .map { it.first() to it.last() }
            .map { (expected, test) ->
                val testCopy = createTestFile(test)
                val execCmd = "${fixPluginConfig.execCmd} $testCopy"
                val executionResult = pb.exec(execCmd, null, false)
                val fixedLines = FileSystem.SYSTEM.readLines(testCopy)
                val expectedLines = FileSystem.SYSTEM.readLines(expected)
                val status = diff(expectedLines, fixedLines).let { patch ->
                    if (patch.deltas.isEmpty()) {
                        Pass(null)
                    } else {
                        Fail(patch.formatToString())
                    }
                }
                TestResult(
                    listOf(expected, test),
                    status,
                    // todo: fill debug info
                    DebugInfo(executionResult.stdout.joinToString("\n"), null, null)
                )
            }
    }

    private fun createTestFile(path: Path): Path {
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / FixPlugin::class.simpleName!!)

        if (!fs.exists(tmpDir)) {
            fs.createDirectory(tmpDir)
        }
        val pathCopy: Path = tmpDir / path.name
        fs.write(fs.createFile(pathCopy)) {
            fs.readLines(path).forEach {
                write(
                    (it + "\n").encodeToByteArray()
                )
            }
        }
        return pathCopy
    }

    override fun rawDiscoverTestFiles(resourceDirectories: Sequence<Path>): Sequence<List<Path>> {
        val fixPluginConfig = testConfig.pluginConfigs.filterIsInstance<FixPluginConfig>().single()
        val regex = fixPluginConfig.resourceNamePattern
        val resourceNameTest = fixPluginConfig.resourceNameTest
        val resourceNameExpected = fixPluginConfig.resourceNameExpected
        return resourceDirectories
            .map { FileSystem.SYSTEM.list(it) }
            .flatMap { files ->
                files.groupBy {
                    val matchResult = (regex).matchEntire(it.name)
                    matchResult?.groupValues?.get(1)  // this is a capture group for the start of file name
                }
                    .filter { it.value.size > 1 && it.key != null }
                    .mapValues { (name, group) ->
                        require(group.size == 2) { "Files should be grouped in pairs, but for name $name these files have been discovered: $group" }
                        listOf(
                            group.first { it.name.contains("$resourceNameExpected.") },
                            group.first { it.name.contains("$resourceNameTest.") }
                        )
                    }
                    .values
            }
            .filter { it.isNotEmpty() }
    }

    override fun cleanDir() {
        val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / FixPlugin::class.simpleName!!)
        if (fs.exists(tmpDir)) {
            fs.deleteRecursively(tmpDir)
        }
    }

    private fun Patch<String>.formatToString() = deltas.joinToString("\n") { delta ->
        when (delta) {
            is ChangeDelta -> diffGenerator
                .generateDiffRows(delta.source.lines, delta.target.lines)
                .joinToString(prefix = "ChangeDelta, position ${delta.source.position}, lines:\n", separator = "\n\n") {
                    """-${it.oldLine}
                      |+${it.newLine}
                      |""".trimMargin()
                }
            else -> delta.toString()
        }
    }
}
