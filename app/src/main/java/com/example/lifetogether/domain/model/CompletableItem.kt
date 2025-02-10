package com.example.lifetogether.domain.model

import java.util.Date

interface CompletableItem : Item, Completable {
    override val id: String?
    override val familyId: String
    override val itemName: String
    override var lastUpdated: Date
    override var completed: Boolean
}
