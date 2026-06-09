package com.example.unisync.data

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val studentId: String = "",
    val faculty: String = "",
    val program: String = "",
    val year: String = "",
    val profilePicUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Note(
    val noteId: String = "",
    val title: String = "",
    val topic: String = "",
    val subject: String = "",
    val description: String = "",
    val fileUrl: String = "",
    val fileType: String = "",   // pdf, image, doc
    val fileName: String = "",
    val uploadedBy: String = "",
    val uploaderName: String = "",
    val uploaderStudentId: String = "",
    val content: String = "",               // Rich text JSON for design notes
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class LostFoundPost(
    val postId: String = "",
    val itemName: String = "",
    val category: String = "",
    val type: String = "LOST",      // LOST or FOUND
    val description: String = "",
    val location: String = "",
    val timePeriod: String = "",
    val imageUrl: String = "",
    val status: String = "ACTIVE",  // ACTIVE or RESOLVED
    val postedBy: String = "",
    val posterName: String = "",
    val posterStudentId: String = "",
    val posterContact: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
