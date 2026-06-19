package com.example.copadex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ======================================================================
// 1. MODELOS DE DADOS
// ======================================================================
data class Sticker(
    val id: String,
    val status: String = "missing", // "missing", "acquired", "repeat"
    val repeatCount: Int = 0,
    val player: String
)

data class Team(
    val id: String,
    val name: String,
    val stickers: List<Sticker>
)

data class Profile(
    val id: Long,
    val name: String
)

data class Stats(
    val percentage: Float,
    val acquired: Int,
    val missing: Int,
    val repeats: Int
)

// ======================================================================
// 2. FUNÇÕES GERADORAS (Simulação da Base de Dados Local)
// ======================================================================
fun generateEmptyAlbum(): List<Team> {
    val teamsInfo = listOf(
        "BRA" to "Brasil", "ARG" to "Argentina", "FRA" to "França",
        "ENG" to "Inglaterra", "ESP" to "Espanha", "GER" to "Alemanha", "POR" to "Portugal"
    )
    return teamsInfo.map { (code, name) ->
        Team(
            id = code.lowercase(),
            name = name,
            stickers = List(20) { i ->
                val idNum = (i + 1).toString().padStart(2, '0')
                Sticker(
                    id = "$code-$idNum",
                    player = if (i == 0) "Escudo Oficial" else "Jogador ${i + 1}"
                )
            }
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(color = Color(0xFFF3F4F6), modifier = Modifier.fillMaxSize()) {
                    CopadexApp()
                }
            }
        }
    }
}

// ======================================================================
// 3. APLICAÇÃO PRINCIPAL E ESTADOS
// ======================================================================
@Composable
fun CopadexApp() {
    var activeTab by remember { mutableStateOf("dashboard") }

    // Estados da Base de Dados Local na Memória (Standalone)
    var profiles by remember { mutableStateOf(listOf(Profile(1L, "Álbum Principal 2026"))) }
    var activeProfileId by remember { mutableLongStateOf(1L) }
    var albumsData by remember { mutableStateOf(mapOf(1L to generateEmptyAlbum())) }

    val currentAlbumData = albumsData[activeProfileId] ?: generateEmptyAlbum()

    // Adicionar ou Remover Figurinhas
    val handleStickerAction = { teamId: String, stickerId: String, isRemoving: Boolean ->
        val updatedAlbum = currentAlbumData.map { team ->
            if (team.id != teamId) team else {
                team.copy(stickers = team.stickers.map { st ->
                    if (st.id != stickerId) st else {
                        if (isRemoving) {
                            // Lógica de Remover (-)
                            when (st.status) {
                                "repeat" -> {
                                    if (st.repeatCount > 1) st.copy(repeatCount = st.repeatCount - 1)
                                    else st.copy(status = "acquired", repeatCount = 0)
                                }
                                "acquired" -> st.copy(status = "missing")
                                else -> st
                            }
                        } else {
                            // Lógica de Adicionar (+)
                            when (st.status) {
                                "missing" -> st.copy(status = "acquired")
                                "acquired" -> st.copy(status = "repeat", repeatCount = 1)
                                "repeat" -> st.copy(repeatCount = st.repeatCount + 1)
                                else -> st
                            }
                        }
                    }
                })
            }
        }
        albumsData = albumsData.toMutableMap().apply { put(activeProfileId, updatedAlbum) }
    }

    // (Dashboard)
    val stats = remember(currentAlbumData) {
        var total = 0; var acquired = 0; var repeats = 0
        currentAlbumData.forEach { team ->
            team.stickers.forEach { st ->
                total++
                if (st.status != "missing") acquired++
                if (st.status == "repeat") repeats += st.repeatCount
            }
        }
        val pct = if (total > 0) (acquired.toFloat() / total.toFloat()) * 100f else 0f
        Stats(pct, acquired, total - acquired, repeats)
    }

    Scaffold(
        bottomBar = { BottomNav(activeTab) { activeTab = it } }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (activeTab) {
                "dashboard" -> DashboardScreen(stats, profiles.find { it.id == activeProfileId }?.name ?: "")
                "album" -> AlbumScreen(currentAlbumData, handleStickerAction)
                "repetidas" -> RepetidasScreen(currentAlbumData, handleStickerAction, stats.repeats)
                "perfil" -> PerfilScreen(
                    profiles, activeProfileId, albumsData,
                    onProfileChange = { activeProfileId = it },
                    onCreateProfile = { name ->
                        val newId = System.currentTimeMillis()
                        profiles = profiles + Profile(newId, name)
                        albumsData = albumsData.toMutableMap().apply { put(newId, generateEmptyAlbum()) }
                        activeProfileId = newId
                    },
                    onZerarAlbum = {
                        albumsData = albumsData.toMutableMap().apply { put(activeProfileId, generateEmptyAlbum()) }
                    }
                )
            }
        }
    }
}

// ======================================================================
// 4. (screens
// ======================================================================

@Composable
fun DashboardScreen(stats: Stats, profileName: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Cabeçalho
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1D4ED8)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(profileName, color = Color(0xFFDBEAFE), fontSize = 12.sp)
                Text("Bom trabalho hoje!", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Progresso Geral
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("VISÃO GERAL DO ÁLBUM", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                // Anel de Progresso
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.size(120.dp)) {
                        drawArc(Color(0xFFF3F4F6), 0f, 360f, false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
                        drawArc(Color(0xFF10B981), -90f, (stats.percentage / 100f) * 360f, false, style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${"%.1f".format(stats.percentage)}%", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("COMPLETO", fontSize = 10.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${stats.acquired}", fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("TENHO", fontSize = 10.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${stats.missing}", fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("FALTAM", fontSize = 10.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${stats.repeats}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFFF59E0B))
                        Text("REPETIDAS", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumScreen(albumData: List<Team>, onStickerAction: (String, String, Boolean) -> Unit) {
    var isRemoveMode by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Alternador Adicionar/Remover
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Card(shape = RoundedCornerShape(50)) {
                Row(modifier = Modifier.background(Color(0xFFF3F4F6))) {
                    Box(
                        modifier = Modifier.clickable { isRemoveMode = false }.background(if (!isRemoveMode) Color(0xFF10B981) else Color.Transparent, RoundedCornerShape(50)).padding(horizontal = 24.dp, vertical = 8.dp)
                    ) { Text("ADICIONAR (+)", color = if (!isRemoveMode) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp) }

                    Box(
                        modifier = Modifier.clickable { isRemoveMode = true }.background(if (isRemoveMode) Color(0xFFEF4444) else Color.Transparent, RoundedCornerShape(50)).padding(horizontal = 24.dp, vertical = 8.dp)
                    ) { Text("REMOVER (-)", color = if (isRemoveMode) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            albumData.forEach { team ->
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(4) }) {
                    Text(
                        team.name.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }

                items(team.stickers) { sticker ->
                    val isMissing = sticker.status == "missing"
                    val bgColor = if (isMissing) Color.White else Color(0xFFEFF6FF)
                    val borderColor = if (isMissing) Color(0xFFD1D5DB) else Color(0xFF3B82F6)
                    val txtColor = if (isMissing) Color.LightGray else Color(0xFF1E40AF)

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .aspectRatio(0.75f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
                            .clickable {
                                onStickerAction(team.id, sticker.id, isRemoveMode)
                            }
                    ) {
                        Text(
                            sticker.id.split("-")[1],
                            color = txtColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )

                        if (sticker.status == "repeat") {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(20.dp)
                                    .background(Color(0xFFEF4444), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "+${sticker.repeatCount}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (sticker.status == "acquired") {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(16.dp)
                                    .background(Color(0xFF3B82F6), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RepetidasScreen(albumData: List<Team>, onStickerAction: (String, String, Boolean) -> Unit, totalRepeats: Int) {
    val repetidasList = albumData.flatMap { team -> team.stickers.filter { it.status == "repeat" }.map { it to team } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("SALDO PARA TROCA", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("$totalRepeats", fontSize = 32.sp, fontWeight = FontWeight.Black)
                }
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))) {
                    Text("${repetidasList.size} ÚNICAS", color = Color(0xFFB45309), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(repetidasList) { (sticker, team) ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(sticker.id, color = Color(0xFF1E40AF), fontWeight = FontWeight.Bold, modifier = Modifier.background(Color(0xFFDBEAFE), RoundedCornerShape(4.dp)).padding(4.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(sticker.player, fontWeight = FontWeight.Bold)
                                Text(team.name.uppercase(), fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                        // Botões
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp)).padding(4.dp)) {
                            Box(modifier = Modifier.size(32.dp).clickable { onStickerAction(team.id, sticker.id, true) }, contentAlignment = Alignment.Center) {
                                Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                            }
                            Text("${sticker.repeatCount}", fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp))
                            Box(modifier = Modifier.size(32.dp).clickable { onStickerAction(team.id, sticker.id, false) }, contentAlignment = Alignment.Center) {
                                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PerfilScreen(
    profiles: List<Profile>, activeProfileId: Long, albumsData: Map<Long, List<Team>>,
    onProfileChange: (Long) -> Unit, onCreateProfile: (String) -> Unit, onZerarAlbum: () -> Unit
) {
    var showNewProfileDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var showZerarDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Meus Álbuns", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))

        profiles.forEach { p ->
            val isActive = activeProfileId == p.id
            val acquired = albumsData[p.id]?.sumOf { team -> team.stickers.count { it.status != "missing" } } ?: 0

            Card(
                colors = CardDefaults.cardColors(containerColor = if (isActive) Color(0xFFEFF6FF) else Color.White),
                border = if (isActive) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF3B82F6)) else null,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onProfileChange(p.id) }
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(p.name, fontWeight = FontWeight.Black, color = if (isActive) Color(0xFF1E3A8A) else Color.DarkGray)
                        Text("${if(isActive) "Ativo • " else ""}$acquired figurinhas", fontSize = 12.sp, color = if (isActive) Color(0xFF2563EB) else Color.Gray)
                    }
                    if (isActive) Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2563EB))
                }
            }
        }

        OutlinedButton(
            onClick = { showNewProfileDialog = true },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) { Text("CRIAR NOVO PERFIL") }

        Spacer(modifier = Modifier.height(24.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Dados 100% Offline (Standalone Mode)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
                Button(
                    onClick = { showZerarDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = Color(0xFFDC2626)),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Zerar Este Álbum") }
            }
        }
    }

    // Janelas
    if (showNewProfileDialog) {
        AlertDialog(
            onDismissRequest = { showNewProfileDialog = false },
            title = { Text("Novo Álbum") },
            text = { OutlinedTextField(value = newProfileName, onValueChange = { newProfileName = it }, label = { Text("Nome do perfil") }) },
            confirmButton = { Button(onClick = { onCreateProfile(newProfileName); showNewProfileDialog = false; newProfileName = "" }) { Text("CRIAR") } },
            dismissButton = { TextButton(onClick = { showNewProfileDialog = false }) { Text("CANCELAR") } }
        )
    }

    if (showZerarDialog) {
        AlertDialog(
            onDismissRequest = { showZerarDialog = false },
            title = { Text("Atenção!", color = Color.Red) },
            text = { Text("Estás prestes a apagar todas as figurinhas marcadas neste perfil. Esta ação não pode ser desfeita.") },
            confirmButton = { Button(onClick = { onZerarAlbum(); showZerarDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("SIM, ZERAR") } },
            dismissButton = { TextButton(onClick = { showZerarDialog = false }) { Text("CANCELAR") } }
        )
    }
}

// ======================================================================
// 5. BARRA DE NAVEGAÇÃO INFERIOR
// ======================================================================
@Composable
fun BottomNav(activeTab: String, onTabSelected: (String) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(
            selected = activeTab == "dashboard",
            onClick = { onTabSelected("dashboard") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
            label = { Text("Início", fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = activeTab == "album",
            onClick = { onTabSelected("album") },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Álbum") },
            label = { Text("Álbum", fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = activeTab == "repetidas",
            onClick = { onTabSelected("repetidas") },
            icon = { Icon(Icons.Default.Star, contentDescription = "Trocas") },
            label = { Text("Troca", fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = activeTab == "perfil",
            onClick = { onTabSelected("perfil") },
            icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
            label = { Text("Perfil", fontSize = 10.sp) }
        )
    }
}