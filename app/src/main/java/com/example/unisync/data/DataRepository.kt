package com.example.unisync.data

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object DataRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // ─── AUTH ─────────────────────────────────────────────────────────────────

    val currentUserId: String? get() = auth.currentUser?.uid
    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await()
        Unit
    }

    suspend fun register(email: String, password: String, name: String, studentId: String): Result<Unit> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user!!.uid
        val user = User(uid = uid, name = name, email = email, studentId = studentId)
        db.collection("users").document(uid).set(user).await()
        Unit
    }

    /** Real Google Sign-In: pass the idToken obtained from CredentialManager */
    suspend fun loginWithGoogle(idToken: String): Result<Unit> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val uid = result.user!!.uid
        val isNew = result.additionalUserInfo?.isNewUser == true
        if (isNew) {
            val name = result.user?.displayName ?: "Google User"
            val email = result.user?.email ?: ""
            val user = User(uid = uid, name = name, email = email, studentId = "")
            db.collection("users").document(uid).set(user).await()
        }
        Unit
    }

    fun logout() = auth.signOut()

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
        Unit
    }

    suspend fun populateSampleNotesIfEmpty(userId: String, userName: String): Result<Unit> = runCatching {
        // Delete all user-made notes (whose IDs do not contain "_sample_")
        val notesSnapshot = db.collection("notes").get().await()
        for (doc in notesSnapshot.documents) {
            val noteId = doc.id
            if (!noteId.contains("_sample_")) {
                db.collection("notes").document(noteId).delete().await()
                db.collection("users").document(userId).collection("savedNotes").document(noteId).delete().await()
            }
        }

        val hasSample = db.collection("notes").document("${userId}_sample_periodic_table").get().await().exists()
        if (!hasSample) {
            val sample1 = Note(
                noteId = "${userId}_sample_periodic_table",
                title = "Periodic Table",
                subject = "Chemistry",
                topic = "Elements",
                description = "Handwritten periodic table study notes detailing groups, periods, trends, and transition elements.",
                fileUrl = "android.resource://com.example.unisync/drawable/note_periodic_table",
                fileType = "image",
                fileName = "note_periodic_table.png",
                uploadedBy = userId,
                uploaderName = userName,
                createdAt = System.currentTimeMillis() - 86400000 * 3,
                updatedAt = System.currentTimeMillis() - 86400000 * 3
            )
            val sample2 = Note(
                noteId = "${userId}_sample_graphs",
                title = "Graphs",
                subject = "Mathematics",
                topic = "Calculus",
                description = "Handwritten calculus notes on plotting linear and quadratic functions, coordinate systems, and limits.",
                fileUrl = "android.resource://com.example.unisync/drawable/note_graphs",
                fileType = "image",
                fileName = "note_graphs.png",
                uploadedBy = userId,
                uploaderName = userName,
                createdAt = System.currentTimeMillis() - 86400000 * 2,
                updatedAt = System.currentTimeMillis() - 86400000 * 2
            )
            val sample3 = Note(
                noteId = "${userId}_sample_mind_map",
                title = "Mind Map",
                subject = "Computer Networks",
                topic = "TCP/IP Layer",
                description = "A colorful, visual mind map explaining network layers, DNS, routers, and application protocols.",
                fileUrl = "android.resource://com.example.unisync/drawable/note_mind_map",
                fileType = "image",
                fileName = "note_mind_map.png",
                uploadedBy = userId,
                uploaderName = userName,
                createdAt = System.currentTimeMillis() - 86400000,
                updatedAt = System.currentTimeMillis() - 86400000
            )
            val sample4 = Note(
                noteId = "${userId}_sample_final_note",
                title = "Final Note",
                subject = "Lecture Skills",
                topic = "Note Taking",
                description = "A guide on active note-taking, featuring Cornell notes, outlines, and review strategies.",
                fileUrl = "android.resource://com.example.unisync/drawable/note_final_note",
                fileType = "image",
                fileName = "note_final_note.png",
                uploadedBy = userId,
                uploaderName = userName,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            db.collection("notes").document(sample1.noteId).set(sample1).await()
            db.collection("notes").document(sample2.noteId).set(sample2).await()
            db.collection("notes").document(sample3.noteId).set(sample3).await()
            db.collection("notes").document(sample4.noteId).set(sample4).await()

            // Auto-favorite the sample notes for this user
            toggleSaveNote(userId, sample1.noteId)
            toggleSaveNote(userId, sample2.noteId)
            toggleSaveNote(userId, sample3.noteId)
            toggleSaveNote(userId, sample4.noteId)
        }
        Unit
    }

    suspend fun populateLostFoundIfEmpty(userId: String, userName: String): Result<Unit> = runCatching {
        val hasSample = db.collection("lostFound").document("${userId}_lf_wallet_gents").get().await().exists()
        if (!hasSample) {
            val now = System.currentTimeMillis()
            val sampleItems = listOf(
                // ─── Stationery & Accessories (LOST) ───
                LostFoundPost(
                    postId = "${userId}_lf_pencil_case_red",
                    itemName = "Red colored pencil case",
                    category = "Stationery & Accessories",
                    type = "LOST",
                    description = "Stuffed with drawing pens and sketch pencils.",
                    location = "Art Room",
                    timePeriod = "Lost 1 hour ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/item_pencil_case",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1234567",
                    createdAt = now - 3600000, // 1 hour ago
                    updatedAt = now - 3600000
                ),
                LostFoundPost(
                    postId = "${userId}_lf_journal_black",
                    itemName = "Black colored journal book",
                    category = "Stationery & Accessories",
                    type = "LOST",
                    description = "A thin black notebook with coordinate graph papers inside.",
                    location = "Auditorium",
                    timePeriod = "Lost 2 days ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/cat_stationery",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1234567",
                    createdAt = now - 2 * 86400000, // 2 days ago
                    updatedAt = now - 2 * 86400000
                ),
                LostFoundPost(
                    postId = "${userId}_lf_water_bottle_glass",
                    itemName = "Glass water bottle",
                    category = "Stationery & Accessories",
                    type = "LOST",
                    description = "Clear glass bottle with wooden cap.",
                    location = "Cafeteria",
                    timePeriod = "Lost 2 days ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/item_water_bottle",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1234567",
                    createdAt = now - 2 * 86400000 - 60000, // 2 days and 1 min ago
                    updatedAt = now - 2 * 86400000 - 60000
                ),
                LostFoundPost(
                    postId = "${userId}_lf_keychain_anime",
                    itemName = "Anime key-chain",
                    category = "Stationery & Accessories",
                    type = "LOST",
                    description = "Cute acrylic anime keychain.",
                    location = "Gate",
                    timePeriod = "Lost 5 days ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/cat_stationery",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1234567",
                    createdAt = now - 5 * 86400000, // 5 days ago
                    updatedAt = now - 5 * 86400000
                ),
                LostFoundPost(
                    postId = "${userId}_lf_pencil_case_purple",
                    itemName = "Purple colored pencil case",
                    category = "Stationery & Accessories",
                    type = "LOST",
                    description = "Beautiful lilac case.",
                    imageUrl = "android.resource://com.example.unisync/drawable/item_pencil_case",
                    location = "First Floor",
                    timePeriod = "Lost 6 days ago",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1234567",
                    createdAt = now - 6 * 86400000, // 6 days ago
                    updatedAt = now - 6 * 86400000
                ),
                LostFoundPost(
                    postId = "${userId}_lf_steel_bottle_green",
                    itemName = "Green colored steel bottle",
                    category = "Stationery & Accessories",
                    type = "LOST",
                    description = "Green steel flask.",
                    imageUrl = "android.resource://com.example.unisync/drawable/item_water_bottle",
                    location = "Auditorium",
                    timePeriod = "Lost 6 days ago",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1234567",
                    createdAt = now - 6 * 86400000 - 60000, // 6 days and 1 min ago
                    updatedAt = now - 6 * 86400000 - 60000
                ),
                LostFoundPost(
                    postId = "${userId}_lf_backpack_leather",
                    itemName = "Leather backpack",
                    category = "Stationery & Accessories",
                    type = "LOST",
                    description = "Brown leather school backpack.",
                    location = "Cafeteria",
                    timePeriod = "Lost a week ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/item_backpack",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1234567",
                    createdAt = now - 7 * 86400000, // a week ago
                    updatedAt = now - 7 * 86400000
                ),
                LostFoundPost(
                    postId = "${userId}_lf_tumbler_violet",
                    itemName = "Violet mist colored tumbler",
                    category = "Stationery & Accessories",
                    type = "LOST",
                    description = "Violet mist ombre steel tumbler.",
                    imageUrl = "android.resource://com.example.unisync/drawable/item_water_bottle",
                    location = "Gate",
                    timePeriod = "Lost a week ago",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1234567",
                    createdAt = now - 7 * 86400000 - 60000, // a week and 1 min ago
                    updatedAt = now - 7 * 86400000 - 60000
                ),

                // ─── Electronics (LOST) ───
                LostFoundPost(
                    postId = "${userId}_lf_phone_s24",
                    itemName = "Samsung S24 mobile phone",
                    category = "Electronics",
                    type = "LOST",
                    description = "Titanium gray phone, no case.",
                    location = "IoT Lab",
                    timePeriod = "Lost 1 day ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/cat_electronics",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1234567",
                    createdAt = now - 86400000, // 1 day ago
                    updatedAt = now - 86400000
                ),
                LostFoundPost(
                    postId = "${userId}_lf_charger_laptop",
                    itemName = "Laptop charger",
                    category = "Electronics",
                    type = "LOST",
                    description = "Black power brick.",
                    location = "Auditorium",
                    timePeriod = "Lost 3 days ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/cat_electronics",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1234567",
                    createdAt = now - 3 * 86400000, // 3 days ago
                    updatedAt = now - 3 * 86400000
                ),
                LostFoundPost(
                    postId = "${userId}_lf_smartwatch_blue_lost",
                    itemName = "Blue stripped smart watch",
                    category = "Electronics",
                    type = "LOST",
                    description = "Blue silicone strap smartwatch.",
                    location = "Printing Room",
                    timePeriod = "Lost 5 days ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/item_smartwatch",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1234567",
                    createdAt = now - 5 * 86400000, // 5 days ago
                    updatedAt = now - 5 * 86400000
                ),
                LostFoundPost(
                    postId = "${userId}_lf_headphone_jbl",
                    itemName = "JBL headphone set",
                    category = "Electronics",
                    type = "LOST",
                    description = "Blue JBL over-ear headphones.",
                    location = "Lecture Room",
                    timePeriod = "Lost 5 days ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/cat_electronics",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1234567",
                    createdAt = now - 5 * 86400000 - 60000, // 5 days and 1 min ago
                    updatedAt = now - 5 * 86400000 - 60000
                ),

                // ─── Wallets (LOST) ───
                LostFoundPost(
                    postId = "${userId}_lf_cardholder_gents",
                    itemName = "Mini card holder - Gents",
                    category = "Wallets",
                    type = "LOST",
                    description = "Gents card holder.",
                    location = "Cafeteria",
                    timePeriod = "Lost 5 hours ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/cat_wallets",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1234567",
                    createdAt = now - 5 * 3600000, // 5 hours ago
                    updatedAt = now - 5 * 3600000
                ),
                LostFoundPost(
                    postId = "${userId}_lf_wallet_ladies_brown",
                    itemName = "Ladies brown colored wallet",
                    category = "Wallets",
                    type = "LOST",
                    description = "Brown leather wallet.",
                    location = "Swimming Pool",
                    timePeriod = "Lost 1 day ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/cat_wallets",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1234567",
                    createdAt = now - 86400000, // 1 day ago
                    updatedAt = now - 86400000
                ),
                LostFoundPost(
                    postId = "${userId}_lf_wallet_gents",
                    itemName = "Gents leather wallet",
                    category = "Wallets",
                    type = "LOST",
                    description = "Gents leather wallet.",
                    location = "IoT Lab",
                    timePeriod = "Lost 1 day ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/cat_wallets",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1234567",
                    createdAt = now - 86400000 - 60000, // 1 day and 1 min ago
                    updatedAt = now - 86400000 - 60000
                ),
                LostFoundPost(
                    postId = "${userId}_lf_wallet_ladies_strap",
                    itemName = "Ladies wallet with long strap",
                    category = "Wallets",
                    type = "LOST",
                    description = "Ladies wallet with strap.",
                    location = "Rest Room",
                    timePeriod = "Lost 2 days ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/cat_wallets",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1234567",
                    createdAt = now - 2 * 86400000, // 2 days ago
                    updatedAt = now - 2 * 86400000
                ),

                // ─── Keys (LOST) ───
                LostFoundPost(
                    postId = "${userId}_lf_key_house",
                    itemName = "House door key",
                    category = "Keys",
                    type = "LOST",
                    description = "House door key.",
                    location = "Cafeteria",
                    timePeriod = "Lost 8 hours ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/cat_keys",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1234567",
                    createdAt = now - 8 * 3600000, // 8 hours ago
                    updatedAt = now - 8 * 3600000
                ),
                LostFoundPost(
                    postId = "${userId}_lf_card_door",
                    itemName = "Door key card",
                    category = "Keys",
                    type = "LOST",
                    description = "Door key card.",
                    location = "Playground",
                    timePeriod = "Lost 1 day ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/cat_keys",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1234567",
                    createdAt = now - 86400000, // 1 day ago
                    updatedAt = now - 86400000
                ),
                LostFoundPost(
                    postId = "${userId}_lf_key_bicycle",
                    itemName = "Bicycle key",
                    category = "Keys",
                    type = "LOST",
                    description = "Bicycle key.",
                    location = "Gate",
                    timePeriod = "Lost 1 day ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/cat_keys",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1234567",
                    createdAt = now - 86400000 - 60000, // 1 day and 1 min ago
                    updatedAt = now - 86400000 - 60000
                ),
                LostFoundPost(
                    postId = "${userId}_lf_key_house_2",
                    itemName = "House door key",
                    category = "Keys",
                    type = "LOST",
                    description = "House door key.",
                    location = "Lecture Room",
                    timePeriod = "Lost 2 days ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/cat_keys",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1234567",
                    createdAt = now - 2 * 86400000, // 2 days ago
                    updatedAt = now - 2 * 86400000
                ),

                // ─── Found Items (FOUND) ───
                LostFoundPost(
                    postId = "${userId}_lf_pencil_found",
                    itemName = "Red colored pencil case",
                    category = "Stationery & Accessories",
                    type = "FOUND",
                    description = "Red fabric pencil case found on desk.",
                    location = "Art Room",
                    timePeriod = "Found 30 minutes ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/item_pencil_case",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1589645",
                    createdAt = now - 1800000, // 30 mins ago
                    updatedAt = now - 1800000
                ),
                LostFoundPost(
                    postId = "${userId}_lf_watch_found",
                    itemName = "Blue stripped smart watch",
                    category = "Electronics",
                    type = "FOUND",
                    description = "Found near the cafeteria tables.",
                    location = "Cafeteria",
                    timePeriod = "Found 4 hours ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/item_smartwatch",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1589645",
                    createdAt = now - 4 * 3600000, // 4 hours ago
                    updatedAt = now - 4 * 3600000
                ),
                LostFoundPost(
                    postId = "${userId}_lf_card_found",
                    itemName = "Door key card",
                    category = "Keys",
                    type = "FOUND",
                    description = "White NFC card.",
                    location = "Playground",
                    timePeriod = "Found 7 hours ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/cat_keys",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1589645",
                    createdAt = now - 7 * 3600000, // 7 hours ago
                    updatedAt = now - 7 * 3600000
                ),
                LostFoundPost(
                    postId = "${userId}_lf_tumbler_found",
                    itemName = "Violet mist colored tumbler",
                    category = "Stationery & Accessories",
                    type = "FOUND",
                    description = "Violet mist ombre tumbler found near gate.",
                    location = "Gate",
                    timePeriod = "Found 5 days ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/item_water_bottle",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1589645",
                    createdAt = now - 5 * 86400000, // 5 days ago
                    updatedAt = now - 5 * 86400000
                ),
                // ─── Others (LOST & FOUND) ───
                LostFoundPost(
                    postId = "${userId}_lf_backpack_others_lost",
                    itemName = "Blue canvas backpack",
                    category = "Others",
                    type = "LOST",
                    description = "Contains textbooks and a notebook.",
                    location = "Main Library",
                    timePeriod = "Lost yesterday",
                    imageUrl = "android.resource://com.example.unisync/drawable/item_backpack",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1234567",
                    createdAt = now - 86400000,
                    updatedAt = now - 86400000
                ),
                LostFoundPost(
                    postId = "${userId}_lf_umbrella_others_found",
                    itemName = "Black folding umbrella",
                    category = "Others",
                    type = "FOUND",
                    description = "Found near the main lecture hall.",
                    location = "Lecture Hall 3",
                    timePeriod = "Found 2 hours ago",
                    imageUrl = "android.resource://com.example.unisync/drawable/item_backpack",
                    status = "ACTIVE",
                    postedBy = userId,
                    posterName = userName,
                    posterContact = "071-1589645",
                    createdAt = now - 7200000,
                    updatedAt = now - 7200000
                )
            )

            for (item in sampleItems) {
                db.collection("lostFound").document(item.postId).set(item).await()
            }
        }
        Unit
    }

    // ─── USER PROFILE ─────────────────────────────────────────────────────────

    fun getUserProfile(uid: String): Flow<User?> = callbackFlow {
        val listener = db.collection("users").document(uid)
            .addSnapshotListener { snap, _ ->
                val user = snap?.toObject(User::class.java)
                trySend(user)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateUserProfile(
        uid: String,
        name: String,
        studentId: String,
        faculty: String = "",
        program: String = "",
        year: String = ""
    ): Result<Unit> = runCatching {
        db.collection("users").document(uid)
            .update(
                "name", name,
                "studentId", studentId,
                "faculty", faculty,
                "program", program,
                "year", year
            ).await()
        Unit
    }

    // ─── FILE UPLOAD ──────────────────────────────────────────────────────────

    suspend fun uploadFile(uri: Uri, path: String): Result<String> = runCatching {
        val ref = storage.reference.child(path)
        ref.putFile(uri).await()
        ref.downloadUrl.await().toString()
    }

    // ─── NOTES ────────────────────────────────────────────────────────────────

    fun getAllNotes(): Flow<List<Note>> = callbackFlow {
        val listener = db.collection("notes")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val notes = snap?.documents?.mapNotNull { it.toObject(Note::class.java) } ?: emptyList()
                trySend(notes)
            }
        awaitClose { listener.remove() }
    }

    fun getMyNotes(uid: String): Flow<List<Note>> = callbackFlow {
        val listener = db.collection("notes")
            .whereEqualTo("uploadedBy", uid)
            .addSnapshotListener { snap, _ ->
                val notes = snap?.documents?.mapNotNull { it.toObject(Note::class.java) } ?: emptyList()
                trySend(notes.sortedByDescending { it.updatedAt })
            }
        awaitClose { listener.remove() }
    }

    fun getSavedNotes(uid: String): Flow<List<Note>> = callbackFlow {
        val listener = db.collection("users").document(uid)
            .collection("savedNotes")
            .orderBy("savedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val noteIds = snap?.documents?.map { it.id } ?: emptyList()
                if (noteIds.isEmpty()) { trySend(emptyList()); return@addSnapshotListener }
                // Fetch the actual notes
                db.collection("notes").whereIn("noteId", noteIds.take(10))
                    .get()
                    .addOnSuccessListener { notesSnap ->
                        val notes = notesSnap.documents.mapNotNull { it.toObject(Note::class.java) }
                        trySend(notes)
                    }
            }
        awaitClose { listener.remove() }
    }

    suspend fun toggleSaveNote(uid: String, noteId: String): Result<Boolean> = runCatching {
        val ref = db.collection("users").document(uid).collection("savedNotes").document(noteId)
        val exists = ref.get().await().exists()
        if (exists) {
            ref.delete().await()
            false // removed
        } else {
            ref.set(mapOf("noteId" to noteId, "savedAt" to System.currentTimeMillis())).await()
            true // saved
        }
    }

    suspend fun isNoteSaved(uid: String, noteId: String): Boolean {
        return db.collection("users").document(uid).collection("savedNotes").document(noteId)
            .get().await().exists()
    }

    fun getNoteById(noteId: String): Flow<Note?> = callbackFlow {
        val listener = db.collection("notes").document(noteId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObject(Note::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun createNote(context: Context, note: Note, fileUri: Uri?, uploaderName: String, uploaderStudentId: String): Result<Unit> = runCatching {
        val docRef = db.collection("notes").document()
        var fileUrl = ""
        var fileName = note.fileName.ifEmpty { "file" }
        var fileType = note.fileType.ifEmpty { "doc" }
        if (fileUri != null) {
            if (fileUri.scheme == "content") {
                val mimeType = context.contentResolver.getType(fileUri)
                fileType = when {
                    mimeType?.contains("pdf", true) == true -> "pdf"
                    mimeType?.contains("image", true) == true -> "image"
                    else -> "doc"
                }
                val cursor = context.contentResolver.query(fileUri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1) {
                        fileName = cursor.getString(nameIdx)
                    }
                }
                cursor?.close()
            } else {
                val seg = fileUri.lastPathSegment ?: "file"
                val ext = seg.substringAfterLast('.', "")
                fileName = seg
                fileType = when {
                    ext.equals("pdf", true) -> "pdf"
                    ext.equals("jpg", true) || ext.equals("jpeg", true) || ext.equals("png", true) -> "image"
                    else -> "doc"
                }
            }
            if (fileName.isBlank()) {
                fileName = "uploaded_file"
            }
            val uploadResult = uploadFile(fileUri, "notes/${docRef.id}/$fileName")
            fileUrl = uploadResult.getOrNull() ?: fileUri.toString()
        }
        val newNote = note.copy(
            noteId = docRef.id,
            fileUrl = if (fileUri != null) fileUrl else note.fileUrl,
            fileName = if (fileUri != null) fileName else note.fileName,
            fileType = if (fileUri != null) fileType else note.fileType,
            uploadedBy = currentUserId ?: "",
            uploaderName = uploaderName,
            uploaderStudentId = uploaderStudentId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        docRef.set(newNote).await()
        // Automatically add to current user's savedNotes (Favorites)
        val uid = currentUserId
        if (uid != null) {
            db.collection("users").document(uid)
                .collection("savedNotes").document(docRef.id)
                .set(mapOf("noteId" to docRef.id, "savedAt" to System.currentTimeMillis())).await()
        }
        Unit
    }

    suspend fun updateNote(noteId: String, title: String, topic: String, subject: String, description: String, newFileUri: Uri?): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any>(
            "title" to title,
            "topic" to topic,
            "subject" to subject,
            "description" to description,
            "updatedAt" to System.currentTimeMillis()
        )
        if (newFileUri != null) {
            val seg = newFileUri.lastPathSegment ?: "file"
            val ext = seg.substringAfterLast('.', "file")
            val fileName = seg
            val fileType = when {
                ext.equals("pdf", true) -> "pdf"
                ext.equals("jpg", true) || ext.equals("jpeg", true) || ext.equals("png", true) -> "image"
                else -> "doc"
            }
            val fileUrl = uploadFile(newFileUri, "notes/$noteId/$fileName").getOrNull() ?: newFileUri.toString()
            updates["fileUrl"] = fileUrl
            updates["fileName"] = fileName
            updates["fileType"] = fileType
        }
        db.collection("notes").document(noteId).update(updates).await()
        Unit
    }

    suspend fun deleteNote(noteId: String): Result<Unit> = runCatching {
        db.collection("notes").document(noteId).delete().await()
        // Automatically remove from user's savedNotes (Favorites)
        val uid = currentUserId
        if (uid != null) {
            db.collection("users").document(uid)
                .collection("savedNotes").document(noteId)
                .delete().await()
        }
        Unit
    }

    // Create a design note document directly (preserves fileUrl for "design:TYPE" notes)
    suspend fun createDesignNote(note: Note): Result<Unit> = runCatching {
        val newNote = note.copy(
            uploadedBy = currentUserId ?: "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        db.collection("notes").document(note.noteId).set(newNote).await()
        // Automatically add to current user's savedNotes (Favorites)
        val uid = currentUserId
        if (uid != null) {
            db.collection("users").document(uid)
                .collection("savedNotes").document(note.noteId)
                .set(mapOf("noteId" to note.noteId, "savedAt" to System.currentTimeMillis())).await()
        }
        Unit
    }

    // Auto-save design document content (rich text JSON + title)
    suspend fun updateDocumentContent(
        noteId: String,
        content: String,
        title: String
    ): Result<Unit> = runCatching {
        db.collection("notes").document(noteId)
            .update(
                mapOf(
                    "content" to content,
                    "title" to title,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
        Unit
    }

    // ─── LOST & FOUND ─────────────────────────────────────────────────────────

    fun getLostItems(): Flow<List<LostFoundPost>> = callbackFlow {
        val listener = db.collection("lostFound")
            .whereEqualTo("type", "LOST")
            .addSnapshotListener { snap, _ ->
                val posts = snap?.documents?.mapNotNull { it.toObject(LostFoundPost::class.java) } ?: emptyList()
                trySend(posts.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    fun getFoundItems(): Flow<List<LostFoundPost>> = callbackFlow {
        val listener = db.collection("lostFound")
            .whereEqualTo("type", "FOUND")
            .addSnapshotListener { snap, _ ->
                val posts = snap?.documents?.mapNotNull { it.toObject(LostFoundPost::class.java) } ?: emptyList()
                trySend(posts.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    fun getLostItemsByCategory(category: String): Flow<List<LostFoundPost>> = callbackFlow {
        val query = if (category == "All") {
            db.collection("lostFound")
        } else {
            db.collection("lostFound")
                .whereEqualTo("category", category)
        }
        val listener = query.addSnapshotListener { snap, _ ->
            val posts = snap?.documents?.mapNotNull { it.toObject(LostFoundPost::class.java) } ?: emptyList()
            trySend(posts.sortedByDescending { it.createdAt })
        }
        awaitClose { listener.remove() }
    }

    fun getMyPosts(uid: String): Flow<List<LostFoundPost>> = callbackFlow {
        val listener = db.collection("lostFound")
            .whereEqualTo("postedBy", uid)
            .addSnapshotListener { snap, _ ->
                val posts = snap?.documents?.mapNotNull { it.toObject(LostFoundPost::class.java) } ?: emptyList()
                trySend(posts.sortedByDescending { it.createdAt })
            }
        awaitClose { listener.remove() }
    }

    fun getPostById(postId: String): Flow<LostFoundPost?> = callbackFlow {
        val listener = db.collection("lostFound").document(postId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObject(LostFoundPost::class.java))
            }
        awaitClose { listener.remove() }
    }

    suspend fun createPost(post: LostFoundPost, imageUri: Uri?, posterName: String, posterStudentId: String, posterContact: String): Result<Unit> = runCatching {
        val docRef = db.collection("lostFound").document()
        var imageUrl = ""
        if (imageUri != null) {
            imageUrl = uploadFile(imageUri, "lostFound/${docRef.id}/image.jpg").getOrNull() ?: imageUri.toString()
        }
        val newPost = post.copy(
            postId = docRef.id,
            imageUrl = imageUrl,
            postedBy = currentUserId ?: "",
            posterName = posterName,
            posterStudentId = posterStudentId,
            posterContact = posterContact,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        docRef.set(newPost).await()
        Unit
    }

    suspend fun updatePost(postId: String, itemName: String, category: String, type: String, description: String, location: String, timePeriod: String, status: String, newImageUri: Uri?): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any>(
            "itemName" to itemName,
            "category" to category,
            "type" to type,
            "description" to description,
            "location" to location,
            "timePeriod" to timePeriod,
            "status" to status,
            "updatedAt" to System.currentTimeMillis()
        )
        if (newImageUri != null) {
            val imageUrl = uploadFile(newImageUri, "lostFound/$postId/image.jpg").getOrNull() ?: newImageUri.toString()
            updates["imageUrl"] = imageUrl
        }
        db.collection("lostFound").document(postId).update(updates).await()
        Unit
    }

    suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        db.collection("lostFound").document(postId).delete().await()
        Unit
    }
}
