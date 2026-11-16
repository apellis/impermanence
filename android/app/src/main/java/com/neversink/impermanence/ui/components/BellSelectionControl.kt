package com.neversink.impermanence.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neversink.impermanence.model.Bell
import com.neversink.impermanence.model.BellCatalog
import com.neversink.impermanence.model.BellSound

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BellSelectionControl(
    title: String,
    bell: Bell,
    onBellChange: (Bell) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedSound: BellSound = bell.sound

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedSound.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Sound") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                BellCatalog.all.forEach { candidate ->
                    DropdownMenuItem(
                        text = { Text(candidate.name) },
                        onClick = {
                            expanded = false
                            onBellChange(bell.copy(soundId = candidate.id))
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text("Chimes: ${bell.numRings}")
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(
                onClick = { if (bell.numRings > 1) onBellChange(bell.copy(numRings = bell.numRings - 1)) }
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease chimes")
            }
            IconButton(
                onClick = { if (bell.numRings < 12) onBellChange(bell.copy(numRings = bell.numRings + 1)) }
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Increase chimes")
            }
        }
    }
}
