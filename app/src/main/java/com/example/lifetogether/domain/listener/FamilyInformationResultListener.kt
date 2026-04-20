package com.example.lifetogether.domain.listener

import com.example.lifetogether.domain.model.family.FamilyInformation

sealed class FamilyInformationResultListener {
    data class Success(val familyInformation: FamilyInformation) : FamilyInformationResultListener()
    data class Failure(val message: String) : FamilyInformationResultListener()
}
