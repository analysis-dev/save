package org.cqfn.save.core

import org.cqfn.save.core.config.TestConfig
import org.cqfn.save.core.files.createFile
import org.cqfn.save.core.plugin.GeneralConfig
import org.cqfn.save.plugin.warn.WarnPluginConfig
import org.cqfn.save.plugins.fix.FixPluginConfig

import okio.FileSystem

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

val fs = FileSystem.SYSTEM
val tmpDir = (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / MergeConfigsTest::class.simpleName!!)

val toml1 = tmpDir / "save.toml"
val nestedDir1 = tmpDir / "nestedDir1"
val toml2 = nestedDir1 / "save.toml"
val nestedDir2 = tmpDir / "nestedDir1" / "nestedDir2"
val toml3 = nestedDir2 / "save.toml"
val toml4 = nestedDir2 / "nestedDir3" / "nestedDir4" / "save.toml"

fun createTomlFiles() {
    fs.createDirectory(tmpDir)
    fs.createFile(toml1)
    fs.createDirectory(nestedDir1)
    fs.createFile(toml2)
    fs.createDirectory(nestedDir2)
    fs.createFile(toml3)
    fs.createDirectory(nestedDir2 / "nestedDir3")
    fs.createDirectory(nestedDir2 / "nestedDir3" / "nestedDir4")
    fs.createFile(toml4)
}

@Suppress("TOO_LONG_FUNCTION", "LOCAL_VARIABLE_EARLY_DECLARATION")
class MergeConfigsTest {
    private val generalConfig1 = GeneralConfig("Tag11, Tag12", "Description1", "suiteName1", "excludedTests: test1", "includedTests: test2")
    private val generalConfig2 = GeneralConfig("Tag21", "Description2", "suiteName2", "excludedTests: test3", "includedTests: test4")
    private val generalConfig3 = GeneralConfig("Tag21, Tag31, Tag32", "Description2", "suiteName3", "excludedTests: test5", "includedTests: test6")
    private val generalConfig4 = GeneralConfig("Tag11, Tag21", "Description2", "suiteName4", "excludedTests: test7", "includedTests: test8")
    private val warningsInputPattern1 = Regex(".*")
    private val warningsInputPattern2 = Regex("// ;warn:(\\d+):(\\d+): (.*)")
    private val warningsOutputPattern1 = Regex(".*")
    private val warningsOutputPattern2 = Regex("\\w+ - (\\d+)/(\\d+) - (.*)$")
    private val warnConfig1 = WarnPluginConfig("execCmd1", warningsInputPattern2, warningsOutputPattern2,
        false, false, 1, 1, 1, false)
    private val warnConfig2 = WarnPluginConfig("execCmd2", warningsInputPattern1, warningsOutputPattern1,
        true, true, 2, 2, 2, true)
    private val warnConfig3 = WarnPluginConfig("execCmd3", warningsInputPattern2, warningsOutputPattern2,
        warningTextHasColumn = false, lineCaptureGroup = 3, columnCaptureGroup = 3, messageCaptureGroup = 3)
    private val warnConfig4 = WarnPluginConfig("execCmd4", warningsInputPattern2, warningsOutputPattern2,
        lineCaptureGroup = 4, columnCaptureGroup = 4, messageCaptureGroup = 4)
    private val fixConfig1 = FixPluginConfig("fixCmd1", "some suffix")
    private val fixConfig2 = FixPluginConfig("fixCmd2")
    private val fixConfig3 = FixPluginConfig("fixCmd3", null)
    private val fixConfig4 = FixPluginConfig("fixCmd4")

    @Test
    fun `merge general configs`() {
        createTomlFiles()
        val config1 = TestConfig(toml1, null, mutableListOf(generalConfig1))
        val config2 = TestConfig(toml2, config1, mutableListOf(generalConfig2))

        config2.mergeConfigWithParents()

        assertEquals(1, config2.pluginConfigs.size)

        val expectedGeneralConfig = GeneralConfig("Tag11, Tag12, Tag21", "Description2", "suiteName2", "excludedTests: test3", "includedTests: test4")

        val actualGeneralConfig = config2.pluginConfigs.filterIsInstance<GeneralConfig>().first()

        assertEquals(expectedGeneralConfig, actualGeneralConfig)
    }

    @Test
    fun `merge two incomplete configs`() {
        createTomlFiles()
        val config1 = TestConfig(toml1, null, mutableListOf(generalConfig1, warnConfig1))
        val config2 = TestConfig(toml2, config1, mutableListOf(generalConfig2))

        config2.mergeConfigWithParents()

        assertEquals(2, config2.pluginConfigs.size)

        val expectedGeneralConfig = GeneralConfig("Tag11, Tag12, Tag21", "Description2", "suiteName2", "excludedTests: test3", "includedTests: test4")

        val actualGeneralConfig = config2.pluginConfigs.filterIsInstance<GeneralConfig>().first()
        val actualWarnConfig = config2.pluginConfigs.filterIsInstance<WarnPluginConfig>().first()

        assertEquals(expectedGeneralConfig, actualGeneralConfig)
        assertEquals(warnConfig1, actualWarnConfig)
    }

    @Test
    fun `merge two incomplete configs 2`() {
        createTomlFiles()
        val config1 = TestConfig(toml1, null, mutableListOf())
        val config2 = TestConfig(toml2, config1, mutableListOf(generalConfig2, warnConfig1))

        config2.mergeConfigWithParents()

        assertEquals(2, config2.pluginConfigs.size)

        val actualGeneralConfig = config2.pluginConfigs.filterIsInstance<GeneralConfig>().first()
        val actualWarnConfig = config2.pluginConfigs.filterIsInstance<WarnPluginConfig>().first()

        assertEquals(generalConfig2, actualGeneralConfig)
        assertEquals(warnConfig1, actualWarnConfig)
    }

    @Test
    fun `merge two configs with different fields`() {
        createTomlFiles()
        val config1 = TestConfig(toml1, null, mutableListOf(generalConfig1, warnConfig2, fixConfig1))
        val config2 = TestConfig(toml2, config1, mutableListOf(generalConfig2, warnConfig3, fixConfig2))

        config2.mergeConfigWithParents()

        assertEquals(3, config2.pluginConfigs.size)

        val expectedGeneralConfig = GeneralConfig("Tag11, Tag12, Tag21", "Description2", "suiteName2", "excludedTests: test3", "includedTests: test4")
        val expectedWarnConfig = WarnPluginConfig("execCmd3", warningsInputPattern2, warningsOutputPattern2,
            true, false, 3, 3, 3, true)
        val expectedFixConfig = FixPluginConfig("fixCmd2", "some suffix")

        val actualGeneralConfig = config2.pluginConfigs.filterIsInstance<GeneralConfig>().first()
        val actualWarnConfig = config2.pluginConfigs.filterIsInstance<WarnPluginConfig>().first()
        val actualFixConfig = config2.pluginConfigs.filterIsInstance<FixPluginConfig>().first()

        assertEquals(expectedGeneralConfig, actualGeneralConfig)
        assertEquals(expectedWarnConfig, actualWarnConfig)
        assertEquals(expectedFixConfig, actualFixConfig)
    }

    @Test
    fun `merge configs with many parents`() {
        createTomlFiles()
        val config1 = TestConfig(toml1, null, mutableListOf(generalConfig1, warnConfig1, fixConfig1))
        val config2 = TestConfig(toml2, config1, mutableListOf(generalConfig2, warnConfig2, fixConfig2))
        val config3 = TestConfig(toml3, config2, mutableListOf(generalConfig3, warnConfig3, fixConfig3))
        val config4 = TestConfig(toml4, config3, mutableListOf(generalConfig4, warnConfig4, fixConfig4))

        config4.mergeConfigWithParents()

        assertEquals(3, config4.pluginConfigs.size)
        val expectedGeneralConfig = GeneralConfig("Tag11, Tag12, Tag21, Tag31, Tag32", "Description2", "suiteName4", "excludedTests: test7", "includedTests: test8")
        val expectedWarnConfig = WarnPluginConfig("execCmd4", warningsInputPattern2, warningsOutputPattern2,
            true, false, 4, 4, 4, true)
        val expectedFixConfig = FixPluginConfig("fixCmd4", "some suffix")

        val actualGeneralConfig = config4.pluginConfigs.filterIsInstance<GeneralConfig>().first()
        val actualWarnConfig = config4.pluginConfigs.filterIsInstance<WarnPluginConfig>().first()
        val actualFixConfig = config4.pluginConfigs.filterIsInstance<FixPluginConfig>().first()

        assertEquals(expectedGeneralConfig, actualGeneralConfig)
        assertEquals(expectedWarnConfig, actualWarnConfig)
        assertEquals(expectedFixConfig, actualFixConfig)
    }

    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }
}

// TODO are these tests even necessary?
class ValidationTest {
    @Test
    fun `general section validation`() {
        createTomlFiles()
        val generalConfig = GeneralConfig(tags = "Tag11, Tag12", description = "Description1", suiteName = "suiteName1")
        val config = TestConfig(toml1, null, mutableListOf(generalConfig))

        config.validate()

        assertEquals(1, config.pluginConfigs.size)

        val actualGeneralConfig1 = config.pluginConfigs.filterIsInstance<GeneralConfig>().first()
        assertEquals("", actualGeneralConfig1.excludedTests)
        assertEquals("", actualGeneralConfig1.includedTests)
        assertEquals(false, actualGeneralConfig1.ignoreSaveComments)
    }

    @Test
    fun `general section validation 2`() {
        createTomlFiles()
        val generalConfig = GeneralConfig()
        val config = TestConfig(toml1, null, mutableListOf(generalConfig))
        try {
            config.validate()
        } catch (ex: IllegalArgumentException) {
            assertEquals(
                "Error: Couldn't found `tags` in [general] section. Please provide it in this, " +
                        "or at least in one of the parent configs", ex.message
            )
        }
    }

    @Test
    fun `warn section validation`() {
        createTomlFiles()
        val warnConfig = WarnPluginConfig(execCmd = "execCmd", messageCaptureGroup = 1)
        val config = TestConfig(toml1, null, mutableListOf(warnConfig))

        config.validate()

        assertEquals(1, config.pluginConfigs.size)

        val actualWarnConfig = config.pluginConfigs.filterIsInstance<WarnPluginConfig>().first()
        assertEquals(Regex(";warn:(\\d+):(\\d+): (.+)").toString(), actualWarnConfig.warningsInputPattern.toString())
        assertEquals(Regex(".*(\\d+):(\\d+): (.+)").toString(), actualWarnConfig.warningsOutputPattern.toString())
        assertEquals(false, actualWarnConfig.warningTextHasLine)
        assertEquals(false, actualWarnConfig.warningTextHasColumn)
        assertEquals(true, actualWarnConfig.exactWarningsMatch)
    }

    @Test
    fun `warn section validation 2`() {
        createTomlFiles()
        val warnConfig = WarnPluginConfig(execCmd = "execCmd", lineCaptureGroup = 2, messageCaptureGroup = 1)
        val config = TestConfig(toml1, null, mutableListOf(warnConfig))
        try {
            config.validate()
        } catch (ex: IllegalArgumentException) {
            assertEquals(
                "warn-plugin configuration error: either warningTextHasLine should be false (actual: null) " +
                        "or lineCaptureGroup should be provided (actual: 2)", ex.message
            )
        }
    }

    @Test
    fun `fix section validation`() {
        createTomlFiles()
        val fixConfig = FixPluginConfig(execCmd = "execCmd")
        val config = TestConfig(toml1, null, mutableListOf(fixConfig))

        config.validate()

        assertEquals(1, config.pluginConfigs.size)

        val actualFixConfig = config.pluginConfigs.filterIsInstance<FixPluginConfig>().first()
        assertEquals("_copy", actualFixConfig.destinationFileSuffix)
    }


    @AfterTest
    fun tearDown() {
        fs.deleteRecursively(tmpDir)
    }
}
