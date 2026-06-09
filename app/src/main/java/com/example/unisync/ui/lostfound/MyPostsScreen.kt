package com.example.unisync.ui.lostfound

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.unisync.data.DataRepository
import com.example.unisync.theme.*
import com.example.unisync.ui.common.EmptyState
import com.example.unisync.ui.common.UniSyncBackground
import com.example.unisync.ui.common.UniSyncTopBar

@Composable
fun MyPostsScreen(onPostClick: (String) -> Unit, onBack: () -> Unit) {
    val uid = DataRepository.currentUserId ?: ""
    val postsFlow = remember(uid) { DataRepository.getMyPosts(uid) }
    val posts by postsFlow.collectAsState(initial = emptyList())

    UniSyncBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { UniSyncTopBar(title = "My Posts", onBack = onBack) }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(8.dp))
                Text("${posts.size} post${if (posts.size != 1) "s" else ""}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                if (posts.isEmpty()) {
                    EmptyState(message = "You haven't posted anything yet.", icon = Icons.Default.FindInPage)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(posts, key = { it.postId }) { post -> LostFoundCard(post = post, onClick = { onPostClick(post.postId) }) }
                        item { Spacer(Modifier.height(32.dp)) }
                    }
                }
            }
        }
    }
}
