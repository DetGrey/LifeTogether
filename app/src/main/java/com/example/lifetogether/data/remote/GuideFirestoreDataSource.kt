package com.example.lifetogether.data.remote

import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.logic.GuideProgress
import com.example.lifetogether.domain.logic.GuideRoundGrouping
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideProgressState
import com.example.lifetogether.domain.model.guides.GuideResume
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.guides.GuideStep
import com.example.lifetogether.domain.model.guides.GuideStepType
import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.domain.result.ListSnapshot
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.jvm.Transient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class GuideFirestoreDataSource @Inject constructor(
    private val db: FirebaseFirestore,
) {
    private companion object {
        const val TAG = "GuideFirestoreDS"
    }
    fun familySharedGuidesSnapshotListener(familyId: String) = callbackFlow {
        val ref = db.collection(Constants.GUIDES_TABLE)
            .whereEqualTo("familyId", familyId)
            .whereEqualTo("visibility", Constants.VISIBILITY_FAMILY)
        val registration = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot == null) {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
                return@addSnapshotListener
            }
            val guides = mapFirestoreDocuments(
                tag = TAG,
                collectionName = Constants.GUIDES_TABLE,
                entityName = "Guide",
                documents = snapshot.documents,
            ) { document ->
                document.toObject(GuideDto::class.java)?.toDomain(document.id)
            }
            trySend(Result.Success(ListSnapshot(guides))).isSuccess
        }
        awaitClose { registration.remove() }
    }

    fun privateGuidesSnapshotListener(familyId: String, uid: String) = callbackFlow {
        val ref = db.collection(Constants.GUIDES_TABLE)
            .whereEqualTo("familyId", familyId)
            .whereEqualTo("visibility", Constants.VISIBILITY_PRIVATE)
            .whereEqualTo("ownerUid", uid)
        val registration = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot == null) {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
                return@addSnapshotListener
            }
            val guides = mapFirestoreDocuments(
                tag = TAG,
                collectionName = Constants.GUIDES_TABLE,
                entityName = "Guide",
                documents = snapshot.documents,
            ) { document ->
                document.toObject(GuideDto::class.java)?.toDomain(document.id)
            }
            trySend(Result.Success(ListSnapshot(guides))).isSuccess
        }
        awaitClose { registration.remove() }
    }

    fun guideProgressSnapshotListener(familyId: String, uid: String) = callbackFlow {
        val ref = db.collection(Constants.GUIDE_PROGRESS_TABLE)
            .whereEqualTo("familyId", familyId)
            .whereEqualTo("uid", uid)
        val registration = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot == null) {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
                return@addSnapshotListener
            }
            val progress = mapFirestoreDocuments(
                tag = TAG,
                collectionName = Constants.GUIDE_PROGRESS_TABLE,
                entityName = "GuideProgressState",
                documents = snapshot.documents,
            ) { document ->
                document.toObject(GuideProgressDto::class.java)?.toDomain(document.id)
            }
            trySend(Result.Success(progress)).isSuccess
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveGuide(guide: Guide): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.GUIDES_TABLE)
                .document(guide.id)
                .set(guide.toDto().toFirestoreMap())
                .await()
        }
    }

    suspend fun updateGuide(guide: Guide): Result<Unit, AppError> {
        val id = guide.id
        return appResultOfSuspend {
            val upload = guide.toDto().toFirestoreMap()
            db.collection(Constants.GUIDES_TABLE).document(id).set(upload, SetOptions.merge()).await()
        }
    }

    suspend fun deleteGuide(guideId: String): Result<Unit, AppError> {
        return appResultOfSuspend {
            deleteGuideWithRelatedProgress(guideId)
        }
    }

    suspend fun updateGuideProgress(progress: GuideProgressState): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.GUIDE_PROGRESS_TABLE)
                .document(progress.id)
                .set(progress.toDto().toFirestoreMap(), SetOptions.merge())
                .await()
        }
    }

    private suspend fun deleteGuideWithRelatedProgress(guideId: String) {
        db.collection(Constants.GUIDES_TABLE).document(guideId).delete().await()
        appResultOfSuspend {
            val progressRefs = db.collection(Constants.GUIDE_PROGRESS_TABLE)
                .whereEqualTo("guideId", guideId)
                .get()
                .await()
                .documents
                .map { it.reference }
            progressRefs.chunked(450).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { ref -> batch.delete(ref) }
                batch.commit().await()
            }
        }
    }
}

private data class GuideDto(
    @DocumentId @Transient
    val id: String? = null,
    val familyId: String? = null,
    val ownerUid: String? = null,
    val visibility: String? = null,
    val name: String? = null,
    val itemName: String? = null,
    val description: String? = null,
    val contentVersion: Long? = null,
    val lastUpdated: Date? = null,
    val started: Boolean? = null,
    val sections: List<GuideSectionDto>? = null,
    val resume: GuideResumeDto? = null,
) {
    fun toDomain(documentId: String): Guide? {
        val familyIdValue = familyId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val itemNameValue = name?.trim()?.takeIf { it.isNotEmpty() }
            ?: itemName?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null
        val visibilityValue = Visibility.fromValue(visibility) ?: return null
        val ownerUidValue = ownerUid?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val lastUpdatedValue = lastUpdated ?: return null
        val sectionsValue = sections.orEmpty().mapIndexed { index, section ->
            section.toDomain(index)
        }

        return Guide(
            id = documentId,
            familyId = familyIdValue,
            itemName = itemNameValue,
            lastUpdated = lastUpdatedValue,
            description = description?.trim().orEmpty(),
            visibility = visibilityValue,
            ownerUid = ownerUidValue,
            contentVersion = contentVersion ?: 1L,
            sections = sectionsValue,
            started = false,
            resume = null,
        )
    }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "familyId" to familyId,
        "ownerUid" to ownerUid,
        "visibility" to visibility,
        "name" to name,
        "description" to description,
        "contentVersion" to contentVersion,
        "lastUpdated" to lastUpdated,
        "sections" to sections?.map { it.toFirestoreMap() },
    ).filterValues { it != null }
}

private data class GuideSectionDto(
    val id: String? = null,
    val orderNumber: Int? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val pieces: Int? = null,
    val completedPieces: Int? = null,
    val completed: Boolean? = null,
    val comment: String? = null,
    val steps: List<GuideStepDto>? = null,
) {
    fun toDomain(sectionIndex: Int): GuideSection {
        val piecesValue = (pieces ?: 1).coerceAtLeast(1)
        val stepsValue = steps.orEmpty().flatMap { it.toDomain() }

        return GuideProgress.updateSectionCompletion(
            GuideSection()
                .copy(
                    id = resolveNestedId(id),
                    orderNumber = orderNumber ?: (sectionIndex + 1),
                    title = title.orEmpty(),
                    subtitle = subtitle?.trim()?.takeIf { it.isNotEmpty() },
                    pieces = piecesValue,
                    completedPieces = 0,
                    completed = false,
                    comment = comment?.trim()?.takeIf { it.isNotEmpty() },
                    steps = stepsValue,
                    stepsProgressByAmount = emptyList(),
                ),
        )
    }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "orderNumber" to orderNumber,
        "title" to title,
        "subtitle" to subtitle,
        "pieces" to pieces?.coerceAtLeast(1),
        "comment" to comment,
        "steps" to steps?.map { it.toFirestoreMap() },
    ).filterValues { it != null }
}

private data class GuideStepDto(
    val id: String? = null,
    val name: String? = null,
    val type: String? = null,
    val title: String? = null,
    val content: String? = null,
    val completed: Boolean? = null,
    val subSteps: List<GuideStepDto>? = null,
    val steps: List<GuideStepDto>? = null,
) {
    fun toDomain(): List<GuideStep> {
        val stepType = GuideStepType.fromValue(type) ?: return emptyList()
        val nestedSource = when {
            subSteps != null -> subSteps
            steps != null -> steps
            else -> emptyList()
        }
        val nestedSteps = nestedSource.flatMap { it.toDomain() }

        val step = GuideStep(
            id = resolveNestedId(id),
            name = name?.trim().orEmpty(),
            type = stepType,
            title = title?.trim().orEmpty(),
            content = content?.trim().orEmpty(),
            completed = false,
            subSteps = nestedSteps,
        )

        return expandRoundRange(step)
    }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name?.takeIf { it.isNotBlank() },
        "type" to type,
        "title" to title?.takeIf { it.isNotBlank() },
        "content" to content?.takeIf { it.isNotBlank() },
        "steps" to subSteps?.map { it.toFirestoreMap() },
    ).filterValues { it != null }
}

private data class GuideProgressDto(
    @DocumentId @Transient
    val id: String? = null,
    val familyId: String? = null,
    val uid: String? = null,
    val guideId: String? = null,
    val contentVersion: Long? = null,
    val started: Boolean? = null,
    val completedPointerKeys: List<String>? = null,
    val resume: GuideResumeDto? = null,
    val lastUpdated: Date? = null,
    val localUpdatedAt: Date? = null,
    val lastUploadedAt: Date? = null,
) {
    fun toDomain(documentId: String): GuideProgressState? {
        val familyIdValue = familyId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val uidValue = uid?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val guideIdValue = guideId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val lastUpdatedValue = lastUpdated ?: return null
        val localUpdatedAtValue = localUpdatedAt ?: return null

        return GuideProgressState(
            id = documentId,
            familyId = familyIdValue,
            uid = uidValue,
            guideId = guideIdValue,
            contentVersion = contentVersion ?: 1L,
            started = started ?: false,
            completedPointerKeys = completedPointerKeys
                .orEmpty()
                .mapNotNull { value -> value.trim().takeIf { it.isNotEmpty() } }
                .sorted(),
            resume = resume?.toDomain(),
            lastUpdated = lastUpdatedValue,
            pendingSync = false,
            localUpdatedAt = localUpdatedAtValue,
            lastUploadedAt = lastUploadedAt,
        )
    }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "familyId" to familyId,
        "uid" to uid,
        "guideId" to guideId,
        "contentVersion" to contentVersion,
        "started" to started,
        "completedPointerKeys" to completedPointerKeys,
        "resume" to resume?.toFirestoreMap(),
        "lastUpdated" to lastUpdated,
        "localUpdatedAt" to localUpdatedAt,
        "lastUploadedAt" to lastUploadedAt,
    ).filterValues { it != null }
}

private data class GuideResumeDto(
    val sectionIndex: Int? = null,
    val sectionPieceIndex: Int? = null,
    val stepIndex: Int? = null,
    val subStepIndex: Int? = null,
) {
    fun toDomain(): GuideResume = GuideResume(
        sectionIndex = sectionIndex ?: 0,
        sectionPieceIndex = sectionPieceIndex ?: 0,
        stepIndex = stepIndex ?: 0,
        subStepIndex = subStepIndex,
    )

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "sectionIndex" to sectionIndex,
        "sectionPieceIndex" to sectionPieceIndex,
        "stepIndex" to stepIndex,
        "subStepIndex" to subStepIndex,
    ).filterValues { it != null }
}

private fun Guide.toDto(): GuideDto = GuideDto(
    familyId = familyId,
    ownerUid = ownerUid,
    visibility = visibility.value,
    name = itemName,
    description = description,
    contentVersion = contentVersion,
    lastUpdated = lastUpdated,
    sections = sections.map { it.toDto() },
)

private fun GuideSection.toDto(): GuideSectionDto = GuideSectionDto(
    id = id,
    orderNumber = orderNumber,
    title = title,
    subtitle = subtitle,
    pieces = pieces,
    comment = comment,
    steps = contentStepsForSection(this).map { it.toDto() },
)

private fun GuideStep.toDto(): GuideStepDto = GuideStepDto(
    id = id,
    name = name,
    type = type.value,
    title = title,
    content = content,
    subSteps = subSteps.map { it.toDto() },
)

private fun GuideProgressState.toDto(): GuideProgressDto = GuideProgressDto(
    familyId = familyId,
    uid = uid,
    guideId = guideId,
    contentVersion = contentVersion,
    started = started,
    completedPointerKeys = completedPointerKeys,
    resume = resume?.toDto(),
    lastUpdated = lastUpdated,
    localUpdatedAt = localUpdatedAt,
    lastUploadedAt = lastUploadedAt,
)

private fun GuideResume.toDto(): GuideResumeDto = GuideResumeDto(
    sectionIndex = sectionIndex,
    sectionPieceIndex = sectionPieceIndex,
    stepIndex = stepIndex,
    subStepIndex = subStepIndex,
)

private fun contentStepsForSection(section: GuideSection): List<GuideStep> {
    val sourceSteps = when {
        section.steps.isNotEmpty() -> section.steps
        section.stepsProgressByAmount.isNotEmpty() -> section.stepsProgressByAmount.first()
        else -> emptyList()
    }
    return clearStepCompletion(sourceSteps)
}

private fun clearStepCompletion(steps: List<GuideStep>): List<GuideStep> {
    return steps.map { step ->
        step.copy(
            completed = false,
            subSteps = clearStepCompletion(step.subSteps),
        )
    }
}

private fun expandRoundRange(step: GuideStep): List<GuideStep> {
    if (step.type != GuideStepType.ROUND || step.subSteps.isNotEmpty()) return listOf(step)

    val nameOrTitleSpan = GuideRoundGrouping.parseRoundSpan(step.name.ifBlank { step.title })
    if (nameOrTitleSpan != null) {
        return buildRoundStepsFromSpan(
            original = step,
            span = nameOrTitleSpan,
            sharedContent = step.content,
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
            )
        }
    }

    return listOf(step)
}

private fun buildRoundStepsFromSpan(
    original: GuideStep,
    span: IntRange,
    sharedContent: String,
): List<GuideStep> {
    val normalizedContent = sharedContent.trim()
    return span.mapIndexed { index, roundNumber ->
        original.copy(
            id = generateExpandedStepId(original.id, index),
            name = "R$roundNumber",
            title = "",
            content = normalizedContent,
        )
    }
}

private fun generateExpandedStepId(
    baseId: String,
    offset: Int,
): String {
    if (baseId.isBlank()) return UUID.randomUUID().toString()
    return if (offset == 0) baseId else "$baseId-$offset"
}

private fun resolveNestedId(existing: String?): String {
    val normalized = existing?.trim().orEmpty()
    return normalized.ifEmpty { UUID.randomUUID().toString() }
}
