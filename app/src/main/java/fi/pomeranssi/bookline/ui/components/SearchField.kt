package fi.pomeranssi.bookline.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fi.pomeranssi.bookline.R
import fi.pomeranssi.bookline.ui.theme.BooklineTheme

/**
 * Reusable search/filter text field with search icon and clear button.
 *
 * When [onClearAll] is provided and [hasActiveFilters] is true, an extra
 * icon button is shown to the right of the field to clear all filters.
 */
@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    hasActiveFilters: Boolean = false,
    onClearAll: (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                )
            },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.action_clear_search),
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        if (onClearAll != null && hasActiveFilters) {
            IconButton(onClick = onClearAll) {
                Icon(
                    imageVector = Icons.Default.FilterListOff,
                    contentDescription = stringResource(R.string.action_clear_all_filters),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 88)
@Composable
private fun SearchFieldEmptyPreview() {
    BooklineTheme(dynamicColor = false) {
        SearchField(
            value = "",
            onValueChange = {},
            placeholder = "Search books…",
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 88)
@Composable
private fun SearchFieldWithQueryAndFiltersPreview() {
    BooklineTheme(dynamicColor = false) {
        SearchField(
            value = "rothfuss",
            onValueChange = {},
            placeholder = "Search books…",
            hasActiveFilters = true,
            onClearAll = {},
        )
    }
}
