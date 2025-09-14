package digital.tonima.mydiary.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.Yellow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import digital.tonima.mydiary.R.string.password_strength_medium
import digital.tonima.mydiary.R.string.password_strength_strong
import digital.tonima.mydiary.R.string.password_strength_weak
import digital.tonima.mydiary.ui.viewmodels.PasswordStrength

@Composable
fun PasswordStrengthIndicator(strength: PasswordStrength) {
    val (text, color) = when (strength) {
        PasswordStrength.WEAK -> stringResource(password_strength_weak) to Red
        PasswordStrength.MEDIUM -> stringResource(password_strength_medium) to Yellow
        PasswordStrength.STRONG -> stringResource(password_strength_strong) to Green
        PasswordStrength.EMPTY -> "" to Color.Transparent
    }

    val animatedColor by animateColorAsState(targetValue = color, animationSpec = tween(500))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        if (strength != PasswordStrength.EMPTY) {
            Text(
                text = text,
                color = animatedColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = spacedBy(4.dp)
        ) {
            StrengthBar(
                modifier = Modifier.weight(1f),
                isActive = strength != PasswordStrength.EMPTY,
                color = animatedColor
            )
            StrengthBar(
                modifier = Modifier.weight(1f),
                isActive = strength == PasswordStrength.MEDIUM || strength == PasswordStrength.STRONG,
                color = animatedColor
            )
            StrengthBar(
                modifier = Modifier.weight(1f),
                isActive = strength == PasswordStrength.STRONG,
                color = animatedColor
            )
        }
    }
}

@Composable
private fun StrengthBar(
    modifier: Modifier = Modifier,
    isActive: Boolean,
    color: Color
) {
    val barColor = if (isActive) color else MaterialTheme.colorScheme.surfaceVariant
    val animatedBarColor by animateColorAsState(targetValue = barColor, animationSpec = tween(500))
    Box(
        modifier = modifier
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(animatedBarColor)
    )
}
