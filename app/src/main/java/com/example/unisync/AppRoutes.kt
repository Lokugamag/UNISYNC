package com.example.unisync

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavBackStack
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.serialization.Serializable

val LocalNavBackStack = staticCompositionLocalOf<NavBackStack<NavKey>?> { null }

sealed interface AppRoute : NavKey {
    @Serializable data object Splash : AppRoute
    @Serializable data object Login : AppRoute
    @Serializable data object Register : AppRoute
    @Serializable data object ForgotPassword : AppRoute
    @Serializable data object Home : AppRoute
    @Serializable data object Profile : AppRoute
    @Serializable data object Notifications : AppRoute

    // Notes Hub
    @Serializable data object NotesList : AppRoute // Main entry (Hub 1)
    @Serializable data object ManageNotes : AppRoute // Manage menu (Hub 2)
    @Serializable data class CreateNotes(val section: String? = null) : AppRoute // Template picker (Hub 3 & 4)
    @Serializable data class CreateDesign(val designType: String, val noteId: String? = null) : AppRoute // Document editor canvas (Hub 5)
    @Serializable data object UploadNotes : AppRoute // Upload image/doc (Hub 7)
    @Serializable data object EditNotes : AppRoute // Edit/Delete list (Hub 8)
    @Serializable data object ViewNotes : AppRoute // View menu (Hub 9)
    @Serializable data object MyNotes : AppRoute // My notes list (Hub 10)
    @Serializable data class NoteDetail(val noteId: String) : AppRoute // Detail view (Hub 11-14)
    @Serializable data class EditNote(val noteId: String) : AppRoute // Edit a specific note details
    @Serializable data object SavedNotes : AppRoute // Saved notes list (Hub 15)

    // Lost & Found
    @Serializable data object LostFoundHome : AppRoute // Main Hub 1
    @Serializable data class LostFoundCategories(val type: String) : AppRoute // Category Picker Hub 2
    @Serializable data class CategoryItemList(val type: String, val category: String) : AppRoute // Grid list Hub 3-7
    @Serializable data class AddPost(val preselectedType: String) : AppRoute // Report Hub 8
    @Serializable data class PostDetail(val postId: String) : AppRoute
    @Serializable data class EditPost(val postId: String) : AppRoute
    @Serializable data object MyPosts : AppRoute
}
