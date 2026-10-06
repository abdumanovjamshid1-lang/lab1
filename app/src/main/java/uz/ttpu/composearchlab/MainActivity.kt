package uz.ttpu.composearchlab

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Change demo below to run a different lab screen. TASTE is the final app.
private enum class LabDemo {
    NAME_REMEMBER,
    NAME_SAVEABLE,
    NEWS,
    SIGN_IN,
    SIGN_IN_BUG,
    TASTE
}

private val demo = LabDemo.TASTE

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                when (demo) {
                    LabDemo.SIGN_IN -> SignInRoute()

                    LabDemo.SIGN_IN_BUG ->
                        SignInRoute(consumeErrors = false)

                    LabDemo.TASTE -> TastePickerRoute()

                    else -> Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        val modifier = Modifier.padding(innerPadding)

                        when (demo) {
                            LabDemo.NAME_REMEMBER ->
                                NameRememberScreen(modifier)

                            LabDemo.NAME_SAVEABLE ->
                                NameScreen(modifier)

                            LabDemo.NEWS ->
                                NewsScreen(modifier)

                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

// Task 2: remember loses state when the Activity is recreated.
@Composable
fun NameRememberScreen(modifier: Modifier = Modifier) {
    var name by remember { mutableStateOf("") }

    Column(modifier.padding(24.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") }
        )

        Text("Hello, $name!")
    }
}

// Task 3: state flows down; changes are reported to the caller.
@Composable
fun NameField(
    name: String,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Name") },
        modifier = modifier
    )
}

@Composable
fun NameScreen(modifier: Modifier = Modifier) {
    // rememberSaveable restores this String after configuration changes.
    var name by rememberSaveable { mutableStateOf("") }

    Column(modifier.padding(24.dp)) {
        NameField(
            name = name,
            onNameChange = { name = it }
        )

        Text("Hello, $name!")
    }
}

@Preview(showBackground = true)
@Composable
private fun NameFieldPreview() {
    MaterialTheme {
        NameField(
            name = "Amin",
            onNameChange = {}
        )
    }
}

// Task 4: compare passing a whole object with passing needed values.
data class News(
    val title: String,
    val subtitle: String,
    val views: Int
)

@Composable
fun Header(news: News) {
    SideEffect {
        Log.d("Recompose", "Header(news)")
    }

    Column {
        Text(news.title)
        Text(news.subtitle)
    }
}

// Better design: the header receives only the values it displays.
// Group many related parameters into a small immutable UI model
// when that makes the composable's interface clearer.
@Composable
fun Header(title: String, subtitle: String) {
    SideEffect {
        Log.d("Recompose", "Header(title, subtitle)")
    }

    Column {
        Text(title)
        Text(subtitle)
    }
}

@Composable
fun NewsScreen(modifier: Modifier = Modifier) {
    var news by remember {
        mutableStateOf(
            News(
                title = "Compose is declarative",
                subtitle = "State in, UI out",
                views = 0
            )
        )
    }

    Column(modifier.padding(24.dp)) {
        Header(news)
        Header(news.title, news.subtitle)

        Text("Views: ${news.views}")

        Button(
            onClick = {
                news = news.copy(views = news.views + 1)
            }
        ) {
            Text("Add view")
        }
    }
}

// Task 5: different cases can carry different per-result data.
// Error carries a message; SignedIn carries an email.
// Enum constants cannot represent these individual result payloads.
sealed interface SignInUiState {
    data object SignedOut : SignInUiState
    data object InProgress : SignInUiState

    data class Error(
        val message: String
    ) : SignInUiState

    data class SignedIn(
        val email: String
    ) : SignInUiState
}

// Task 6: the ViewModel owns business state and handles events.
class SignInViewModel : ViewModel() {
    // Mutable state is private so only this ViewModel changes it.
    // The UI receives read-only State for observation.
    private val _uiState =
        mutableStateOf<SignInUiState>(SignInUiState.SignedOut)

    val uiState: State<SignInUiState>
        get() = _uiState

    fun onSignIn(email: String, password: String) {
        if (_uiState.value == SignInUiState.InProgress) return

        _uiState.value = SignInUiState.InProgress

        viewModelScope.launch {
            delay(1500)

            // Simulated authentication for the laboratory only.
            _uiState.value = if (password == "kotlin123") {
                SignInUiState.SignedIn(email)
            } else {
                SignInUiState.Error("Wrong email or password")
            }
        }
    }

    // Task 9: acknowledge the completed error Snackbar.
    fun onErrorShown() {
        if (_uiState.value is SignInUiState.Error) {
            _uiState.value = SignInUiState.SignedOut
        }
    }
}

// Task 7: business state is supplied by the caller.
// Text fields keep their own local UI-element state.
@Composable
fun SignInScreen(
    uiState: SignInUiState,
    onSignIn: (email: String, password: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (uiState) {
            is SignInUiState.SignedIn -> {
                Text("Welcome, ${uiState.email}")
            }

            SignInUiState.SignedOut,
            SignInUiState.InProgress,
            is SignInUiState.Error -> {
                val loading = uiState == SignInUiState.InProgress

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    enabled = !loading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = !loading,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        onSignIn(email, password)
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign in")
                }

                if (loading) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Signed out")
@Composable
private fun SignedOutPreview() {
    MaterialTheme {
        SignInScreen(
            uiState = SignInUiState.SignedOut,
            onSignIn = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "In progress")
@Composable
private fun InProgressPreview() {
    MaterialTheme {
        SignInScreen(
            uiState = SignInUiState.InProgress,
            onSignIn = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "Error")
@Composable
private fun ErrorPreview() {
    MaterialTheme {
        SignInScreen(
            uiState = SignInUiState.Error("Wrong email or password"),
            onSignIn = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "Signed in")
@Composable
private fun SignedInPreview() {
    MaterialTheme {
        SignInScreen(
            uiState = SignInUiState.SignedIn("amin@example.com"),
            onSignIn = { _, _ -> }
        )
    }
}

// Tasks 8–9: demonstrate and fix Snackbar replay.
@Composable
fun SignInRoute(
    viewModel: SignInViewModel = viewModel(),
    consumeErrors: Boolean = true
) {
    val uiState = viewModel.uiState.value
    val snackbarHostState = remember { SnackbarHostState() }

    // Without acknowledgment, the ViewModel retains Error.
    // Rotation recreates the composition and starts this effect again,
    // causing the same Snackbar to appear.
    LaunchedEffect(uiState, consumeErrors) {
        if (uiState is SignInUiState.Error) {
            snackbarHostState.showSnackbar(uiState.message)

            // Reset the error after the Snackbar finishes.
            if (consumeErrors) {
                viewModel.onErrorShown()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { innerPadding ->
        SignInScreen(
            uiState = uiState,
            onSignIn = viewModel::onSignIn,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

// Task 10: one screen state with derived properties.
const val REQUIRED_LIKES = 3

data class Artist(
    val id: Int,
    val name: String,
    val genre: String
)

val seedArtists = listOf(
    Artist(1, "Nilufar Skye", "Pop"),
    Artist(2, "Amir Neon", "Pop"),
    Artist(3, "Zarina Bloom", "Pop"),
    Artist(4, "Iron Caravan", "Rock"),
    Artist(5, "Qora Tun", "Rock"),
    Artist(6, "Steel Orchard", "Rock"),
    Artist(7, "Midnight Samarkand", "Jazz"),
    Artist(8, "Blue Registan", "Jazz"),
    Artist(9, "Saida Quartet", "Jazz"),
    Artist(10, "Silk Road Duo", "Folk"),
    Artist(11, "Navruz Ensemble", "Folk"),
    Artist(12, "Chor Minor", "Folk")
)

// The UI receives a read-only Set.
// copy and + / - produce new snapshots instead of mutating old state,
// avoiding shared mutable-state problems.
// Do not pass a MutableSet alias into this state.
data class TastePickerState(
    val artists: List<Artist>,
    val selectedGenre: String? = null,
    val likedIds: Set<Int> = emptySet()
) {
    val genres: List<String>
        get() = artists.map { it.genre }.distinct()

    val visibleArtists: List<Artist>
        get() = if (selectedGenre == null) {
            artists
        } else {
            artists.filter { it.genre == selectedGenre }
        }

    val likedCount: Int
        get() = likedIds.size

    val canContinue: Boolean
        get() = likedCount >= REQUIRED_LIKES
}

// Task 11: expose StateFlow and handle UI events.
class TastePickerViewModel : ViewModel() {
    private val _state = MutableStateFlow(
        TastePickerState(artists = seedArtists)
    )

    val state: StateFlow<TastePickerState> = _state.asStateFlow()

    fun onGenreClick(genre: String) {
        _state.update { current ->
            current.copy(
                selectedGenre = if (current.selectedGenre == genre) {
                    null
                } else {
                    genre
                }
            )
        }
    }

    fun onArtistLikeToggled(id: Int) {
        _state.update { current ->
            current.copy(
                likedIds = if (id in current.likedIds) {
                    current.likedIds - id
                } else {
                    current.likedIds + id
                }
            )
        }
    }
}

// Task 12: stateless artist card.
@Composable
fun ArtistCard(
    artist: Artist,
    liked: Boolean,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(
                    color = Color.hsv(
                        (artist.id * 47 % 360).toFloat(),
                        0.5f,
                        0.8f
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = artist.name.take(1),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
        }

        Text(
            text = artist.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(top = 8.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )

        IconButton(onClick = onLikeClick) {
            Icon(
                imageVector = if (liked) {
                    Icons.Default.Favorite
                } else {
                    Icons.Default.FavoriteBorder
                },
                contentDescription = if (liked) {
                    "Unlike ${artist.name}"
                } else {
                    "Like ${artist.name}"
                },
                tint = if (liked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 120, name = "Not liked")
@Composable
private fun ArtistCardNotLikedPreview() {
    MaterialTheme {
        ArtistCard(
            artist = seedArtists[0],
            liked = false,
            onLikeClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 120, name = "Liked")
@Composable
private fun ArtistCardLikedPreview() {
    MaterialTheme {
        ArtistCard(
            artist = seedArtists[0],
            liked = true,
            onLikeClick = {}
        )
    }
}

@Composable
fun ArtistGrid(
    artists: List<Artist>,
    likedIds: Set<Int>,
    onLikeClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = artists,
            key = { it.id }
        ) { artist ->
            ArtistCard(
                artist = artist,
                liked = artist.id in likedIds,
                onLikeClick = {
                    onLikeClick(artist.id)
                }
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 920)
@Composable
private fun ArtistGridPreview() {
    MaterialTheme {
        ArtistGrid(
            artists = seedArtists,
            likedIds = setOf(1, 3),
            onLikeClick = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

// Task 13: genre filtering.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreChips(
    genres: List<String>,
    selectedGenre: String?,
    onGenreClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = genres,
            key = { it }
        ) { genre ->
            FilterChip(
                selected = genre == selectedGenre,
                onClick = {
                    onGenreClick(genre)
                },
                label = {
                    Text(genre)
                }
            )
        }
    }
}

// Task 14: progress and actions.
@Composable
fun TastePickerBottomBar(
    likedCount: Int,
    required: Int,
    canContinue: Boolean,
    onContinueClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    val progress = if (required > 0) {
        (likedCount.toFloat() / required).coerceIn(0f, 1f)
    } else {
        1f
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(56.dp),
                        strokeWidth = 5.dp
                    )

                    Text(
                        text = "$likedCount/$required",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Text("Pick $required artists you like")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSkipClick) {
                    Text("Later")
                }

                Button(
                    onClick = onContinueClick,
                    enabled = canContinue
                ) {
                    Text("Continue")
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 380, name = "0 likes")
@Composable
private fun BottomBarZeroPreview() {
    MaterialTheme {
        TastePickerBottomBar(
            likedCount = 0,
            required = REQUIRED_LIKES,
            canContinue = false,
            onContinueClick = {},
            onSkipClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 380, name = "2 likes")
@Composable
private fun BottomBarTwoPreview() {
    MaterialTheme {
        TastePickerBottomBar(
            likedCount = 2,
            required = REQUIRED_LIKES,
            canContinue = false,
            onContinueClick = {},
            onSkipClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 380, name = "3 likes")
@Composable
private fun BottomBarThreePreview() {
    MaterialTheme {
        TastePickerBottomBar(
            likedCount = 3,
            required = REQUIRED_LIKES,
            canContinue = true,
            onContinueClick = {},
            onSkipClick = {}
        )
    }
}

@Composable
fun TastePickerScreen(
    state: TastePickerState,
    onGenreClick: (String) -> Unit,
    onLikeClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            TastePickerBottomBar(
                likedCount = state.likedCount,
                required = REQUIRED_LIKES,
                canContinue = state.canContinue,
                onContinueClick = {
                    Log.d(
                        "TastePicker",
                        "Continue: ${state.likedCount} artists liked"
                    )
                },
                onSkipClick = {
                    Log.d("TastePicker", "Later")
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            GenreChips(
                genres = state.genres,
                selectedGenre = state.selectedGenre,
                onGenreClick = onGenreClick
            )

            ArtistGrid(
                artists = state.visibleArtists,
                likedIds = state.likedIds,
                onLikeClick = onLikeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
fun TastePickerRoute(
    viewModel: TastePickerViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    TastePickerScreen(
        state = state,
        onGenreClick = viewModel::onGenreClick,
        onLikeClick = viewModel::onArtistLikeToggled
    )
}