package com.nura.messaging.features.chat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nura.messaging.core.common.ui.theme.NuraTheme

data class EmojiCategory(
    val name: String,
    val icon: ImageVector,
    val emojis: List<String>
)

object EmojiCatalog {
    val categories: List<EmojiCategory> = listOf(
        EmojiCategory(
            name = "Smileys",
            icon = Icons.Outlined.SentimentSatisfied,
            emojis = listOf(
                "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "🙃",
                "😉", "😊", "😇", "🥰", "😍", "🤩", "😘", "😗", "😚", "😙",
                "😋", "😛", "😜", "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤔",
                "🤐", "🤨", "😐", "😑", "😶", "😏", "😒", "🙄", "😬", "🤥",
                "😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕", "🤢", "🤮",
                "🤧", "🥵", "🥶", "🥴", "😵", "🤯", "🤠", "🥳", "😎", "🤓",
                "🧐", "😕", "😟", "🙁", "☹️", "😮", "😯", "😲", "😳", "🥺",
                "😦", "😧", "😨", "😰", "😥", "😢", "😭", "😱", "😖", "😣",
                "😞", "😓", "😩", "😫", "🥱", "😤", "😡", "😠", "🤬", "😈",
                "👿", "💀", "☠️", "💩", "🤡", "👹", "👺", "👻", "👽", "👾",
                "🤖", "😺", "😸", "😹", "😻", "😼", "😽", "🙀", "😿", "😾",
                "❤️", "🧡", "💛", "💚", "💙", "💜", "🤎", "🖤", "🤍", "💔",
                "❤️‍🔥", "❤️‍🩹", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝"
            )
        ),
        EmojiCategory(
            name = "People",
            icon = Icons.Outlined.Person,
            emojis = listOf(
                "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏", "✌️", "🤞",
                "🤟", "🤘", "🤙", "👈", "👉", "👆", "🖕", "👇", "☝️", "👍",
                "👎", "✊", "👊", "🤛", "🤜", "👏", "🙌", "👐", "🤲", "🤝",
                "🙏", "✍️", "💅", "🤳", "💪", "🦾", "🦿", "🦵", "🦶", "👂",
                "🦻", "👃", "🧠", "🫀", "🫁", "🦷", "🦴", "👀", "👁️", "👅",
                "👄", "👶", "🧒", "👦", "👧", "🧑", "👱", "👨", "🧔", "👩",
                "🧓", "👴", "👵", "👮", "🕵️", "💂", "🥷", "👷", "🤴", "👸"
            )
        ),
        EmojiCategory(
            name = "Animals",
            icon = Icons.Outlined.Pets,
            emojis = listOf(
                "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯",
                "🦁", "🐮", "🐷", "🐽", "🐸", "🐵", "🙈", "🙉", "🙊", "🐒",
                "🐔", "🐧", "🐦", "🐤", "🐣", "🐥", "🦆", "🦅", "🦉", "🦇",
                "🐺", "🐗", "🐴", "🦄", "🐝", "🐛", "🦋", "🐌", "🐞", "🐜",
                "🦟", "🦗", "🕷️", "🦂", "🐢", "🐍", "🦎", "🦖", "🦕", "🐙",
                "🦑", "🦐", "🦞", "🦀", "🐡", "🐠", "🐟", "🐬", "🐳", "🐋",
                "🦈", "🐊", "🐅", "🐆", "🦓", "🦍", "🦧", "🦣", "🐘", "🦛"
            )
        ),
        EmojiCategory(
            name = "Food",
            icon = Icons.Outlined.Fastfood,
            emojis = listOf(
                "🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐",
                "🍈", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑",
                "🥦", "🥬", "🥒", "🌶️", "🫑", "🌽", "🥕", "🫒", "🧄", "🧅",
                "🥔", "🍠", "🥐", "🥯", "🍞", "🥖", "🥨", "🧀", "🥚", "🍳",
                "🧈", "🥞", "🧇", "🥓", "🥩", "🍗", "🍖", "🌭", "🍔", "🍟",
                "🍕", "🫓", "🥪", "🥙", "🧆", "🌮", "🌯", "🥗", "🥘", "🍝",
                "🍜", "🍲", "🍛", "🍣", "🍱", "🥟", "🦪", "🍤", "🍙", "🍚",
                "🍦", "🍧", "🍨", "🍩", "🍪", "🎂", "🍰", "🧁", "🍫", "🍬",
                "🍭", "🍮", "🍯", "☕️", "🍵", "🧃", "🥤", "🧋", "🍺", "🍻"
            )
        ),
        EmojiCategory(
            name = "Activities",
            icon = Icons.Outlined.SportsSoccer,
            emojis = listOf(
                "⚽️", "🏀", "🏈", "⚾️", "🥎", "🎾", "🏐", "🏉", "🥏", "🎱",
                "🪀", "🏓", "🏸", "🏒", "🏑", "🥍", "🏏", "🪃", "🥅", "⛳️",
                "🪁", "🏹", "🎣", "🤿", "🥊", "🥋", "🎽", "🛹", "🛼", "🛷",
                "⛸️", "🎿", "🏂", "🏋️", "🤼", "🤸", "⛹️", "🤺", "🤾", "🧗",
                "🏄", "🏊", "🤽", "🚣", "🏇", "🚴", "🚵", "🎪", "🎭", "🎨",
                "🎬", "🎤", "🎧", "🎼", "🎹", "🥁", "🎷", "🎺", "🎸", "🎻",
                "🎲", "♟️", "🎯", "🎳", "🎮", "🎰", "🧩", "🏆", "🥇", "🥈"
            )
        ),
        EmojiCategory(
            name = "Travel",
            icon = Icons.Outlined.DirectionsCar,
            emojis = listOf(
                "🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑", "🚒", "🚐",
                "🛻", "🚚", "🚛", "🚜", "🛴", "🚲", "🛵", "🏍️", "🛺", "🚨",
                "🚔", "🚍", "🚘", "🚖", "🚄", "🚅", "🚆", "🚇", "🚉", "✈️",
                "🛫", "🛬", "🛩️", "🚀", "🛸", "🚁", "🛶", "⛵️", "🚤", "🛥️",
                "🛳️", "⛴️", "🚢", "⚓️", "⛽️", "🚧", "🚦", "🚥", "🗺️", "🗿",
                "🗽", "🗼", "🏰", "🏯", "🏟️", "🎡", "🎢", "🏖️", "🏝️", "🏜️",
                "🌋", "⛰️", "🏔️", "🏕️", "🏠", "🏡", "🏢", "🏬", "🏥", "🏦"
            )
        ),
        EmojiCategory(
            name = "Objects",
            icon = Icons.Outlined.Lightbulb,
            emojis = listOf(
                "💡", "🔦", "🕯️", "📱", "📲", "💻", "⌨️", "🖥️", "🖨️", "🖱️",
                "📷", "📸", "📹", "🎥", "📽️", "🎞️", "🔋", "🔌", "💰", "🪙",
                "💳", "💎", "⚖️", "🔧", "🔨", "⚒️", "🛠️", "⛏️", "🔩", "⚙️",
                "🧱", "⛓️", "🧲", "💣", "🧨", "🪓", "🔪", "🗡️", "⚔️", "🛡️",
                "🔮", "🧿", "💈", "⚗️", "🔭", "🔬", "🩹", "🩺", "💊", "💉",
                "🔑", "🗝️", "🚪", "🪑", "🛋️", "🛏️", "🎁", "🎈", "🎉", "🎊",
                "✉️", "📩", "📦", "📫", "📜", "📄", "📅", "📊", "📈", "📉",
                "📚", "📖", "🔖", "📌", "📍", "📎", "✂️", "🔒", "🔓", "💯"
            )
        )
    )
}

@Composable
fun EmojiPickerSection(
    onEmojiSelected: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NuraTheme.colors
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val categories = EmojiCatalog.categories
    val currentEmojis = categories[selectedCategoryIndex].emojis

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(colors.cardDarkBubble)
    ) {
        // Tab Row with Categories
        ScrollableTabRow(
            selectedTabIndex = selectedCategoryIndex,
            edgePadding = 8.dp,
            containerColor = colors.cardDarkBubble,
            contentColor = colors.terracottaAccent,
            modifier = Modifier.fillMaxWidth()
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = selectedCategoryIndex == index,
                    onClick = { selectedCategoryIndex = index },
                    icon = {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = category.name,
                            tint = if (selectedCategoryIndex == index) colors.terracottaAccent else colors.subtitleText,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.height(44.dp)
                )
            }
        }

        // Emoji Grid + Bottom Action Row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(currentEmojis, key = { it }) { emoji ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { onEmojiSelected(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Floating backspace icon button in bottom right (like WhatsApp / Gboard)
            IconButton(
                onClick = onBackspace,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(colors.badgeBackground)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = "Backspace",
                    tint = colors.brandLogoText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
