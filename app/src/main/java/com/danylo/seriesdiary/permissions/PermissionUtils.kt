package com.danylo.seriesdiary.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat

/**
 * Три стани runtime-дозволу:
 *  - Granted — користувач дозволив, основний UI;
 *  - Denied — користувач відмовив, можна повторно запросити;
 *  - PermanentlyDenied — користувач відмовив із "Не питати знову" — потрібно вести в системні налаштування.
 */
enum class PermissionStatus { Granted, Denied, PermanentlyDenied }

/**
 * Відкриває системні налаштування саме для цього застосунку,
 * щоб користувач міг вручну надати дозвіл, який раніше відхилив остаточно.
 */
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private fun Context.hasPermission(name: String): Boolean =
    ContextCompat.checkSelfPermission(this, name) == PackageManager.PERMISSION_GRANTED

private fun Activity.shouldShowRationaleSafe(name: String): Boolean =
    ActivityCompat.shouldShowRequestPermissionRationale(this, name)

/**
 * Сценарій після відмови:
 *  - shouldShowRationale=true → користувач відмовив, але можна спитати ще;
 *  - shouldShowRationale=false і дозвіл не наданий → "Не питати знову" або політика пристрою.
 */
private fun resolveDeniedStatus(activity: Activity?, name: String): PermissionStatus {
    val rationale = activity?.shouldShowRationaleSafe(name) ?: false
    return if (rationale) PermissionStatus.Denied else PermissionStatus.PermanentlyDenied
}

/**
 * Універсальний "permission gate" для single permission:
 *  - якщо granted → рендеримо content();
 *  - якщо denied → запит дозволу через системний діалог;
 *  - якщо permanently denied → кнопка переходу у налаштування.
 *
 * featureTitle / featureRationale формулюють, навіщо застосунку дозвіл (UA).
 */
@Composable
fun PermissionGate(
    permission: String,
    featureTitle: String,
    featureRationale: String,
    icon: ImageVector = when (permission) {
        android.Manifest.permission.CAMERA -> Icons.Default.NoPhotography
        else -> Icons.Default.LocationOff
    },
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // початковий стан читаємо синхронно
    var status by rememberSaveable(permission) {
        mutableStateOf(
            if (context.hasPermission(permission)) PermissionStatus.Granted
            else PermissionStatus.Denied
        )
    }
    // позначає, що користувач уже клацав "Запросити" — без цього не можемо відрізнити
    // первинне "Denied" (треба показати кнопку) від реальної відмови з rationale
    var requested by rememberSaveable(permission) { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        requested = true
        status = if (granted) PermissionStatus.Granted
        else resolveDeniedStatus(activity, permission)
    }

    // повертаємось із системних налаштувань — перевіряємо статус заново
    LaunchedEffect(Unit) {
        if (context.hasPermission(permission)) {
            status = PermissionStatus.Granted
        }
    }

    when (status) {
        PermissionStatus.Granted -> content()
        PermissionStatus.Denied -> PermissionRequestPanel(
            icon = icon,
            title = featureTitle,
            rationale = featureRationale,
            actionLabel = if (requested) "Спробувати ще раз" else "Надати дозвіл",
            onAction = { launcher.launch(permission) }
        )
        PermissionStatus.PermanentlyDenied -> PermissionSettingsPanel(
            icon = icon,
            title = featureTitle,
            rationale = featureRationale,
            onOpenSettings = { openAppSettings(context) }
        )
    }
}

@Composable
private fun PermissionRequestPanel(
    icon: ImageVector,
    title: String,
    rationale: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                text = rationale,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun PermissionSettingsPanel(
    icon: ImageVector,
    title: String,
    rationale: String,
    onOpenSettings: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "$title — дозвіл відхилено",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "$rationale\n\nВи відмовили остаточно. Щоб увімкнути функцію, надайте дозвіл вручну в системних налаштуваннях застосунку.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Перейти в налаштування")
            }
        }
    }
}

/**
 * Аналогічний gate для пари дозволів (FINE/COARSE location).
 * Granted = хоча б один з дозволів надано (FINE точніший, COARSE — fallback).
 */
@Composable
fun LocationPermissionGate(
    featureTitle: String,
    featureRationale: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val fine = android.Manifest.permission.ACCESS_FINE_LOCATION
    val coarse = android.Manifest.permission.ACCESS_COARSE_LOCATION

    fun currentStatus(): PermissionStatus = when {
        context.hasPermission(fine) || context.hasPermission(coarse) -> PermissionStatus.Granted
        else -> PermissionStatus.Denied
    }

    var status by rememberSaveable { mutableStateOf(currentStatus()) }
    var requested by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        requested = true
        val granted = results[fine] == true || results[coarse] == true
        status = when {
            granted -> PermissionStatus.Granted
            // якщо для жодного з location-дозволів немає rationale — це permanently denied
            activity != null && !activity.shouldShowRationaleSafe(fine)
                    && !activity.shouldShowRationaleSafe(coarse) -> PermissionStatus.PermanentlyDenied
            else -> PermissionStatus.Denied
        }
    }

    LaunchedEffect(Unit) {
        status = currentStatus()
    }

    when (status) {
        PermissionStatus.Granted -> content()
        PermissionStatus.Denied -> PermissionRequestPanel(
            icon = Icons.Default.LocationOff,
            title = featureTitle,
            rationale = featureRationale,
            actionLabel = if (requested) "Спробувати ще раз" else "Надати дозвіл",
            onAction = { launcher.launch(arrayOf(fine, coarse)) }
        )
        PermissionStatus.PermanentlyDenied -> PermissionSettingsPanel(
            icon = Icons.Default.LocationOff,
            title = featureTitle,
            rationale = featureRationale,
            onOpenSettings = { openAppSettings(context) }
        )
    }
}
