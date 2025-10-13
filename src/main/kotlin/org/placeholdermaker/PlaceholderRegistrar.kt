package org.placeholdermaker

import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.PlaceholderResult
import eu.pb4.placeholders.api.Placeholders
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.placeholdermaker.utils.CustomColorParser
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object PlaceholderRegistrar {
    fun registerPlaceholders() {
        for (config in PlaceholderLoader.getPlaceholderConfigs()) {
            val identifier = Identifier.of("pm", config.placeholderid)
            Placeholders.register(identifier) { ctx: PlaceholderContext, _: String? ->
                handlePlaceholder(config, ctx)
            }
        }
    }

    private fun handlePlaceholder(config: PlaceholderConfig, ctx: PlaceholderContext): PlaceholderResult {
        val inputPlaceholder = config.placeholder
        val inputValue = parsePlaceholder(inputPlaceholder, ctx)

        val outputText = if (inputValue == null) {
            applyNullCondition(config.rules, ctx)
        } else {
            applyRules(config.rules, inputValue, ctx)
        }

        return PlaceholderResult.value(outputText)
    }

    private fun parsePlaceholder(placeholder: String, ctx: PlaceholderContext): String? {
        return try {
            val parsedText = Placeholders.parseText(Text.literal(placeholder), ctx)
            parsedText.string.ifBlank {
                null
            }
        } catch (e: Exception) {
            println("Error parsing placeholder: $placeholder - ${e.message}")
            null
        }
    }

    private fun applyNullCondition(rules: List<PlaceholderRule>, ctx: PlaceholderContext): Text {
        for (rule in rules) {
            if (rule.condition == null) {
                return processText(rule.text, ctx)
            }
        }
        return Text.literal("")
    }

    private fun applyRules(rules: List<PlaceholderRule>, inputValue: String, ctx: PlaceholderContext): Text {
        for (rule in rules) {
            val condition = rule.condition
            if (checkCondition(condition, inputValue)) {
                return processText(rule.text, ctx)
            }
        }
        return Text.literal("")
    }

    private fun checkCondition(condition: String?, inputValue: String): Boolean {
        try {
            if (condition == null) {
                return true
            }

            val operator = condition.takeWhile { !it.isLetterOrDigit() && it != '-' && it != ':' }
            val value = condition.drop(operator.length)

            if (operator == "=" || operator == "!") {
                return when (operator) {
                    "=" -> inputValue == value
                    "!" -> inputValue != value
                    else -> false
                }
            }

            if (value.matches("\\d{1,2}:\\d{2}(:\\d{2})?".toRegex())) {
                val conditionTime = LocalTime.parse(value, DateTimeFormatter.ofPattern("H:m[:s]"))
                val inputTime = LocalTime.parse(inputValue, DateTimeFormatter.ofPattern("H:m[:s]"))

                return when (operator) {
                    "<" -> inputTime.isBefore(conditionTime)
                    ">" -> inputTime.isAfter(conditionTime)
                    "=" -> inputTime == conditionTime
                    "!" -> inputTime != conditionTime
                    else -> false
                }
            }

            if (value.toDoubleOrNull() != null && inputValue.toDoubleOrNull() != null) {
                val inputNumber = inputValue.toDouble()
                val conditionNumber = value.toDouble()

                return when (operator) {
                    "<" -> inputNumber < conditionNumber
                    ">" -> inputNumber > conditionNumber
                    "<=" -> inputNumber <= conditionNumber
                    ">=" -> inputNumber >= conditionNumber
                    "%" -> inputNumber % conditionNumber == 0.0
                    "=" -> inputNumber == conditionNumber
                    "!" -> inputNumber != conditionNumber
                    else -> false
                }
            }

            return false
        } catch (e: Exception) {
            println("Error checking condition: $condition with input value: $inputValue - ${e.message}")
            return false
        }
    }

    private fun processText(rawText: String, ctx: PlaceholderContext): Text {
        return try {
            val resolvedText = Placeholders.parseText(Text.literal(rawText), ctx).string
            CustomColorParser.processAllFormats(resolvedText)
        } catch (e: Exception) {
            println("Error processing text: $rawText - ${e.message}")
            Text.literal("Error")
        }
    }
}
