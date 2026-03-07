package com.example.lifetogether.domain.logic

import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideProgressState
import com.example.lifetogether.domain.model.guides.GuideResume
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.guides.GuideStep
import com.example.lifetogether.domain.model.guides.GuideStepType
import com.example.lifetogether.domain.model.guides.GuideVisibility
import com.google.firebase.Timestamp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.util.Date
import java.util.UUID

object GuideParser {
    private val json = Json

    fun parseFirestoreGuide(documentId: String, data: Map<String, Any?>): Guide {
        return parseGuideMap(rawMap = data, idOverride = documentId)
    }

    fun parseGuideMap(
        rawMap: Map<String, Any?>,
        idOverride: String? = null,
        familyIdOverride: String? = null,
        ownerUidOverride: String? = null,
        regenerateIds: Boolean = false,
        defaultVisibility: GuideVisibility = GuideVisibility.PRIVATE,
    ): Guide {
        val normalizedId = idOverride?.takeIf { it.isNotBlank() }
        val familyId = familyIdOverride ?: readString(rawMap, "familyId")
        val ownerUid = ownerUidOverride ?: readString(rawMap, "ownerUid")

        val itemName = readString(rawMap, "name").ifBlank {
            readString(rawMap, "itemName")
        }

        val rawSections = asList(rawMap["sections"])
        val sections = rawSections.mapIndexedNotNull { index, value ->
            asMap(value)?.let { parseSection(it, index, regenerateIds) }
        }

        return Guide(
            id = normalizedId,
            familyId = familyId,
            itemName = itemName,
            description = readString(rawMap, "description"),
            lastUpdated = parseDate(rawMap["lastUpdated"]),
            visibility = GuideVisibility.fromValue(readString(rawMap, "visibility").ifBlank { defaultVisibility.value }),
            ownerUid = ownerUid,
            contentVersion = readLong(rawMap, "contentVersion") ?: 1L,
            started = readBoolean(rawMap, "started"),
            sections = sections,
            resume = parseResume(rawMap["resume"]),
        )
    }

    fun parseJsonGuides(
        json: String,
        familyId: String,
        ownerUid: String,
        defaultVisibility: GuideVisibility = GuideVisibility.PRIVATE,
    ): List<Guide> {
        val root = GuideParser.json.parseToJsonElement(json)
        val maps = when {
            root is JsonObject -> listOf(jsonObjectToMap(root))
            root is JsonArray -> root.mapNotNull { element ->
                if (element !is JsonObject) return@mapNotNull null
                jsonObjectToMap(element)
            }

            else -> emptyList()
        }

        return maps.mapNotNull { map ->
            runCatching {
                parseGuideMap(
                    rawMap = map,
                    familyIdOverride = familyId,
                    ownerUidOverride = ownerUid,
                    regenerateIds = true,
                    defaultVisibility = defaultVisibility,
                )
            }.getOrNull()
        }
    }

    fun guideToFirestoreMap(guide: Guide): Map<String, Any?> {
        return mutableMapOf(
            "familyId" to guide.familyId,
            "ownerUid" to guide.ownerUid,
            "visibility" to guide.visibility.value,
            "name" to guide.itemName,
            "description" to guide.description,
            "contentVersion" to guide.contentVersion,
            "lastUpdated" to guide.lastUpdated,
            "sections" to guide.sections.map { sectionToFirestoreMap(it) },
        )
    }

    fun parseGuideProgressMap(
        documentId: String,
        data: Map<String, Any?>,
    ): GuideProgressState? {
        val familyId = readString(data, "familyId")
        val uid = readString(data, "uid")
        val guideId = readString(data, "guideId")
        if (familyId.isBlank() || uid.isBlank() || guideId.isBlank()) return null

        val completedPointerKeys = asList(data["completedPointerKeys"])
            .mapNotNull { value -> readNullableString(value) }
            .filter { it.isNotBlank() }

        return GuideProgressState(
            id = documentId,
            familyId = familyId,
            uid = uid,
            guideId = guideId,
            contentVersion = readLong(data, "contentVersion") ?: 1L,
            started = readBoolean(data, "started"),
            completedPointerKeys = completedPointerKeys,
            resume = parseResume(data["resume"]),
            lastUpdated = parseDate(data["lastUpdated"]),
            pendingSync = false,
            localUpdatedAt = parseDate(data["localUpdatedAt"]),
            lastUploadedAt = parseDateOrNull(data["lastUploadedAt"]),
        )
    }

    fun guideProgressToFirestoreMap(progress: GuideProgressState): Map<String, Any?> {
        return mutableMapOf(
            "familyId" to progress.familyId,
            "uid" to progress.uid,
            "guideId" to progress.guideId,
            "contentVersion" to progress.contentVersion,
            "started" to progress.started,
            "completedPointerKeys" to progress.completedPointerKeys,
            "resume" to resumeToFirestoreMap(progress.resume),
            "lastUpdated" to progress.lastUpdated,
            "localUpdatedAt" to progress.localUpdatedAt,
            "lastUploadedAt" to progress.lastUploadedAt,
        ).filterValues { value -> value != null }
    }

    private fun parseSection(
        rawSection: Map<String, Any?>,
        sectionIndex: Int,
        regenerateIds: Boolean,
    ): GuideSection {
        val steps = parseSteps(
            rawSteps = parseStepSource(rawSection["steps"]),
            regenerateIds = regenerateIds,
        )
        val amount = (readInt(rawSection, "amount") ?: 1).coerceAtLeast(1)
        val sectionMarkedCompleted = readBoolean(rawSection, "completed", "isCompleted")
        val allLeafStepsCompleted = areAllLeafStepsCompleted(steps)
        val inferredCompletedAmount = when {
            sectionMarkedCompleted -> amount
            allLeafStepsCompleted -> 1.coerceAtMost(amount)
            else -> 0
        }
        val completedAmount = (readInt(rawSection, "completedAmount") ?: inferredCompletedAmount)
            .coerceIn(0, amount)

        return GuideSection(
            id = resolveNestedId(readString(rawSection, "id"), regenerateIds),
            orderNumber = readInt(rawSection, "orderNumber") ?: (sectionIndex + 1),
            title = readString(rawSection, "title"),
            subtitle = readNullableString(rawSection["subtitle"]),
            amount = amount,
            completedAmount = completedAmount,
            completed = completedAmount >= amount,
            comment = readNullableString(rawSection["comment"]),
            steps = steps,
        )
    }

    private fun parseStep(
        rawStep: Map<String, Any?>,
        regenerateIds: Boolean,
    ): List<GuideStep> {
        val nestedSource = when {
            rawStep.containsKey("subSteps") -> rawStep["subSteps"]
            rawStep.containsKey("steps") -> rawStep["steps"]
            else -> null
        }

        val subSteps = parseSteps(
            rawSteps = parseStepSource(nestedSource),
            regenerateIds = regenerateIds,
        )

        val step = GuideStep(
            id = resolveNestedId(readString(rawStep, "id"), regenerateIds),
            name = readString(rawStep, "name"),
            type = GuideStepType.fromValue(readString(rawStep, "type")),
            title = readString(rawStep, "title"),
            content = readString(rawStep, "content"),
            completed = readBoolean(rawStep, "completed", "isCompleted"),
            subSteps = subSteps,
        )

        return expandRoundRange(step, regenerateIds)
    }

    private fun parseSteps(
        rawSteps: List<Any?>,
        regenerateIds: Boolean,
    ): List<GuideStep> {
        return rawSteps.flatMap { value ->
            val rawStep = asMap(value) ?: return@flatMap emptyList()
            parseStep(rawStep, regenerateIds)
        }
    }

    private fun expandRoundRange(
        step: GuideStep,
        regenerateIds: Boolean,
    ): List<GuideStep> {
        if (step.type != GuideStepType.ROUND || step.subSteps.isNotEmpty()) return listOf(step)

        val nameOrTitleSpan = GuideRoundGrouping.parseRoundSpan(
            step.name.ifBlank { step.title },
        )
        if (nameOrTitleSpan != null) {
            return buildRoundStepsFromSpan(
                original = step,
                span = nameOrTitleSpan,
                sharedContent = step.content,
                regenerateIds = regenerateIds,
            )
        }

        if (step.name.isBlank() && step.title.isBlank()) {
            val contentPrefix = GuideRoundGrouping.parseRoundPrefix(step.content)
            if (contentPrefix != null) {
                val (span, sharedContent) = contentPrefix
                return buildRoundStepsFromSpan(
                    original = step,
                    span = span,
                    sharedContent = sharedContent,
                    regenerateIds = regenerateIds,
                )
            }
        }

        return listOf(step)
    }

    private fun buildRoundStepsFromSpan(
        original: GuideStep,
        span: IntRange,
        sharedContent: String,
        regenerateIds: Boolean,
    ): List<GuideStep> {
        val normalizedContent = sharedContent.trim()
        return span.mapIndexed { index, roundNumber ->
            original.copy(
                id = generateExpandedStepId(original.id, index, regenerateIds),
                name = "R$roundNumber",
                title = "",
                content = normalizedContent,
            )
        }
    }

    private fun generateExpandedStepId(
        baseId: String,
        offset: Int,
        regenerateIds: Boolean,
    ): String {
        if (regenerateIds || baseId.isBlank()) return UUID.randomUUID().toString()
        return if (offset == 0) {
            baseId
        } else {
            "$baseId-$offset"
        }
    }

    private fun parseResume(rawValue: Any?): GuideResume? {
        val map = asMap(rawValue) ?: return null
        return GuideResume(
            sectionIndex = readInt(map, "sectionIndex") ?: 0,
            sectionAmountIndex = readInt(map, "sectionAmountIndex") ?: 0,
            stepIndex = readInt(map, "stepIndex") ?: 0,
            subStepIndex = readInt(map, "subStepIndex"),
        )
    }

    private fun parseStepSource(rawSource: Any?): List<Any?> {
        return when (rawSource) {
            is List<*> -> rawSource
            is Map<*, *> -> listOf(rawSource)
            else -> emptyList()
        }
    }

    private fun sectionToFirestoreMap(section: GuideSection): Map<String, Any?> {
        val normalizedAmount = section.amount.coerceAtLeast(1)
        val normalizedCompletedAmount = section.completedAmount.coerceIn(0, normalizedAmount)
        return mutableMapOf(
            "id" to section.id,
            "orderNumber" to section.orderNumber,
            "title" to section.title,
            "subtitle" to section.subtitle,
            "amount" to normalizedAmount,
            "completedAmount" to normalizedCompletedAmount,
            "completed" to (normalizedCompletedAmount >= normalizedAmount),
            "comment" to section.comment,
            "steps" to section.steps.map { stepToFirestoreMap(it) },
        ).filterValues { value -> value != null }
    }

    private fun stepToFirestoreMap(step: GuideStep): Map<String, Any?> {
        return mutableMapOf<String, Any?>(
            "id" to step.id,
            "name" to step.name.takeIf { it.isNotBlank() },
            "type" to step.type.value,
            "title" to step.title.takeIf { it.isNotBlank() },
            "content" to step.content.takeIf { it.isNotBlank() },
            "completed" to step.completed,
            "steps" to step.subSteps.takeIf { it.isNotEmpty() }?.map { subStep ->
                stepToFirestoreMap(subStep)
            },
        ).filterValues { value -> value != null }
    }

    private fun resumeToFirestoreMap(resume: GuideResume?): Map<String, Any?>? {
        if (resume == null) return null
        return mutableMapOf<String, Any?>(
            "sectionIndex" to resume.sectionIndex,
            "sectionAmountIndex" to resume.sectionAmountIndex,
            "stepIndex" to resume.stepIndex,
            "subStepIndex" to resume.subStepIndex,
        ).filterValues { value -> value != null }
    }

    private fun areAllLeafStepsCompleted(steps: List<GuideStep>): Boolean {
        if (steps.isEmpty()) return false
        return steps.all { step ->
            if (step.subSteps.isNotEmpty()) {
                step.subSteps.all { it.completed }
            } else {
                step.completed
            }
        }
    }

    private fun jsonObjectToMap(jsonObject: JsonObject): Map<String, Any?> {
        return jsonObject.entries.associate { (key, value) ->
            key to jsonElementToValue(value)
        }
    }

    private fun jsonElementToValue(value: JsonElement): Any? {
        return when (value) {
            is JsonObject -> jsonObjectToMap(value)
            is JsonArray -> value.map { element -> jsonElementToValue(element) }
            is JsonNull -> null
            is JsonPrimitive -> {
                if (value.isString) {
                    value.content
                } else {
                    value.booleanOrNull
                        ?: value.longOrNull
                        ?: value.doubleOrNull
                        ?: value.content
                }
            }
        }
    }

    private fun resolveNestedId(existing: String, regenerateIds: Boolean): String {
        if (regenerateIds) return UUID.randomUUID().toString()
        if (existing.isNotBlank()) return existing
        return UUID.randomUUID().toString()
    }

    private fun parseDate(rawValue: Any?): Date {
        return when (rawValue) {
            is Date -> rawValue
            is Timestamp -> rawValue.toDate()
            is Number -> Date(rawValue.toLong())
            is String -> {
                rawValue.toLongOrNull()?.let { Date(it) } ?: Date()
            }

            else -> Date()
        }
    }

    private fun parseDateOrNull(rawValue: Any?): Date? {
        if (rawValue == null) return null
        return when (rawValue) {
            is Date -> rawValue
            is Timestamp -> rawValue.toDate()
            is Number -> Date(rawValue.toLong())
            is String -> rawValue.toLongOrNull()?.let(::Date)
            else -> null
        }
    }

    private fun asMap(value: Any?): Map<String, Any?>? {
        if (value !is Map<*, *>) return null
        return value.entries.associate { (key, item) ->
            key.toString() to normalizeNestedValue(item)
        }
    }

    private fun asList(value: Any?): List<Any?> {
        return if (value is List<*>) {
            value.map { normalizeNestedValue(it) }
        } else {
            emptyList()
        }
    }

    private fun normalizeNestedValue(value: Any?): Any? {
        return when (value) {
            is Map<*, *> -> asMap(value)
            is List<*> -> value.map { normalizeNestedValue(it) }
            is JsonElement -> jsonElementToValue(value)

            else -> value
        }
    }

    private fun readString(rawMap: Map<String, Any?>, key: String): String {
        return readNullableString(rawMap[key]) ?: ""
    }

    private fun readNullableString(value: Any?): String? {
        return when (value) {
            null -> null
            is String -> value
            else -> value.toString()
        }
    }

    private fun readBoolean(rawMap: Map<String, Any?>, vararg keys: String): Boolean {
        keys.forEach { key ->
            if (rawMap.containsKey(key)) {
                return parseBoolean(rawMap[key])
            }
        }
        return false
    }

    private fun parseBoolean(value: Any?): Boolean {
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> value.equals("true", ignoreCase = true) || value == "1"
            else -> false
        }
    }

    private fun readInt(rawMap: Map<String, Any?>, key: String): Int? {
        return when (val value = rawMap[key]) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }

    private fun readLong(rawMap: Map<String, Any?>, key: String): Long? {
        return when (val value = rawMap[key]) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }
    }
}
