package com.example.unisync

import androidx.compose.runtime.*
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.unisync.data.DataRepository
import com.example.unisync.data.Note
import com.example.unisync.ui.auth.LoginScreen
import com.example.unisync.ui.auth.RegisterScreen
import com.example.unisync.ui.auth.ForgotPasswordScreen
import androidx.compose.runtime.CompositionLocalProvider
import com.example.unisync.ui.home.HomeScreen
import com.example.unisync.ui.home.NotificationsScreen
import com.example.unisync.ui.lostfound.*
import com.example.unisync.ui.notes.*
import com.example.unisync.ui.profile.ProfileScreen
import com.example.unisync.ui.splash.SplashScreen

@Composable
fun UniSyncNavigation() {
    val startDest: AppRoute = if (DataRepository.isLoggedIn) AppRoute.Home else AppRoute.Splash
    val backStack = rememberNavBackStack(startDest)

    CompositionLocalProvider(LocalNavBackStack provides backStack) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = { key ->
            when (key) {
                is AppRoute.Splash -> NavEntry(key) {
                    SplashScreen(onSplashFinished = {
                        val dest: AppRoute = if (DataRepository.isLoggedIn) AppRoute.Home else AppRoute.Login
                        backStack.clear()
                        backStack.add(dest)
                    })
                }
                is AppRoute.Login -> NavEntry(key) {
                    LoginScreen(
                        onLoginSuccess = {
                            backStack.clear()
                            backStack.add(AppRoute.Home)
                        },
                        onNavigateToRegister = { backStack.add(AppRoute.Register) },
                        onNavigateToForgotPassword = { backStack.add(AppRoute.ForgotPassword) }
                    )
                }
                is AppRoute.Register -> NavEntry(key) {
                    RegisterScreen(
                        onRegisterSuccess = {
                            backStack.clear()
                            backStack.add(AppRoute.Home)
                        },
                        onNavigateToLogin = { backStack.removeLastOrNull() }
                    )
                }
                is AppRoute.ForgotPassword -> NavEntry(key) {
                    ForgotPasswordScreen(
                        onSuccess = {
                            backStack.removeLastOrNull()
                        },
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                is AppRoute.Home -> NavEntry(key) {
                    HomeScreen(
                        onNavigateToNotes = { backStack.add(AppRoute.NotesList) },
                        onNavigateToLostFound = { backStack.add(AppRoute.LostFoundHome) },
                        onNavigateToProfile = { backStack.add(AppRoute.Profile) },
                        onLogout = {
                            DataRepository.logout()
                            backStack.clear()
                            backStack.add(AppRoute.Login)
                        },
                        onAddClick = { backStack.add(AppRoute.CreateNotes()) }
                    )
                }
                is AppRoute.Profile -> NavEntry(key) {
                    ProfileScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onLogout = {
                            backStack.clear()
                            backStack.add(AppRoute.Login)
                        }
                    )
                }

                // ── Notes Hub ──────────────────────────────────────────────────
                is AppRoute.NotesList -> NavEntry(key) {
                    NotesListScreen(
                        onNavigateToManageNotes = { backStack.add(AppRoute.ManageNotes) },
                        onNavigateToViewNotes = { backStack.add(AppRoute.ViewNotes) },
                        onBack = { backStack.removeLastOrNull() },
                        onAddClick = { backStack.add(AppRoute.CreateNotes()) }
                    )
                }
                is AppRoute.ManageNotes -> NavEntry(key) {
                    ManageNotesScreen(
                        onCreateNotes = { backStack.add(AppRoute.CreateNotes()) },
                        onUploadNotes = { backStack.add(AppRoute.UploadNotes) },
                        onEditNotes = { backStack.add(AppRoute.EditNotes) },
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                is AppRoute.ViewNotes -> NavEntry(key) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    ViewNotesScreen(
                        onMyNotes = { backStack.add(AppRoute.MyNotes) },
                        onSavedNotes = { backStack.add(AppRoute.SavedNotes) },
                        onChooseFromInternet = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"))
                            context.startActivity(intent)
                        },
                        onBack = { backStack.removeLastOrNull() },
                        onAddClick = { backStack.add(AppRoute.CreateNotes()) }
                    )
                }
                is AppRoute.CreateNotes -> NavEntry(key) {
                    CreateNotesScreen(
                        initialSection = key.section,
                        onCreateDesignClick = { designType -> backStack.add(AppRoute.CreateDesign(designType)) },
                        onNoteClick = { id -> backStack.add(AppRoute.NoteDetail(id)) },
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                is AppRoute.CreateDesign -> NavEntry(key) {
                    CreateDesignScreen(
                        designType = key.designType,
                        noteId = key.noteId,
                        onSuccess = { backStack.removeLastOrNull() },
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                is AppRoute.UploadNotes -> NavEntry(key) {
                    UploadNotesScreen(
                        onSuccess = { backStack.removeLastOrNull() },
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                is AppRoute.EditNotes -> NavEntry(key) {
                    EditNotesScreen(
                        onEditClick = { note ->
                            if (note.fileUrl.startsWith("design:")) {
                                val designType = note.fileUrl.substringAfter("design:")
                                backStack.add(AppRoute.CreateDesign(designType, note.noteId))
                            } else {
                                backStack.add(AppRoute.EditNote(note.noteId))
                            }
                        },
                        onNoteClick = { id -> backStack.add(AppRoute.NoteDetail(id)) },
                        onBack = { backStack.removeLastOrNull() },
                        onHomeClick = {
                            backStack.clear()
                            backStack.add(AppRoute.Home)
                        },
                        onAddClick = {
                            backStack.add(AppRoute.CreateNotes())
                        }
                    )
                }
                is AppRoute.MyNotes -> NavEntry(key) {
                    MyNotesGridScreen(
                        onNoteClick = { id -> backStack.add(AppRoute.NoteDetail(id)) },
                        onBack = { backStack.removeLastOrNull() },
                        onHomeClick = {
                            backStack.clear()
                            backStack.add(AppRoute.Home)
                        },
                        onAddClick = {
                            backStack.add(AppRoute.CreateNotes())
                        }
                    )
                }
                is AppRoute.SavedNotes -> NavEntry(key) {
                    SavedNotesScreen(
                        onNoteClick = { id -> backStack.add(AppRoute.NoteDetail(id)) },
                        onBack = { backStack.removeLastOrNull() },
                        onHomeClick = {
                            backStack.clear()
                            backStack.add(AppRoute.Home)
                        },
                        onAddClick = {
                            backStack.add(AppRoute.CreateNotes())
                        }
                    )
                }
                is AppRoute.NoteDetail -> NavEntry(key) {
                    NoteDetailScreen(
                        noteId = key.noteId,
                        onBack = { backStack.removeLastOrNull() },
                        onDeleted = { backStack.removeLastOrNull() },
                        onEditDesignClick = { noteId, designType ->
                            backStack.add(AppRoute.CreateDesign(designType, noteId))
                        },
                        onHomeClick = {
                            backStack.clear()
                            backStack.add(AppRoute.Home)
                        },
                        onAddClick = {
                            backStack.add(AppRoute.CreateNotes())
                        }
                    )
                }
                is AppRoute.EditNote -> NavEntry(key) {
                    EditNoteScreen(
                        noteId = key.noteId,
                        onSuccess = { backStack.removeLastOrNull() },
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                // ── Lost & Found ───────────────────────────────────────────────
                is AppRoute.LostFoundHome -> NavEntry(key) {
                    LostFoundHomeScreen(
                        onViewLost = { backStack.add(AppRoute.LostFoundCategories("LOST")) },
                        onViewFound = { backStack.add(AppRoute.LostFoundCategories("FOUND")) },
                        onReportLost = { backStack.add(AppRoute.AddPost("LOST")) },
                        onReportFound = { backStack.add(AppRoute.AddPost("FOUND")) },
                        onMyPosts = { backStack.add(AppRoute.MyPosts) },
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                is AppRoute.LostFoundCategories -> NavEntry(key) {
                    LostFoundCategoriesScreen(
                        type = key.type,
                        onCategoryClick = { catName -> backStack.add(AppRoute.CategoryItemList(key.type, catName)) },
                        onBack = { backStack.removeLastOrNull() },
                        onHomeClick = {
                            backStack.clear()
                            backStack.add(AppRoute.Home)
                        },
                        onAddClick = {
                            backStack.add(AppRoute.CreateNotes())
                        }
                    )
                }
                is AppRoute.CategoryItemList -> NavEntry(key) {
                    CategoryItemListScreen(
                        type = key.type,
                        category = key.category,
                        onEditPost = { postId -> backStack.add(AppRoute.EditPost(postId)) },
                        onBack = { backStack.removeLastOrNull() },
                        onHomeClick = {
                            backStack.clear()
                            backStack.add(AppRoute.Home)
                        },
                        onAddClick = {
                            backStack.add(AppRoute.CreateNotes())
                        }
                    )
                }
                is AppRoute.AddPost -> NavEntry(key) {
                    AddPostScreen(
                        preselectedType = key.preselectedType,
                        onSuccess = { backStack.removeLastOrNull() },
                        onBack = { backStack.removeLastOrNull() },
                        onHomeClick = {
                            backStack.clear()
                            backStack.add(AppRoute.Home)
                        },
                        onAddClick = {
                            backStack.add(AppRoute.CreateNotes())
                        }
                    )
                }
                is AppRoute.PostDetail -> NavEntry(key) {
                    PostDetailScreen(
                        postId = key.postId,
                        onEdit = { id -> backStack.add(AppRoute.EditPost(id)) },
                        onBack = { backStack.removeLastOrNull() },
                        onDeleted = { backStack.removeLastOrNull() }
                    )
                }
                is AppRoute.EditPost -> NavEntry(key) {
                    EditPostScreen(
                        postId = key.postId,
                        onSuccess = { backStack.removeLastOrNull() },
                        onBack = { backStack.removeLastOrNull() },
                        onHomeClick = {
                            backStack.clear()
                            backStack.add(AppRoute.Home)
                        },
                        onAddClick = {
                            backStack.add(AppRoute.CreateNotes())
                        }
                    )
                }
                is AppRoute.MyPosts -> NavEntry(key) {
                    MyPostsScreen(
                        onPostClick = { id -> backStack.add(AppRoute.PostDetail(id)) },
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                is AppRoute.Notifications -> NavEntry(key) {
                    NotificationsScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                else -> NavEntry(AppRoute.Login as AppRoute) {
                    LoginScreen(
                        onLoginSuccess = { backStack.clear(); backStack.add(AppRoute.Home) },
                        onNavigateToRegister = { backStack.add(AppRoute.Register) },
                        onNavigateToForgotPassword = { backStack.add(AppRoute.ForgotPassword) }
                    )
                }
            }
        }
    )
    }
}
