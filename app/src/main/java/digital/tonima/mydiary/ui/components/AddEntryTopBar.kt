package digital.tonima.mydiary.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle.Companion.Italic
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextDecoration.Companion.Underline
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import digital.tonima.mydiary.R.drawable.bold
import digital.tonima.mydiary.R.drawable.italic
import digital.tonima.mydiary.R.drawable.list_bulleted
import digital.tonima.mydiary.R.drawable.list_numbered
import digital.tonima.mydiary.R.drawable.underline
import digital.tonima.mydiary.R.string.back
import digital.tonima.mydiary.R.string.bulleted_list
import digital.tonima.mydiary.R.string.delete
import digital.tonima.mydiary.R.string.edit_entry
import digital.tonima.mydiary.R.string.new_entry
import digital.tonima.mydiary.R.string.numbered_list
import digital.tonima.mydiary.R.string.save_note

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryTopBar(
    fileNameToEdit: String?,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    TopAppBar(
        title = { Text(stringResource(if (fileNameToEdit == null) new_entry else edit_entry)) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(back))
            }
        },
        actions = {
            if (fileNameToEdit != null) {
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(delete))
                }
            }
            Button(onClick = onSaveClick) {
                Text(stringResource(save_note))
            }
        }
    )
}

@Composable
fun FormattingToolbar(
    richTextState: RichTextState,
    modifier: Modifier = Modifier
) {
    val selectedButtonColors = IconButtonDefaults.iconToggleButtonColors(
        checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconToggleButton(
            checked = richTextState.currentSpanStyle.fontWeight == Bold,
            onCheckedChange = { richTextState.toggleSpanStyle(SpanStyle(fontWeight = Bold)) },
            colors = selectedButtonColors
        ) { Icon(painterResource(bold), "Bold") }

        IconToggleButton(
            checked = richTextState.currentSpanStyle.fontStyle == Italic,
            onCheckedChange = { richTextState.toggleSpanStyle(SpanStyle(fontStyle = Italic)) },
            colors = selectedButtonColors
        ) { Icon(painterResource(italic), "Italic") }

        IconToggleButton(
            checked = richTextState.currentSpanStyle.textDecoration == Underline,
            onCheckedChange = { richTextState.toggleSpanStyle(SpanStyle(textDecoration = Underline)) },
            colors = selectedButtonColors
        ) { Icon(painterResource(underline), "Underline") }

        IconToggleButton(
            checked = richTextState.isUnorderedList,
            onCheckedChange = { richTextState.toggleUnorderedList() },
            colors = selectedButtonColors
        ) {
            Icon(painterResource(list_bulleted), stringResource(bulleted_list))
        }
        IconToggleButton(
            checked = richTextState.isOrderedList,
            onCheckedChange = { richTextState.toggleOrderedList() },
            colors = selectedButtonColors
        ) {
            Icon(painterResource(list_numbered), stringResource(numbered_list))
        }
    }
}
