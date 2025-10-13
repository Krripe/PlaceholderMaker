package org.placeholdermaker

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.fabricmc.loader.api.FabricLoader
import org.apache.logging.log4j.LogManager
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.StandardOpenOption

data class PlaceholderRule(
    val condition: String?,
    val text: String,
    val permission: String? = null
)

data class PlaceholderConfig(
    val placeholderid: String,
    val placeholder: String,
    val rules: List<PlaceholderRule>
)

data class PlaceholderFile(
    val placeholders: List<PlaceholderConfig>?
)

object PlaceholderLoader {
    private val gson = Gson()
    private val logger = LogManager.getLogger("PMaker")
    private val placeholderConfigs = mutableListOf<PlaceholderConfig>()

    fun loadPlaceholders() {
        val configDir = FabricLoader.getInstance().configDir.resolve("pmaker/placeholders")
        val configDirFile = configDir.toFile()
        if (!configDirFile.exists()) {
            configDirFile.mkdirs()

            val exampleFile = configDir.resolve("example_placeholders.json")
            val exampleContent = """
            {
              "placeholders": [
                {
                  "placeholderid": "health_status",
                  "placeholder": "%player_health%",
                  "rules": [
                    {
                      "condition": "<3",
                      "text": "&cYou're about to die!"
                    },
                    {
                      "condition": "<6",
                      "text": "&eYou're still fiiine..."
                    },
                    {
                      "condition": null,
                      "text": "&aHealthy as ever!"
                    }
                  ]
                },
                {
                  "placeholderid": "time_of_day",
                  "placeholder": "%server_time%",
                  "rules": [
                    {
                      "condition": "<12",
                      "text": "&6Good morning!"
                    },
                    {
                      "condition": "<18",
                      "text": "&eGood afternoon!"
                    },
                    {
                      "condition": null,
                      "text": "&9Good evening!"
                    }
                  ]
                }
              ]
            }
            """.trimIndent()

            try {
                Files.writeString(
                    exampleFile,
                    exampleContent,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
                )
                logger.info("Created example placeholder file at ${exampleFile.toAbsolutePath()}")
            } catch (e: Exception) {
                logger.error("Failed to create example placeholder file: ${e.message}")
            }

            return
        }

        val jsonFiles = configDirFile.listFiles { _, name -> name.endsWith(".json") } ?: return

        for (file in jsonFiles) {
            try {
                FileReader(file).use { reader ->
                    val placeholderListType = object : TypeToken<PlaceholderFile>() {}.type
                    val placeholderFile: PlaceholderFile = gson.fromJson(reader, placeholderListType)
                    if (placeholderFile.placeholders != null) {
                        placeholderConfigs.addAll(placeholderFile.placeholders)
                    } else {
                        logger.warn("No placeholders found in file ${file.name}")
                    }
                }
            } catch (e: Exception) {
                logger.error("Error reading file ${file.name}: ${e.message}")
            }
        }
    }

    fun getPlaceholderConfigs(): List<PlaceholderConfig> {
        return placeholderConfigs
    }
}
