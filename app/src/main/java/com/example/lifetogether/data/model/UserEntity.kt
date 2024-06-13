package com.example.lifetogether.data.model

// @Entity(tableName = "messages")
data class UserEntity(
//    @PrimaryKey(autoGenerate = true) val id: Int,
    val email: String,
    val password: String,
)
