package com.example.lifetogether.data.repository

import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.repository.UserRepository
// import com.google.firebase.Firebase
// import com.google.firebase.auth.auth

class UserRepositoryImpl : UserRepository {

    override fun login(user: User) {
//        Firebase.auth.signInWithEmailAndPassword(user.email, user.password)
//            .addOnSuccessListener {
//                val firebaseUser = Firebase.auth.currentUser
// //                if (firebaseUser != null) {
// //
// //                }
//            }
//            .addOnFailureListener {
// //                errorOperations.showError("Wrong username or password")
//            }
    }

    override fun signUp(user: User) {
//        Firebase.auth.createUserWithEmailAndPassword(user.email, user.password)
//            .addOnSuccessListener {
//                val firebaseUser = Firebase.auth.currentUser
// //                if (user != null) {
// //                    userRepository.addUserData(user.uid, username, onSuccess = { onSuccess(it) })
// //                }
//            }
//            .addOnFailureListener {
// //                errorOperations.showError("User already exists")
//            }
    }
}
