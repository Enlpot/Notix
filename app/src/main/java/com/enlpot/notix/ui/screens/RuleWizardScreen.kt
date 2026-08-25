package com.enlpot.notix.ui.screens

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.LocalView
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.enlpot.notix.ActionSpec
import com.enlpot.notix.AppInfoStorage
import com.enlpot.notix.BlockerRule
import com.enlpot.notix.BluetoothState
import com.enlpot.notix.ChargingState
import com.enlpot.notix.ClickButtonParams
import com.enlpot.notix.CopyMode
import com.enlpot.notix.CopyParams
import com.enlpot.notix.DelayParams
import com.enlpot.notix.DndState
import com.enlpot.notix.ExtraCondition
import com.enlpot.notix.KnownApp
import com.enlpot.notix.MatchMode
import com.enlpot.notix.TtsParams
import com.enlpot.notix.R
import com.enlpot.notix.RuleAction
import com.enlpot.notix.RuleCondition
import com.enlpot.notix.RuleWizardSupport
import com.enlpot.notix.ScreenState
import com.enlpot.notix.SimpleNotification
import com.enlpot.notix.SnoozeDurations
import com.enlpot.notix.toParamsJson
import com.enlpot.notix.SourceApp
import com.enlpot.notix.TimeCondition
import com.enlpot.notix.paramsGson
import com.enlpot.notix.ui.components.EmptyState
import com.enlpot.notix.ui.components.NotixConfirmDialog
import com.enlpot.notix.ui.components.NotixDialog
import com.enlpot.notix.ui.components.NotixDialogButton
import com.enlpot.notix.ui.components.RealAppIcon
import com.enlpot.notix.ui.theme.NotixCorner
import com.enlpot.notix.ui.theme.notix
import com.enlpot.notix.ui.theme.notixSpacing
import com.enlpot.notix.ui.theme.notixType
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 规则创建/编辑页（v7.11 全新界面）：
 * 1. 来源 App（多选，仅历史通知 App，从通知卡片创建时默认当前 App）
 * 2. 匹配条件（纯关键字标签输入 + 匹配模式）
 * 3. 手机状态额外条件（屏幕/充电/时间日期）
 * 4. 动作单选 7 种（含参数）
 * 5. 描述（可选）
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleWizardScreen(
    existingRules: List<BlockerRule>,
    pastNotifications: List<SimpleNotification>,
    onClose: () -> Unit,
    onCreateRule: (BlockerRule) -> Unit,
    editingRule: BlockerRule? = null,
    onUpdateRule: ((BlockerRule, BlockerRule) -> Unit)? = null,
    onDeleteRule: ((BlockerRule) -> Unit)? = null,
    prefillNotification: SimpleNotification? = null,
) {
    val isEditMode = editingRule != null
    val context = LocalContext.current
    val view = LocalView.current

    // v7.24：应用内 Snackbar 提示（替代系统 Toast）
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    // ===== 1. 来源 App（多选） =====
    var showAppPicker by rememberSaveable { mutableStateOf(false) }
    var selectedPackages by rememberSaveable {
        mutableStateOf(
            editingRule?.sourcePackages?.map { it.packageName }
                ?: listOfNotNull(prefillNotification?.packageName)
        )
    }
    var selectedAppNames by remember {
        mutableStateOf(
            buildMap {
                editingRule?.sourcePackages?.forEach { src ->
                    src.appName?.takeIf { it != src.packageName }?.let { put(src.packageName, it) }
                }
                prefillNotification?.packageName?.let { pkg ->
                    prefillNotification?.appLabel?.takeIf { it != pkg }?.let { put(pkg, it) }
                }
            }
        )
    }

    // ===== 2. 匹配条件 =====
    var matchMode by rememberSaveable { mutableStateOf(editingRule?.condition?.mode ?: MatchMode.CONTAINS_ANY) }
    var includeKeywords by rememberSaveable {
        mutableStateOf<List<String>>(
            if (editingRule != null) {
                editingRule?.condition?.includeKeywords ?: emptyList()
            } else {
                // v7.14：从通知卡片进入创建规则时，自动把完整标题+完整内容填入关键字集合（去重）
                listOfNotNull(
                    prefillNotification?.title?.takeIf { it.isNotBlank() },
                    prefillNotification?.text?.takeIf { it.isNotBlank() }
                ).distinct()
            }
        )
    }
    var excludeKeywords by rememberSaveable {
        mutableStateOf<List<String>>(
            editingRule?.condition?.excludeKeywords ?: emptyList()
        )
    }
    // ===== 3. 额外条件 =====
    var screenState by rememberSaveable {
        mutableStateOf(editingRule?.extraCondition?.screenState ?: ScreenState.ANY)
    }
    var chargingState by rememberSaveable {
        mutableStateOf(editingRule?.extraCondition?.chargingState ?: ChargingState.ANY)
    }
    var dndState by rememberSaveable {
        mutableStateOf(editingRule?.extraCondition?.dndState ?: DndState.ANY)
    }
    var bluetoothState by rememberSaveable {
        mutableStateOf(editingRule?.extraCondition?.bluetoothState ?: BluetoothState.ANY)
    }
    var bluetoothDeviceNames by rememberSaveable {
        mutableStateOf(editingRule?.extraCondition?.bluetoothDeviceNames ?: emptyList())
    }
    var timeEnabled by rememberSaveable { mutableStateOf(editingRule?.extraCondition?.time?.enabled ?: false) }
    var startHour by rememberSaveable { mutableIntStateOf(editingRule?.extraCondition?.time?.startHour ?: 0) }
    var startMinute by rememberSaveable { mutableIntStateOf(editingRule?.extraCondition?.time?.startMinute ?: 0) }
    var endHour by rememberSaveable { mutableIntStateOf(editingRule?.extraCondition?.time?.endHour ?: 23) }
    var endMinute by rememberSaveable { mutableIntStateOf(editingRule?.extraCondition?.time?.endMinute ?: 59) }
    var selectedWeekdays by rememberSaveable {
        mutableStateOf(editingRule?.extraCondition?.time?.weekdays?.toSet() ?: emptySet())
    }

    // ===== 4. Action Flow =====
    // 阶段3A：UI 只维护一个 List<ActionSpec> 作为 Action Flow 唯一编辑状态，
    // 添加/删除/上移/下移/编辑均产生新 list；UI 顺序 == actions 顺序。
    var actionFlow by rememberSaveable(stateSaver = actionSpecListSaver) {
        mutableStateOf(editingRule?.actions.orEmpty())
    }
    // 正在编辑的 Action 下标（-1 = 无）；添加有参数的 Action 后自动指向新卡片
    var editingActionIndex by rememberSaveable { mutableIntStateOf(-1) }

    // ===== 0. 规则名称（可选，复用 BlockerRule.description） =====
    var ruleName by rememberSaveable { mutableStateOf(editingRule?.description.orEmpty()) }

    // ===== 2b. 条件配置弹窗 =====
    var showConditionDialog by rememberSaveable { mutableStateOf(false) }
    var conditionTab by rememberSaveable { mutableIntStateOf(0) }

    // 已知 App（仅历史通知 App，禁止读取已安装应用列表）
    val knownApps by produceState<List<KnownApp>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            val appInfoRows = AppInfoStorage(context).getAllApps()
            val historyRows = pastNotifications
                .mapNotNull { n -> n.packageName?.let { it to n.appLabel } }
            val ruleRows = existingRules.mapNotNull { r ->
                r.sourcePackages.orEmpty().firstOrNull()?.let { src -> src.packageName to src.appName }
            }
            RuleWizardSupport.mergeKnownApps(
                appInfoRows = appInfoRows,
                historyRows = historyRows,
                queryableInstalled = emptyMap(),
                prebuiltNames = emptyMap(),
                ruleRows = ruleRows,
            )
        }
    }

    val effectivePackages = selectedPackages
    fun appDisplayName(pkg: String): String =
        selectedAppNames[pkg] ?: knownApps?.find { it.packageName == pkg }?.appName ?: pkg

    fun hideIme() {
        ViewCompat.getWindowInsetsController(view)?.hide(WindowInsetsCompat.Type.ime())
    }

    BackHandler {
        val imeVisible = ViewCompat.getRootWindowInsets(view)
            ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        if (imeVisible) hideIme() else onClose()
    }

    val hasKeywords = includeKeywords.isNotEmpty() ||
        matchMode == MatchMode.MIXED && excludeKeywords.isNotEmpty()
    val hasApp = effectivePackages.isNotEmpty()
    // 阶段3A：空 Flow 不允许保存（保存按钮 disabled + “至少添加一个动作”提示）
    val canSave = hasApp && hasKeywords && RuleWizardSupport.canSaveFlow(actionFlow)

    // 未命名规则自动命名（N 递增且不与现有规则重复）
    val unnamedPrefix = stringResource(R.string.rule_wizard_unnamed_prefix)
    val submitRule: () -> Unit = {
        val finalName = if (ruleName.isBlank()) {
            var n = 1
            val existing = (existingRules ?: emptyList())
                .let { list -> if (editingRule != null) (list - editingRule).map { r -> r.description } else list.map { r -> r.description } }
            var name = "$unnamedPrefix$n"
            while (existing.contains(name)) {
                n++
                name = "$unnamedPrefix$n"
            }
            name
        } else {
            ruleName.trim()
        }
        val rule = buildNewRule(
            editingRule = editingRule,
            description = finalName,
            selectedPackages = effectivePackages,
            appNameOf = ::appDisplayName,
            matchMode = matchMode,
            includeKeywords = includeKeywords,
            excludeKeywords = excludeKeywords,
            screenState = screenState,
            chargingState = chargingState,
            dndState = dndState,
            bluetoothState = bluetoothState,
            bluetoothDeviceNames = bluetoothDeviceNames,
            timeEnabled = timeEnabled,
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            selectedWeekdays = selectedWeekdays,
            actions = actionFlow,
        )
        if (isEditMode && onUpdateRule != null) {
            onUpdateRule(editingRule!!, rule)
        } else {
            onCreateRule(rule)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.notix.background,
    ) {
        Scaffold(
            // v7.24：应用内 Snackbar 提示（替代系统 Toast）
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(if (isEditMode) R.string.rule_wizard_title_edit_rule else R.string.rule_wizard_title_new_rule)) },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                        }
                    },
                    actions = {
                        // v7.50：对号按钮增加底色——canSave=false 灰色无底色 disabled，canSave=true 主题色高亮
                        Box(
                            modifier = Modifier
                                .padding(end = MaterialTheme.notixSpacing.sm)
                                .clip(RoundedCornerShape(14.dp))
                                .then(
                                    if (canSave) Modifier.background(MaterialTheme.notix.primary)
                                    else Modifier
                                ),
                        ) {
                            IconButton(
                                onClick = submitRule,
                                enabled = canSave,
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = stringResource(if (isEditMode) R.string.save else R.string.rule_wizard_create),
                                    tint = if (canSave) MaterialTheme.notix.onPrimary else MaterialTheme.notix.contentPrimary.copy(alpha = 0.38f),
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.notix.background,
                    ),
                )
            },
            bottomBar = {},
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = MaterialTheme.notixSpacing.lg),
            ) {
                // ===== 0. 规则名称（可选，复用 BlockerRule.description） =====
                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.rule_wizard_name_hint)) },
                    singleLine = true,
                    shape = NotixCorner.Control,
                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                )

                Spacer(Modifier.height(MaterialTheme.notixSpacing.xl))

                // ===== 1. 来源 =====
                SectionHeader(title = stringResource(R.string.rule_wizard_section_source))
                Spacer(Modifier.height(MaterialTheme.notixSpacing.sm))

                if (!showAppPicker) {
                    SourceSummaryCard(
                        packageNames = effectivePackages,
                        appNames = effectivePackages.map { appDisplayName(it) },
                        selectedCount = effectivePackages.size,
                        placeholder = stringResource(R.string.rule_wizard_source_app_none),
                        onClick = { showAppPicker = true },
                    )
                } else {
                    AppPickerPanel(
                        knownApps = knownApps,
                        selectedPackages = effectivePackages,
                        onAppToggle = { pkg ->
                            selectedPackages = if (pkg in effectivePackages) {
                                effectivePackages - pkg
                            } else {
                                effectivePackages + pkg
                            }
                        },
                        onDone = { showAppPicker = false },
                    )
                }

                Spacer(Modifier.height(MaterialTheme.notixSpacing.xl))

                // ===== 2. 条件（摘要展示 + 添加条件弹窗） =====
                SectionHeader(title = stringResource(R.string.rule_wizard_section_conditions))
                Spacer(Modifier.height(MaterialTheme.notixSpacing.sm))

                ConditionSummaryCard(
                    hasKeywords = hasKeywords,
                    keywordSummary = keywordSummaryText(matchMode, includeKeywords, excludeKeywords),
                    phoneStateSummary = phoneStateConditionSummaryText(
                        screenState = screenState,
                        chargingState = chargingState,
                        dndState = dndState,
                        bluetoothState = bluetoothState,
                        bluetoothDeviceNames = bluetoothDeviceNames,
                    ),
                    timeSummary = timeConditionSummaryText(
                        timeEnabled = timeEnabled,
                        startHour = startHour,
                        startMinute = startMinute,
                        endHour = endHour,
                        endMinute = endMinute,
                        selectedWeekdays = selectedWeekdays,
                    ),
                    onClick = {
                        conditionTab = if (hasKeywords) 1 else 0
                        showConditionDialog = true
                    },
                )

                Spacer(Modifier.height(MaterialTheme.notixSpacing.xl))

                // ===== 4. Action Flow（工作流） =====
                SectionHeader(title = stringResource(R.string.rule_wizard_section_workflow))
                Spacer(Modifier.height(MaterialTheme.notixSpacing.sm))

                ActionFlowSection(
                    actions = actionFlow,
                    editingIndex = editingActionIndex,
                    onEditIndex = { editingActionIndex = it },
                    onAdd = { type ->
                        actionFlow = RuleWizardSupport.actionFlowAdd(actionFlow, type)
                        if (RuleWizardSupport.hasActionParams(type)) {
                            editingActionIndex = actionFlow.lastIndex
                        }
                    },
                    onRemove = { index ->
                        actionFlow = RuleWizardSupport.actionFlowRemoveAt(actionFlow, index)
                        if (editingActionIndex == index) editingActionIndex = -1
                        else if (editingActionIndex > index) editingActionIndex--
                    },
                    onMove = { from, to ->
                        if (from != to) {
                            actionFlow = RuleWizardSupport.actionFlowMove(actionFlow, from, to)
                            if (editingActionIndex == from) editingActionIndex = to
                            else if (editingActionIndex == to) editingActionIndex = from
                        }
                    },
                    onUpdate = { index, spec ->
                        actionFlow = RuleWizardSupport.actionFlowUpdate(actionFlow, index, spec)
                    },
                    onCloseEdit = { editingActionIndex = -1 },
                )

                if (actionFlow.isEmpty()) {
                    Spacer(Modifier.height(MaterialTheme.notixSpacing.sm))
                    Text(
                        text = stringResource(R.string.rule_wizard_at_least_one_action),
                        style = MaterialTheme.notixType.caption,
                        color = MaterialTheme.notix.error,
                    )
                }

                Spacer(Modifier.height(MaterialTheme.notixSpacing.xl))

                // -- Duplicate warning --
                val duplicate = RuleWizardSupport.isDuplicate(
                    existingRules,
                    effectivePackages,
                    RuleCondition(
                        mode = matchMode,
                        includeKeywords = includeKeywords,
                        excludeKeywords = excludeKeywords,
                    ),
                    actionFlow,
                ) && !(isEditMode && editingRule?.let { e ->
                    // v7.13：空安全兜底——旧数据可能残留 null 字段
                    e.sourcePackages.orEmpty().map { it.packageName }.toSet() == effectivePackages.toSet() &&
                        (e.condition ?: RuleCondition()).mode == matchMode &&
                        (e.condition ?: RuleCondition()).includeKeywords == includeKeywords &&
                        (e.condition ?: RuleCondition()).excludeKeywords == excludeKeywords &&
                        e.actions == actionFlow
                } == true)
                if (duplicate) {
                    Spacer(Modifier.height(MaterialTheme.notixSpacing.md))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.notix.errorContainer,
                                shape = MaterialTheme.shapes.small,
                            )
                            .padding(MaterialTheme.notixSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.notix.onErrorContainer,
                        )
                        Spacer(Modifier.width(MaterialTheme.notixSpacing.md))
                        Text(
                            text = stringResource(R.string.rule_wizard_duplicate_warning),
                            style = MaterialTheme.notixType.bodySecondary,
                            color = MaterialTheme.notix.onErrorContainer,
                        )
                    }
                }

                Spacer(Modifier.height(MaterialTheme.notixSpacing.xxl))
            }
        }
    }

    // ===== 条件配置弹窗（v7.50：三栏化，含关键字/手机状态/时间 tab） =====
    if (showConditionDialog) {
        ConditionConfigDialog(
            initialTab = conditionTab,
            matchMode = matchMode,
            onMatchModeChange = { matchMode = it },
            includeKeywords = includeKeywords,
            excludeKeywords = excludeKeywords,
            onAddIncludeKeyword = { k ->
                if (k.isNotEmpty() && k !in includeKeywords) includeKeywords = includeKeywords + k
            },
            onRemoveIncludeKeyword = { k -> includeKeywords = includeKeywords - k },
            onAddExcludeKeyword = { k ->
                if (k.isNotEmpty() && k !in excludeKeywords) excludeKeywords = excludeKeywords + k
            },
            onRemoveExcludeKeyword = { k -> excludeKeywords = excludeKeywords - k },
            screenState = screenState,
            onScreenStateChange = { screenState = it },
            chargingState = chargingState,
            onChargingStateChange = { chargingState = it },
            dndState = dndState,
            onDndStateChange = { dndState = it },
            bluetoothState = bluetoothState,
            onBluetoothStateChange = { bluetoothState = it },
            bluetoothDeviceNames = bluetoothDeviceNames,
            onBluetoothDeviceNamesChange = { bluetoothDeviceNames = it },
            timeEnabled = timeEnabled,
            onTimeEnabledChange = { timeEnabled = it },
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            onStartChange = { h, m -> startHour = h; startMinute = m },
            onEndChange = { h, m -> endHour = h; endMinute = m },
            selectedWeekdays = selectedWeekdays,
            onWeekdayToggle = { day ->
                selectedWeekdays = if (day in selectedWeekdays) selectedWeekdays - day
                else selectedWeekdays + day
            },
            onDismiss = { showConditionDialog = false },
        )
    }
}

/** 从当前 UI 状态构建新模型规则 */
private fun buildNewRule(
    editingRule: BlockerRule?,
    description: String,
    selectedPackages: List<String>,
    appNameOf: (String) -> String,
    matchMode: MatchMode,
    includeKeywords: List<String>,
    excludeKeywords: List<String>,
    screenState: ScreenState,
    chargingState: ChargingState,
    dndState: DndState,
    bluetoothState: BluetoothState,
    bluetoothDeviceNames: List<String>,
    timeEnabled: Boolean,
    startHour: Int,
    startMinute: Int,
    endHour: Int,
    endMinute: Int,
    selectedWeekdays: Set<Int>,
    actions: List<ActionSpec>,
): BlockerRule {
    val now = System.currentTimeMillis()
    return BlockerRule(
        id = editingRule?.id ?: "",
        isEnabled = editingRule?.isEnabled ?: true,
        hitCount = editingRule?.hitCount ?: 0,
        sourcePackages = selectedPackages.map { pkg ->
            SourceApp(packageName = pkg, appName = appNameOf(pkg).ifBlank { pkg }.takeIf { it != pkg })
        },
        condition = RuleCondition(
            mode = matchMode,
            includeKeywords = includeKeywords.distinct(),
            excludeKeywords = excludeKeywords.distinct(),
        ),
        extraCondition = ExtraCondition(
            screenState = screenState,
            chargingState = chargingState,
            dndState = dndState,
            bluetoothState = bluetoothState,
            bluetoothDeviceNames = bluetoothDeviceNames.distinct(),
            time = TimeCondition(
                enabled = timeEnabled,
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                weekdays = selectedWeekdays.sorted(),
            ),
        ),
        // 阶段3A：直接保存 UI 唯一状态 actionFlow（顺序 == actions 顺序）
        actions = actions,
        description = description,
        createdAt = editingRule?.createdAt ?: now,
    )
}

// ---------------------------------------------------------------------------
// Section header
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.notix.contentSecondary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 2.dp),
    )
}

// ---------------------------------------------------------------------------
// Source summary card (来源栏入口卡片：来源应用 + 已选应用)
// ---------------------------------------------------------------------------

@Composable
private fun SourceSummaryCard(
    packageNames: List<String>,
    appNames: List<String>,
    selectedCount: Int,
    placeholder: String,
    onClick: () -> Unit,
) {
    val hasSelection = selectedCount > 0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NotixCorner.Control)
            .clickable(onClick = onClick),
        shape = NotixCorner.Control,
        colors = CardDefaults.cardColors(
            containerColor = if (hasSelection) MaterialTheme.notix.primaryContainer
            else MaterialTheme.notix.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = MaterialTheme.notixSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.rule_wizard_source_app),
                style = MaterialTheme.notixType.bodySecondary,
                fontWeight = FontWeight.SemiBold,
                color = if (hasSelection) MaterialTheme.notix.onPrimaryContainer
                else MaterialTheme.notix.contentSecondary,
            )
            Spacer(Modifier.weight(1f))
            if (hasSelection) {
                packageNames.take(3).forEachIndexed { index, pkg ->
                    if (index > 0) Spacer(Modifier.width(2.dp))
                    RealAppIcon(
                        packageName = pkg,
                        appName = appNames.getOrNull(index),
                        size = 24.dp,
                        shape = CircleShape,
                    )
                }
                Spacer(Modifier.width(MaterialTheme.notixSpacing.sm))
                Text(
                    text = if (selectedCount == 1) appNames.first()
                    else stringResource(R.string.rule_wizard_source_app_count, selectedCount),
                    style = MaterialTheme.notixType.bodySecondary,
                    color = if (hasSelection) MaterialTheme.notix.onPrimaryContainer
                    else MaterialTheme.notix.contentSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    text = placeholder,
                    style = MaterialTheme.notixType.bodySecondary,
                    color = MaterialTheme.notix.contentSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.notix.contentSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// App picker panel (notification-history apps only)
// ---------------------------------------------------------------------------

@Composable
private fun AppPickerPanel(
    knownApps: List<KnownApp>?,
    selectedPackages: List<String>,
    onAppToggle: (String) -> Unit,
    onDone: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    // v8.10：默认展开；收起时隐藏应用列表 + 底部「完成」按钮，仅保留搜索框 + 折叠控件。
    var isAppListExpanded by rememberSaveable { mutableStateOf(true) }

    if (knownApps == null) {
        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = NotixCorner.Control,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.notix.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.notixSpacing.md)) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_apps)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search)) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear_search))
                            }
                        }
                        // v8.10：折叠/展开应用列表；展开显示 ↑（收起），收起显示 ↓（展开）
                        IconButton(onClick = { isAppListExpanded = !isAppListExpanded }) {
                            Icon(
                                imageVector = if (isAppListExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (isAppListExpanded) {
                                    stringResource(R.string.collapse)
                                } else {
                                    stringResource(R.string.expand)
                                }
                            )
                        }
                    }
                },
                singleLine = true,
                shape = NotixCorner.Sm,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            )

            Spacer(Modifier.height(MaterialTheme.notixSpacing.sm))

            // v8.10：收起时整段隐藏（应用列表 + 完成按钮），仅保留搜索框
            if (isAppListExpanded) {
                val filtered = remember(knownApps, searchQuery) {
                    if (searchQuery.isBlank()) knownApps.take(50)
                    else {
                        val q = searchQuery.lowercase()
                        knownApps.filter {
                            it.appName?.lowercase()?.contains(q) == true ||
                                    it.packageName.lowercase().contains(q)
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.Apps,
                        title = stringResource(R.string.rule_wizard_no_known_apps_title),
                    )
                }

                // v7.14：应用列表限高（约 5~6 项），多余项列表内部滚动，避免撑长整个界面
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filtered) { app ->
                        val selected = app.packageName in selectedPackages
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(NotixCorner.Sm)
                                .then(
                                    if (selected) Modifier.border(1.5.dp, MaterialTheme.notix.primary, NotixCorner.Sm)
                                    else Modifier
                                )
                                .clickable { onAppToggle(app.packageName) }
                                .padding(horizontal = MaterialTheme.notixSpacing.sm, vertical = MaterialTheme.notixSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .then(
                                        if (selected) Modifier.background(MaterialTheme.notix.primary)
                                        else Modifier.border(1.5.dp, MaterialTheme.notix.outline, RoundedCornerShape(4.dp))
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            RealAppIcon(
                                packageName = app.packageName,
                                appName = app.appName,
                                size = 32.dp,
                                shape = CircleShape,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName ?: app.packageName,
                                    style = MaterialTheme.notixType.bodySecondary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (app.appName != null) {
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.notixType.caption,
                                        color = MaterialTheme.notix.contentSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(MaterialTheme.notixSpacing.sm))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDone) {
                        Text(
                            text = if (selectedPackages.isEmpty()) {
                                stringResource(R.string.rule_wizard_app_picker_done)
                            } else {
                                "${stringResource(R.string.rule_wizard_app_picker_done)}（${selectedPackages.size}）"
                            }
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Condition summary text helpers
// ---------------------------------------------------------------------------

@Composable
private fun keywordSummaryText(
    matchMode: MatchMode,
    includeKeywords: List<String>,
    excludeKeywords: List<String>,
): String {
    val parts = mutableListOf<String>()
    if (includeKeywords.isNotEmpty()) {
        parts.add(
            stringResource(R.string.rule_wizard_keyword_include) + ": " +
                includeKeywords.take(2).joinToString(", ") +
                if (includeKeywords.size > 2) " +${includeKeywords.size - 2}" else ""
        )
    }
    if (matchMode == MatchMode.MIXED && excludeKeywords.isNotEmpty()) {
        parts.add(
            stringResource(R.string.rule_wizard_keyword_exclude) + ": " +
                excludeKeywords.take(2).joinToString(", ") +
                if (excludeKeywords.size > 2) " +${excludeKeywords.size - 2}" else ""
        )
    }
    return parts.joinToString(" · ")
}

@Composable
private fun phoneStateConditionSummaryText(
    screenState: ScreenState,
    chargingState: ChargingState,
    dndState: DndState,
    bluetoothState: BluetoothState,
    bluetoothDeviceNames: List<String>,
): String {
    val parts = mutableListOf<String>()
    if (screenState != ScreenState.ANY) parts.add(screenStateLabel(screenState))
    if (chargingState != ChargingState.ANY) parts.add(chargingStateLabel(chargingState))
    if (dndState != DndState.ANY) parts.add(dndStateLabel(dndState))
    if (bluetoothState != BluetoothState.ANY) parts.add(bluetoothStateLabel(bluetoothState))
    if (bluetoothDeviceNames.isNotEmpty()) {
        parts.add(stringResource(R.string.rule_wizard_extra_bt_devices) + ": " + bluetoothDeviceNames.joinToString(","))
    }
    return parts.joinToString(" · ")
}

@Composable
private fun timeConditionSummaryText(
    timeEnabled: Boolean,
    startHour: Int,
    startMinute: Int,
    endHour: Int,
    endMinute: Int,
    selectedWeekdays: Set<Int>,
): String {
    if (!timeEnabled) return ""
    return "%02d:%02d-%02d:%02d".format(startHour, startMinute, endHour, endMinute) +
        if (selectedWeekdays.isNotEmpty()) " " + selectedWeekdays.sorted().joinToString(",") else ""
}

// ---------------------------------------------------------------------------
// Condition summary card + config dialog (三栏化：关键字/手机状态/时间)
// ---------------------------------------------------------------------------

@Composable
private fun ConditionSummaryCard(
    hasKeywords: Boolean,
    keywordSummary: String,
    phoneStateSummary: String,
    timeSummary: String,
    onClick: () -> Unit,
) {
    val summaryLines = listOf(
        keywordSummary.takeIf { hasKeywords },
        phoneStateSummary.takeIf { it.isNotBlank() },
        timeSummary.takeIf { it.isNotBlank() },
    ).filterNotNull()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NotixCorner.Control)
            .clickable(onClick = onClick),
        shape = NotixCorner.Control,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.notix.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = MaterialTheme.notixSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.rule_wizard_condition_title),
                    style = MaterialTheme.notixType.bodySecondary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.notix.contentSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.height(MaterialTheme.notixSpacing.xs))
            if (summaryLines.isNotEmpty()) {
                summaryLines.forEachIndexed { index, line ->
                    if (index > 0) Spacer(Modifier.height(2.dp))
                    Text(
                        text = line,
                        style = MaterialTheme.notixType.caption,
                        color = MaterialTheme.notix.contentSecondary,
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.rule_wizard_condition_none),
                    style = MaterialTheme.notixType.caption,
                    color = MaterialTheme.notix.contentSecondary,
                )
            }
        }
    }
}

@Composable
private fun ConditionConfigDialog(
    initialTab: Int,
    matchMode: MatchMode,
    onMatchModeChange: (MatchMode) -> Unit,
    includeKeywords: List<String>,
    excludeKeywords: List<String>,
    onAddIncludeKeyword: (String) -> Unit,
    onRemoveIncludeKeyword: (String) -> Unit,
    onAddExcludeKeyword: (String) -> Unit,
    onRemoveExcludeKeyword: (String) -> Unit,
    screenState: ScreenState,
    onScreenStateChange: (ScreenState) -> Unit,
    chargingState: ChargingState,
    onChargingStateChange: (ChargingState) -> Unit,
    dndState: DndState,
    onDndStateChange: (DndState) -> Unit,
    bluetoothState: BluetoothState,
    onBluetoothStateChange: (BluetoothState) -> Unit,
    bluetoothDeviceNames: List<String>,
    onBluetoothDeviceNamesChange: (List<String>) -> Unit,
    timeEnabled: Boolean,
    onTimeEnabledChange: (Boolean) -> Unit,
    startHour: Int,
    startMinute: Int,
    endHour: Int,
    endMinute: Int,
    onStartChange: (Int, Int) -> Unit,
    onEndChange: (Int, Int) -> Unit,
    selectedWeekdays: Set<Int>,
    onWeekdayToggle: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    NotixDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.rule_wizard_condition_title),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp),
        ) {
            TabRow(
                selectedTabIndex = tab,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text(stringResource(R.string.rule_wizard_condition_keywords)) },
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text(stringResource(R.string.rule_wizard_condition_phone_state)) },
                )
                Tab(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    text = { Text(stringResource(R.string.rule_wizard_condition_time)) },
                )
            }
            Spacer(Modifier.height(MaterialTheme.notixSpacing.md))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                when (tab) {
                    0 -> Column {
                        var showIncludeDialog by rememberSaveable { mutableStateOf(false) }
                        var showExcludeDialog by rememberSaveable { mutableStateOf(false) }
                        var includeInitial by rememberSaveable { mutableStateOf("") }
                        var excludeInitial by rememberSaveable { mutableStateOf("") }
                        MatchModePicker(mode = matchMode, onModeSelected = onMatchModeChange)
                        Spacer(Modifier.height(MaterialTheme.notixSpacing.md))
                        // v8.12：关键字直接显示在条件配置界面，点击 chip 可编辑，+ 号打开输入弹窗。
                        KeywordChipRow(
                            label = if (matchMode == MatchMode.MIXED) {
                                stringResource(R.string.rule_wizard_keyword_contains_a)
                            } else {
                                stringResource(R.string.rule_wizard_keyword_include)
                            },
                            keywords = includeKeywords,
                            // 点击 chip 主体进入编辑：先把原词从列表移除并回填到弹窗输入框
                            onEditKeyword = { kw ->
                                onRemoveIncludeKeyword(kw)
                                includeInitial = kw
                                showIncludeDialog = true
                            },
                            onRemoveKeyword = onRemoveIncludeKeyword,
                            onAddClick = {
                                includeInitial = ""
                                showIncludeDialog = true
                            },
                        )
                        if (matchMode == MatchMode.MIXED) {
                            Spacer(Modifier.height(MaterialTheme.notixSpacing.md))
                            KeywordChipRow(
                                label = stringResource(R.string.rule_wizard_keyword_not_contains_b),
                                keywords = excludeKeywords,
                                // 点击 chip 主体进入编辑：先把原词从列表移除并回填到弹窗输入框
                                onEditKeyword = { kw ->
                                    onRemoveExcludeKeyword(kw)
                                    excludeInitial = kw
                                    showExcludeDialog = true
                                },
                                onRemoveKeyword = onRemoveExcludeKeyword,
                                onAddClick = {
                                    excludeInitial = ""
                                    showExcludeDialog = true
                                },
                            )
                        }
                        if (showIncludeDialog) {
                            KeywordInputDialog(
                                title = if (matchMode == MatchMode.MIXED) {
                                    stringResource(R.string.rule_wizard_keyword_contains_a)
                                } else {
                                    stringResource(R.string.rule_wizard_keyword_include)
                                },
                                keywords = includeKeywords,
                                initialInput = includeInitial,
                                onAdd = onAddIncludeKeyword,
                                onRemove = onRemoveIncludeKeyword,
                                onDismiss = {
                                    showIncludeDialog = false
                                    includeInitial = ""
                                },
                            )
                        }
                        if (matchMode == MatchMode.MIXED && showExcludeDialog) {
                            KeywordInputDialog(
                                title = stringResource(R.string.rule_wizard_keyword_not_contains_b),
                                keywords = excludeKeywords,
                                initialInput = excludeInitial,
                                onAdd = onAddExcludeKeyword,
                                onRemove = onRemoveExcludeKeyword,
                                onDismiss = {
                                    showExcludeDialog = false
                                    excludeInitial = ""
                                },
                            )
                        }
                    }
                    1 -> PhoneStateSection(
                        screenState = screenState,
                        onScreenStateChange = onScreenStateChange,
                        chargingState = chargingState,
                        onChargingStateChange = onChargingStateChange,
                        dndState = dndState,
                        onDndStateChange = onDndStateChange,
                        bluetoothState = bluetoothState,
                        onBluetoothStateChange = onBluetoothStateChange,
                        bluetoothDeviceNames = bluetoothDeviceNames,
                        onBluetoothDeviceNamesChange = onBluetoothDeviceNamesChange,
                    )
                    2 -> TimeSection(
                        timeEnabled = timeEnabled,
                        onTimeEnabledChange = onTimeEnabledChange,
                        startHour = startHour,
                        startMinute = startMinute,
                        endHour = endHour,
                        endMinute = endMinute,
                        onStartChange = onStartChange,
                        onEndChange = onEndChange,
                        selectedWeekdays = selectedWeekdays,
                        onWeekdayToggle = onWeekdayToggle,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Match mode picker（弹窗选择，与 NotixDialog 风格一致）
// ---------------------------------------------------------------------------

@Composable
private fun MatchModePicker(
    mode: MatchMode,
    onModeSelected: (MatchMode) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { showDialog = true },
            shape = NotixCorner.Sm,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.rule_wizard_mode_label) + "：" + matchModeLabel(mode),
                style = MaterialTheme.notixType.bodySecondary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        if (showDialog) {
            MatchModePickerDialog(
                currentMode = mode,
                onModeSelected = {
                    onModeSelected(it)
                    showDialog = false
                },
                onDismiss = { showDialog = false },
            )
        }
    }
}

@Composable
private fun MatchModePickerDialog(
    currentMode: MatchMode,
    onModeSelected: (MatchMode) -> Unit,
    onDismiss: () -> Unit,
) {
    NotixDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.rule_wizard_mode_label),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            MatchMode.entries.forEachIndexed { index, m ->
                val disabled = m == MatchMode.ADVANCED
                val selected = m == currentMode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !disabled) {
                            if (!disabled) {
                                onModeSelected(m)
                            }
                        }
                        .padding(vertical = MaterialTheme.notixSpacing.md, horizontal = MaterialTheme.notixSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = matchModeLabel(m),
                            style = MaterialTheme.notixType.body,
                            color = if (disabled) {
                                MaterialTheme.notix.contentPrimary.copy(alpha = 0.38f)
                            } else {
                                MaterialTheme.notix.contentPrimary
                            },
                        )
                        if (m == MatchMode.ADVANCED) {
                            Text(
                                text = stringResource(R.string.rule_wizard_mode_advanced_hint),
                                style = MaterialTheme.notixType.caption,
                                color = MaterialTheme.notix.contentSecondary,
                            )
                        }
                    }
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.notix.primary,
                        )
                    }
                }
                if (index < MatchMode.entries.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun matchModeLabel(mode: MatchMode): String = when (mode) {
    MatchMode.CONTAINS_ANY -> stringResource(R.string.rule_wizard_mode_contains_any)
    MatchMode.CONTAINS_ALL -> stringResource(R.string.rule_wizard_mode_contains_all)
    MatchMode.NOT_CONTAINS_ANY -> stringResource(R.string.rule_wizard_mode_not_contains_any)
    MatchMode.NOT_CONTAINS_ALL -> stringResource(R.string.rule_wizard_mode_not_contains_all)
    MatchMode.MIXED -> stringResource(R.string.rule_wizard_mode_mixed)
    MatchMode.ADVANCED -> stringResource(R.string.rule_wizard_mode_advanced)
}

// ---------------------------------------------------------------------------
// Keyword input（v8.12：点击按钮弹出 NotixDialog 风格输入弹窗）
// ---------------------------------------------------------------------------

/**
 * 关键字 chip 行：直接展示已选关键字，点击 chip 主体进入编辑，尾部关闭图标删除。
 * 标题行右侧提供「+」按钮打开输入弹窗添加新关键字。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeywordChipRow(
    label: String,
    keywords: List<String>,
    onEditKeyword: (String) -> Unit,
    onRemoveKeyword: (String) -> Unit,
    onAddClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.notixType.bodySecondary,
                fontWeight = FontWeight.Medium,
            )
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.rule_wizard_add_keyword),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(MaterialTheme.notixSpacing.xs))
        if (keywords.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.notixSpacing.xs),
            ) {
                keywords.forEach { keyword ->
                    InputChip(
                        selected = false,
                        onClick = { onEditKeyword(keyword) },
                        label = { Text(keyword) },
                        trailingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .clickable { onRemoveKeyword(keyword) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.remove),
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        },
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.rule_wizard_keyword_placeholder),
                style = MaterialTheme.notixType.caption,
                color = MaterialTheme.notix.contentSecondary,
            )
        }
    }
}

/**
 * 关键字输入弹窗（NotixDialog 风格）：标题 + 已选 chip 列表 + 多行输入框。
 * 点击 chip 主体回填输入框并移除原词，尾部关闭图标直接删除；底部「确定」提交输入并关闭。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeywordInputDialog(
    title: String,
    keywords: List<String>,
    initialInput: String = "",
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by remember { mutableStateOf(initialInput) }
    val commit: () -> Unit = {
        val kw = input.trim()
        if (kw.isNotEmpty()) onAdd(kw)
        input = ""
    }
    NotixDialog(
        onDismiss = onDismiss,
        title = title,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                if (keywords.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.notixSpacing.xs),
                    ) {
                        keywords.forEach { keyword ->
                            InputChip(
                                selected = false,
                                onClick = {
                                    input = keyword
                                    onRemove(keyword)
                                },
                                label = { Text(keyword) },
                                trailingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .clickable { onRemove(keyword) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.remove),
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(title) },
                    placeholder = { Text(stringResource(R.string.rule_wizard_keyword_placeholder)) },
                    // v8.12：长文本可换行显示
                    singleLine = false,
                    minLines = 2,
                    maxLines = 5,
                    shape = NotixCorner.Sm,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commit() }),
                )
            }
        },
        buttons = {
            NotixDialogButton(
                onClick = { commit(); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.ok),
            )
        },
    )
}

// ---------------------------------------------------------------------------
// Extra condition sections（条件弹窗内：手机状态 / 时间）
// ---------------------------------------------------------------------------

@Composable
private fun PhoneStateSection(
    screenState: ScreenState,
    onScreenStateChange: (ScreenState) -> Unit,
    chargingState: ChargingState,
    onChargingStateChange: (ChargingState) -> Unit,
    dndState: DndState,
    onDndStateChange: (DndState) -> Unit,
    bluetoothState: BluetoothState,
    onBluetoothStateChange: (BluetoothState) -> Unit,
    bluetoothDeviceNames: List<String>,
    onBluetoothDeviceNamesChange: (List<String>) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 屏幕状态
        Text(
            text = stringResource(R.string.rule_wizard_extra_screen),
            style = MaterialTheme.notixType.label,
            color = MaterialTheme.notix.contentSecondary,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ScreenState.entries.forEach { s ->
                FilterChip(
                    selected = screenState == s,
                    onClick = { onScreenStateChange(s) },
                    label = { Text(screenStateLabel(s)) },
                    shape = FilterChipDefaults.shape,
                )
            }
        }

        Spacer(Modifier.height(MaterialTheme.notixSpacing.md))

        // 充电状态
        Text(
            text = stringResource(R.string.rule_wizard_extra_charging),
            style = MaterialTheme.notixType.label,
            color = MaterialTheme.notix.contentSecondary,
        )
        Spacer(Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.notixSpacing.xs),
        ) {
            ChargingState.entries.forEach { c ->
                FilterChip(
                    selected = chargingState == c,
                    onClick = { onChargingStateChange(c) },
                    label = { Text(chargingStateLabel(c)) },
                )
            }
        }

        Spacer(Modifier.height(MaterialTheme.notixSpacing.md))

        // 勿扰模式状态
        Text(
            text = stringResource(R.string.rule_wizard_extra_dnd),
            style = MaterialTheme.notixType.label,
            color = MaterialTheme.notix.contentSecondary,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DndState.entries.forEach { d ->
                FilterChip(
                    selected = dndState == d,
                    onClick = { onDndStateChange(d) },
                    label = { Text(dndStateLabel(d)) },
                    shape = FilterChipDefaults.shape,
                )
            }
        }

        Spacer(Modifier.height(MaterialTheme.notixSpacing.md))

        // 蓝牙耳机连接状态
        Text(
            text = stringResource(R.string.rule_wizard_extra_bluetooth),
            style = MaterialTheme.notixType.label,
            color = MaterialTheme.notix.contentSecondary,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BluetoothState.entries.forEach { b ->
                FilterChip(
                    selected = bluetoothState == b,
                    onClick = { onBluetoothStateChange(b) },
                    label = { Text(bluetoothStateLabel(b)) },
                    shape = FilterChipDefaults.shape,
                )
            }
        }

        // v7.20：指定设备多选（从当前已连接蓝牙音频设备 productName 列表选择，任一命中即成立）
        Spacer(Modifier.height(10.dp))
        BluetoothDevicePicker(
            selectedNames = bluetoothDeviceNames,
            onNamesChange = onBluetoothDeviceNamesChange,
        )
    }
}

@Composable
private fun TimeSection(
    timeEnabled: Boolean,
    onTimeEnabledChange: (Boolean) -> Unit,
    startHour: Int,
    startMinute: Int,
    endHour: Int,
    endMinute: Int,
    onStartChange: (Int, Int) -> Unit,
    onEndChange: (Int, Int) -> Unit,
    selectedWeekdays: Set<Int>,
    onWeekdayToggle: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.rule_wizard_extra_time),
                style = MaterialTheme.notixType.label,
                color = MaterialTheme.notix.contentSecondary,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = timeEnabled, onCheckedChange = onTimeEnabledChange)
        }
        AnimatedVisibility(visible = timeEnabled) {
            Column {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TimeField(
                        value = String.format("%02d:%02d", startHour, startMinute),
                        onValueChange = { v -> parseTime(v)?.let { onStartChange(it.first, it.second) } },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "至",
                        style = MaterialTheme.notixType.bodySecondary,
                        modifier = Modifier.padding(horizontal = MaterialTheme.notixSpacing.sm),
                    )
                    TimeField(
                        value = String.format("%02d:%02d", endHour, endMinute),
                        onValueChange = { v -> parseTime(v)?.let { onEndChange(it.first, it.second) } },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(MaterialTheme.notixSpacing.sm))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.notixSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.notixSpacing.xs),
                ) {
                    (1..7).forEach { day ->
                        FilterChip(
                            selected = day in selectedWeekdays,
                            onClick = { onWeekdayToggle(day) },
                            label = { Text(weekdayLabel(day)) },
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun TimeField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = NotixCorner.Sm,
    )
}

private fun parseTime(s: String): Pair<Int, Int>? {
    val parts = s.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h to m
}

@Composable
private fun screenStateLabel(s: ScreenState): String = when (s) {
    ScreenState.ANY -> stringResource(R.string.rule_wizard_screen_any)
    ScreenState.SCREEN_ON -> stringResource(R.string.rule_wizard_screen_on)
    ScreenState.SCREEN_OFF -> stringResource(R.string.rule_wizard_screen_off)
}

@Composable
private fun chargingStateLabel(c: ChargingState): String = when (c) {
    ChargingState.ANY -> stringResource(R.string.rule_wizard_charging_any)
    ChargingState.WIRED -> stringResource(R.string.rule_wizard_charging_wired)
    ChargingState.WIRELESS -> stringResource(R.string.rule_wizard_charging_wireless)
    ChargingState.BATTERY -> stringResource(R.string.rule_wizard_charging_battery)
}

@Composable
private fun dndStateLabel(d: DndState): String = when (d) {
    DndState.ANY -> stringResource(R.string.rule_wizard_dnd_any)
    DndState.ON -> stringResource(R.string.rule_wizard_dnd_on)
    DndState.OFF -> stringResource(R.string.rule_wizard_dnd_off)
}

@Composable
private fun bluetoothStateLabel(b: BluetoothState): String = when (b) {
    BluetoothState.ANY -> stringResource(R.string.rule_wizard_bt_any)
    BluetoothState.CONNECTED -> stringResource(R.string.rule_wizard_bt_connected)
    BluetoothState.DISCONNECTED -> stringResource(R.string.rule_wizard_bt_disconnected)
}

/**
 * v7.20：蓝牙指定设备多选面板（仿 AppPickerPanel）。
 * 从当前已连接蓝牙音频设备（A2DP/SCO）的 productName 列表选择，多选任一命中即成立。
 */
@Composable
private fun BluetoothDevicePicker(
    selectedNames: List<String>,
    onNamesChange: (List<String>) -> Unit,
) {
    val context = LocalContext.current
    var availableDevices by remember { mutableStateOf<List<String>?>(null) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        availableDevices = withContext(Dispatchers.IO) {
            try {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val devices = am?.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty()
                devices.filter { d ->
                    d.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        d.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                }.mapNotNull { d -> d.productName?.toString()?.trim()?.takeIf { it.isNotEmpty() } }
                    .distinct()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.rule_wizard_extra_bt_devices),
                style = MaterialTheme.notixType.label,
                color = MaterialTheme.notix.contentSecondary,
                modifier = Modifier.weight(1f),
            )
            if (selectedNames.isNotEmpty()) {
                TextButton(onClick = { onNamesChange(emptyList()) }) {
                    Text(stringResource(R.string.rule_wizard_bt_devices_clear))
                }
            }
        }
        if (selectedNames.isNotEmpty()) {
            Text(
                text = stringResource(R.string.rule_wizard_bt_devices_selected, selectedNames.size) +
                    "：" + selectedNames.joinToString("、"),
                style = MaterialTheme.notixType.caption,
                color = MaterialTheme.notix.contentSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = MaterialTheme.notixSpacing.xs),
            )
        }
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.align(Alignment.Start),
        ) {
            Text(
                stringResource(
                    if (expanded) R.string.rule_wizard_bt_devices_collapse
                    else R.string.rule_wizard_bt_devices_expand
                )
            )
        }
        if (expanded) {
            val devices = availableDevices
            when {
                devices == null -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                devices.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.rule_wizard_bt_devices_none),
                        style = MaterialTheme.notixType.caption,
                        color = MaterialTheme.notix.contentSecondary,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
                else -> {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.rule_wizard_bt_devices_hint)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = NotixCorner.Sm,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                    )
                    Spacer(Modifier.height(6.dp))
                    val q = searchQuery.trim().lowercase()
                    val filtered = remember(devices, q) {
                        if (q.isEmpty()) devices else devices.filter { it.lowercase().contains(q) }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(filtered) { name ->
                            val selected = name in selectedNames
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(NotixCorner.Sm)
                                    .then(
                                        if (selected) Modifier.border(1.5.dp, MaterialTheme.notix.primary, NotixCorner.Sm)
                                        else Modifier
                                    )
                                    .clickable {
                                        onNamesChange(
                                            if (selected) selectedNames - name else selectedNames + name
                                        )
                                    }
                                    .padding(horizontal = MaterialTheme.notixSpacing.sm, vertical = MaterialTheme.notixSpacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .then(
                                            if (selected) Modifier.background(MaterialTheme.notix.primary)
                                            else Modifier.border(1.5.dp, MaterialTheme.notix.outline, RoundedCornerShape(4.dp))
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.notix.onPrimary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.notixType.bodySecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
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
private fun weekdayLabel(day: Int): String = when (day) {
    1 -> "一"; 2 -> "二"; 3 -> "三"; 4 -> "四"; 5 -> "五"; 6 -> "六"; 7 -> "日"; else -> ""
}

// ---------------------------------------------------------------------------
// Action Flow 编辑器（阶段3A）
// 唯一状态：List<ActionSpec>，所有转换走 RuleWizardSupport 纯函数。
// ---------------------------------------------------------------------------

/** ActionSpec 列表的 rememberSaveable Saver：按 Json 字符串序列化（Bundle 可存）。 */
private val actionSpecListSaver = Saver<List<ActionSpec>, List<String>>(
    save = { list -> list.map { paramsGson.toJson(it) } },
    restore = { saved ->
        saved.mapNotNull { json ->
            runCatching { paramsGson.fromJson(json, ActionSpec::class.java) }.getOrNull()
        }.filter { it.type != null && it.isValid }
    },
)

@Composable
private fun ActionFlowSection(
    actions: List<ActionSpec>,
    editingIndex: Int,
    onEditIndex: (Int) -> Unit,
    onAdd: (RuleAction) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onUpdate: (Int, ActionSpec) -> Unit,
    onCloseEdit: () -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        if (actions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = NotixCorner.Control,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.notix.surfaceVariant.copy(alpha = 0.4f)),
            ) {
                Text(
                    text = stringResource(R.string.rule_wizard_action_flow_empty),
                    style = MaterialTheme.notixType.bodySecondary,
                    color = MaterialTheme.notix.contentSecondary,
                    modifier = Modifier.padding(MaterialTheme.notixSpacing.lg),
                )
            }
        } else {
            Text(
                text = stringResource(R.string.rule_wizard_action_flow_order_hint),
                style = MaterialTheme.notixType.caption,
                color = MaterialTheme.notix.contentSecondary,
                modifier = Modifier.padding(bottom = 6.dp),
            )

            // v8.11：动作流列表迁移到 LazyColumn + sh.calvin.reorderable 接管拖动排序，
            // 实现「被拖卡片实时偏移 + 其他卡片让出 gap + spring 过渡」的完整动画指示
            val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
            val reorderableLazyListState = sh.calvin.reorderable.rememberReorderableLazyListState(lazyListState) { from, to ->
                // 用 List 的 add/remove 实现 in-place 移动（库通过 key 匹配）
                onMove(from.index, to.index)
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    // 父级 RuleWizardScreen 是 verticalScroll，LazyColumn 必须有界高度
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.notixSpacing.sm),
            ) {
                items(
                    items = actions,
                    key = { spec -> spec.toStableKey() },
                ) { spec ->
                    val index = actions.indexOf(spec)
                    ReorderableItem(
                        state = reorderableLazyListState,
                        key = spec.toStableKey(),
                    ) { isDragging ->
                        val view = LocalView.current
                        val dragHandleModifier = Modifier.draggableHandle(
                            onDragStarted = {
                                ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.DRAG_START)
                            },
                            onDragStopped = {
                                ViewCompat.performHapticFeedback(view, HapticFeedbackConstantsCompat.GESTURE_END)
                            },
                        )
                        ActionCard(
                            index = index,
                            spec = spec,
                            isEditing = editingIndex == index,
                            isDragging = isDragging,
                            onClick = { onEditIndex(index) },
                            onDelete = { onRemove(index) },
                            dragHandleModifier = dragHandleModifier,
                        )

                        // CLICK_BUTTON 后存在 DISMISS 时的组合风险提示（动态对应当前 Flow）
                        if (spec.type == RuleAction.CLICK_BUTTON &&
                            actions.drop(index + 1).any { it.type == RuleAction.DISMISS }
                        ) {
                            Text(
                                text = stringResource(R.string.rule_wizard_click_dismiss_warning),
                                style = MaterialTheme.notixType.caption,
                                color = MaterialTheme.notix.error,
                                modifier = Modifier.padding(top = MaterialTheme.notixSpacing.xs),
                            )
                        }
                        // OPEN_NOTIFICATION 后存在 DISMISS 时的组合风险提示（阶段 4C-C-B P2-6）
                        if (spec.type == RuleAction.OPEN_NOTIFICATION &&
                            actions.drop(index + 1).any { it.type == RuleAction.DISMISS }
                        ) {
                            Text(
                                text = stringResource(R.string.rule_wizard_open_dismiss_warning),
                                style = MaterialTheme.notixType.caption,
                                color = MaterialTheme.notix.error,
                                modifier = Modifier.padding(top = MaterialTheme.notixSpacing.xs),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(MaterialTheme.notixSpacing.md))
        OutlinedButton(
            onClick = { showPicker = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.rule_wizard_action_flow_add))
        }
    }
    if (showPicker) {
        ActionPickerDialog(
            onDismiss = { showPicker = false },
            onSelect = { type ->
                showPicker = false
                onAdd(type)
            },
        )
    }
    // v8.10：编辑中的动作以 NotixDialog 形式打开（不内联展开）
    if (editingIndex >= 0 && editingIndex < actions.size) {
        val editingSpec = actions[editingIndex]
        ActionConfigDialog(
            spec = editingSpec,
            onDismiss = onCloseEdit,
            onCommit = { updated ->
                onUpdate(editingIndex, updated)
                onCloseEdit()
            },
        )
    }
}

/**
 * 动作规格的稳定唯一 key（用于 LazyColumn + Reorderable 的 items key），
 * 由 type + params JSON 序列化派生，避免 index 作为 key 导致拖动后错位。
 */
private fun ActionSpec.toStableKey(): String {
    val paramsJson = params?.toString() ?: "null"
    return "$type::$paramsJson"
}

@Composable
private fun ActionCard(
    index: Int,
    spec: ActionSpec,
    isEditing: Boolean,
    isDragging: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    dragHandleModifier: Modifier,
) {
    val accent = actionAccent(spec.type)
    // v8.6：长按删除动作前二次确认（与崩溃日志弹窗统一风格）
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // v8.11 拖动反馈：拖动期间抬升 6dp + primaryContainer30% + primary 描边；
    // 释放后 animateDpAsState 150ms tween 平滑回落
    val cardElevation by animateDpAsState(
        targetValue = if (isDragging) 6.dp else if (isEditing) 1.5.dp else 0.dp,
        animationSpec = tween(durationMillis = 150),
        label = "actionCardElevation",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isDragging) Modifier.border(1.5.dp, MaterialTheme.notix.primary, NotixCorner.Control)
                else if (isEditing) Modifier.border(1.5.dp, MaterialTheme.notix.primary, NotixCorner.Control)
                else Modifier
            ),
        shape = NotixCorner.Control,
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) MaterialTheme.notix.primaryContainer.copy(alpha = 0.3f)
            else if (isEditing) MaterialTheme.notix.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.notix.surfaceVariant.copy(alpha = 0.4f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // v8.10 改造：左侧主体（序号/图标/标题/摘要）单独 clickable + longClickable
            // ——打开弹窗/长按删除；与右侧拖手柄互不冲突。
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(NotixCorner.Control)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = { showDeleteConfirm = true },
                    )
                    .padding(start = MaterialTheme.notixSpacing.md, top = 10.dp, end = MaterialTheme.notixSpacing.xs, bottom = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 序号
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(accent.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.notixType.label,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                        )
                    }
                    Spacer(Modifier.width(MaterialTheme.notixSpacing.sm))
                    Icon(
                        imageVector = actionIcon(spec.type),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = actionLabel(spec.type),
                        style = MaterialTheme.notixType.bodySecondary,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(MaterialTheme.notixSpacing.xs))
                Text(
                    text = RuleWizardSupport.actionFlowSummary(spec),
                    style = MaterialTheme.notixType.caption,
                    color = MaterialTheme.notix.contentSecondary,
                )
            }

            // v8.11 改造：拖动指示按钮接入 sh.calvin.reorderable 库的 draggableHandle；
            // 仅这一列响应拖动手势，由库接管实时偏移 + 其他卡片让位 gap + spring 过渡。
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 64.dp)
                    .then(dragHandleModifier),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = stringResource(R.string.rule_wizard_action_drag_hint),
                    tint = if (isDragging) MaterialTheme.notix.primary
                    else MaterialTheme.notix.contentSecondary,
                    modifier = Modifier.size(if (isDragging) 32.dp else 28.dp),
                )
            }
        }
    }

    if (showDeleteConfirm) {
        NotixConfirmDialog(
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            },
            title = stringResource(R.string.rule_wizard_action_delete_title),
            body = stringResource(R.string.rule_wizard_action_delete_message),
            confirmText = stringResource(R.string.delete),
            danger = true
        )
    }
}

@Composable
private fun ActionParamEditor(
    spec: ActionSpec,
    onCommit: (ActionSpec) -> Unit,
    onCancel: () -> Unit,
) {
    // v8.10 改造：取消 Card + surfaceVariant 包装。
    // 此函数现在作为「动作配置」弹窗的 content 块被 NotixDialog 调用，
    // 自身不再带外壳。8 个 when 分支保持不变。
    when (spec.type) {
                RuleAction.CLICK_BUTTON -> {
                    var label by remember(spec) {
                        mutableStateOf(spec.params?.get("buttonLabel")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty())
                    }
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.rule_wizard_button_label)) },
                        placeholder = { Text(stringResource(R.string.rule_wizard_button_label_hint)) },
                        singleLine = true,
                    )
                    Text(
                        text = stringResource(R.string.rule_wizard_click_button_match_hint),
                        style = MaterialTheme.notixType.caption,
                        color = MaterialTheme.notix.contentSecondary,
                    )
                    Text(
                        text = stringResource(R.string.rule_wizard_click_fail_hint),
                        style = MaterialTheme.notixType.caption,
                        color = MaterialTheme.notix.contentSecondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onCancel) { Text(stringResource(R.string.rule_wizard_action_flow_cancel)) }
                        Button(
                            onClick = { onCommit(RuleWizardSupport.clickButtonSpec(label)) },
                            enabled = label.isNotBlank(),
                        ) { Text(stringResource(R.string.rule_wizard_action_flow_save)) }
                    }
                }
                RuleAction.COPY -> {
                    var mode by remember(spec) {
                        mutableStateOf(
                            runCatching { CopyMode.valueOf(spec.params?.get("mode")?.asString ?: "") }
                                .getOrDefault(CopyMode.TITLE_AND_TEXT)
                        )
                    }
                    CopyMode.entries.forEach { m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(NotixCorner.Sm)
                                .clickable { mode = m }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Switch(checked = mode == m, onCheckedChange = { mode = m })
                            Spacer(Modifier.width(MaterialTheme.notixSpacing.sm))
                            Text(
                                text = copyModeLabel(m),
                                style = MaterialTheme.notixType.bodySecondary,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onCancel) { Text(stringResource(R.string.rule_wizard_action_flow_cancel)) }
                        Button(onClick = { onCommit(RuleWizardSupport.copySpec(mode)) }) {
                            Text(stringResource(R.string.rule_wizard_action_flow_save))
                        }
                    }
                }
                RuleAction.TTS -> {
                    val initialTemplate = spec.params?.get("template")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
                    var templateField by remember(spec) {
                        mutableStateOf(TextFieldValue(text = initialTemplate, selection = TextRange(initialTemplate.length)))
                    }
                    val template = templateField.text
                    OutlinedTextField(
                        value = templateField,
                        onValueChange = { templateField = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.rule_wizard_action_tts_template)) },
                        placeholder = { Text(stringResource(R.string.rule_wizard_action_tts_template_hint)) },
                        supportingText = { Text(stringResource(R.string.rule_wizard_action_tts_template_default)) },
                        minLines = 2,
                    )
                    Text(
                        text = stringResource(R.string.rule_wizard_action_tts_variables_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.notix.contentSecondary,
                        modifier = Modifier.padding(top = MaterialTheme.notixSpacing.md, bottom = MaterialTheme.notixSpacing.xs),
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.notixSpacing.sm),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.notixSpacing.sm),
                    ) {
                        val variables = listOf(
                            R.string.rule_wizard_action_tts_variable_app to "{app}",
                            R.string.rule_wizard_action_tts_variable_title to "{title}",
                            R.string.rule_wizard_action_tts_variable_text to "{text}",
                            R.string.rule_wizard_action_tts_variable_time to "{time}",
                            R.string.rule_wizard_action_tts_variable_date to "{date}",
                        )
                        variables.forEach { (labelRes, token) ->
                            AssistChip(
                                onClick = {
                                    val current = templateField
                                    val start = minOf(current.selection.start, current.selection.end)
                                    val end = maxOf(current.selection.start, current.selection.end)
                                    val newText = current.text.substring(0, start) + token + current.text.substring(end)
                                    val newCursor = start + token.length
                                    templateField = current.copy(
                                        text = newText,
                                        selection = TextRange(newCursor),
                                    )
                                },
                                label = { Text(stringResource(labelRes)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = MaterialTheme.notix.primary,
                                ),
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.rule_wizard_tts_wait_hint),
                        style = MaterialTheme.notixType.caption,
                        color = MaterialTheme.notix.contentSecondary,
                        modifier = Modifier.padding(top = MaterialTheme.notixSpacing.sm),
                    )
                    Text(
                        text = stringResource(R.string.rule_wizard_tts_fail_hint),
                        style = MaterialTheme.notixType.caption,
                        color = MaterialTheme.notix.contentSecondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onCancel) { Text(stringResource(R.string.rule_wizard_action_flow_cancel)) }
                        Button(onClick = { onCommit(RuleWizardSupport.ttsSpec(template)) }) {
                            Text(stringResource(R.string.rule_wizard_action_flow_save))
                        }
                    }
                }
                RuleAction.DELAY -> {
                    var msText by remember(spec) {
                        mutableStateOf(
                            (spec.params?.get("durationMs")?.takeIf { it.isJsonPrimitive }?.asLong ?: 1000L).toString()
                        )
                    }
                    val ms = msText.toLongOrNull()
                    val valid = ms != null && ms > 0L
                    OutlinedTextField(
                        value = msText,
                        onValueChange = { msText = it.filter(Char::isDigit) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.rule_wizard_delay_duration)) },
                        suffix = { Text("ms") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = !valid,
                        supportingText = {
                            if (!valid) {
                                Text(stringResource(R.string.rule_wizard_delay_invalid))
                            } else {
                                Column {
                                    Text(stringResource(R.string.rule_wizard_delay_desc))
                                    Text(stringResource(R.string.rule_wizard_delay_units_hint))
                                }
                            }
                        },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onCancel) { Text(stringResource(R.string.rule_wizard_action_flow_cancel)) }
                        Button(
                            onClick = { if (ms != null) onCommit(RuleWizardSupport.delaySpec(ms)) },
                            enabled = valid,
                        ) { Text(stringResource(R.string.rule_wizard_action_flow_save)) }
                    }
                }
                RuleAction.STRONG_REMIND -> {
                    // v8.10 新增：执行层 TODO，参数面板先就位（响铃/震动开关）
                    var sound by remember(spec) {
                        mutableStateOf(
                            spec.params?.get("sound")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: true
                        )
                    }
                    var vibrate by remember(spec) {
                        mutableStateOf(
                            spec.params?.get("vibrate")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: true
                        )
                    }
                    Text(
                        text = stringResource(R.string.rule_wizard_strong_remind_desc),
                        style = MaterialTheme.notixType.caption,
                        color = MaterialTheme.notix.contentSecondary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { sound = !sound }.padding(vertical = MaterialTheme.notixSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Switch(checked = sound, onCheckedChange = { sound = it })
                        Spacer(Modifier.width(MaterialTheme.notixSpacing.sm))
                        Text(stringResource(R.string.rule_wizard_strong_remind_sound))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { vibrate = !vibrate }.padding(vertical = MaterialTheme.notixSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                        Spacer(Modifier.width(MaterialTheme.notixSpacing.sm))
                        Text(stringResource(R.string.rule_wizard_strong_remind_vibrate))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onCancel) { Text(stringResource(R.string.rule_wizard_action_flow_cancel)) }
                        Button(
                            onClick = { onCommit(RuleWizardSupport.strongRemindSpec(sound, vibrate)) },
                        ) { Text(stringResource(R.string.rule_wizard_action_flow_save)) }
                    }
                }
                RuleAction.POSTPONE -> {
                    // v8.10 新增：执行层 TODO，参数面板先就位（延迟毫秒数）
                    var msText by remember(spec) {
                        mutableStateOf(
                            (spec.params?.get("delayMs")?.takeIf { it.isJsonPrimitive }?.asLong ?: 60_000L).toString()
                        )
                    }
                    val ms = msText.toLongOrNull()
                    val valid = ms != null && ms > 0L
                    OutlinedTextField(
                        value = msText,
                        onValueChange = { msText = it.filter(Char::isDigit) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.rule_wizard_postpone_duration)) },
                        suffix = { Text("ms") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = !valid,
                        supportingText = {
                            if (!valid) {
                                Text(stringResource(R.string.rule_wizard_postpone_invalid))
                            } else {
                                Text(stringResource(R.string.rule_wizard_postpone_desc))
                            }
                        },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onCancel) { Text(stringResource(R.string.rule_wizard_action_flow_cancel)) }
                        Button(
                            onClick = { if (ms != null) onCommit(RuleWizardSupport.postponeSpec(ms)) },
                            enabled = valid,
                        ) { Text(stringResource(R.string.rule_wizard_action_flow_save)) }
                    }
                }
                RuleAction.DISMISS -> {
                    // v8.13：DISMISS 弹窗：含「包括常驻通知」勾选；v8.14 起加可自定义冻结时长
                    val includeOngoingInit = spec.params?.get("includeOngoing")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
                    var includeOngoing by remember(spec) { mutableStateOf(includeOngoingInit) }
                    val durationInit = spec.params?.get("snoozeDurationMs")?.takeIf { it.isJsonPrimitive }?.asLong
                        ?.takeIf { it in SnoozeDurations.OPTIONS } ?: SnoozeDurations.DAY_7
                    // v8.14.1：release 包发现 Long 状态在 R8 优化后偶发比较异常，改用 OPTIONS 索引。
                    val initialIndex = SnoozeDurations.OPTIONS.indexOf(durationInit).coerceAtLeast(0)
                    var selectedDurationIndex by remember(spec) { mutableIntStateOf(initialIndex) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MaterialTheme.notixSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.rule_wizard_dismiss_include_ongoing),
                                style = MaterialTheme.notixType.bodySecondary,
                            )
                            Text(
                                text = stringResource(R.string.rule_wizard_dismiss_include_ongoing_desc),
                                style = MaterialTheme.notixType.caption,
                                color = MaterialTheme.notix.contentSecondary,
                            )
                        }
                        Switch(
                            checked = includeOngoing,
                            onCheckedChange = { includeOngoing = it },
                        )
                    }
                    if (includeOngoing) {
                        Spacer(Modifier.height(MaterialTheme.notixSpacing.md))
                        Text(
                            text = stringResource(R.string.rule_wizard_dismiss_snooze_duration),
                            style = MaterialTheme.notixType.label,
                            color = MaterialTheme.notix.contentSecondary,
                        )
                        Text(
                            text = stringResource(R.string.rule_wizard_dismiss_snooze_duration_desc),
                            style = MaterialTheme.notixType.caption,
                            color = MaterialTheme.notix.contentSecondary,
                        )
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.notixSpacing.xs),
                        ) {
                            SnoozeDurations.OPTIONS.forEachIndexed { index, ms ->
                                FilterChip(
                                    selected = selectedDurationIndex == index,
                                    onClick = { selectedDurationIndex = index },
                                    label = { Text(RuleWizardSupport.formatSnoozeDuration(ms)) },
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onCancel) { Text(stringResource(R.string.rule_wizard_action_flow_cancel)) }
                        Button(
                            onClick = { onCommit(RuleWizardSupport.dismissSpec(includeOngoing, SnoozeDurations.OPTIONS[selectedDurationIndex])) },
                        ) { Text(stringResource(R.string.rule_wizard_action_flow_save)) }
                    }
                }
                else -> {
                    // OPEN_NOTIFICATION：无参数，只显示说明
                    Text(
                        text = actionDescription(spec.type),
                        style = MaterialTheme.notixType.bodySecondary,
                        color = MaterialTheme.notix.contentSecondary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(onClick = onCancel) {
                            Text(stringResource(R.string.rule_wizard_action_flow_done))
                        }
                    }
                }
            }
}

@Composable
private fun ActionConfigDialog(
    spec: ActionSpec,
    onDismiss: () -> Unit,
    onCommit: (ActionSpec) -> Unit,
) {
    NotixDialog(
        onDismiss = onDismiss,
        title = actionLabel(spec.type),
        content = {
            Text(
                text = actionDescription(spec.type),
                style = MaterialTheme.notixType.caption,
                color = MaterialTheme.notix.contentSecondary,
            )
            Spacer(Modifier.height(MaterialTheme.notixSpacing.md))
            ActionParamEditor(
                spec = spec,
                onCommit = onCommit,
                onCancel = onDismiss,
            )
        },
    )
}

@Composable
private fun ActionPickerDialog(
    onDismiss: () -> Unit,
    onSelect: (RuleAction) -> Unit,
) {
    NotixDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.rule_wizard_action_flow_pick_title),
        content = {
            Text(
                text = stringResource(R.string.rule_wizard_action_flow_pick_hint),
                style = MaterialTheme.notixType.caption,
                color = MaterialTheme.notix.contentSecondary,
            )
            Spacer(Modifier.height(MaterialTheme.notixSpacing.sm))
            RuleAction.entries.forEach { action ->
                val accent = actionAccent(action)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(NotixCorner.Control)
                        .clickable { onSelect(action) }
                        .padding(horizontal = MaterialTheme.notixSpacing.xs, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(accent.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = actionIcon(action),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = actionLabel(action),
                            style = MaterialTheme.notixType.bodySecondary,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = actionDescription(action),
                            style = MaterialTheme.notixType.caption,
                            color = MaterialTheme.notix.contentSecondary,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun copyModeLabel(mode: CopyMode): String = when (mode) {
    CopyMode.TITLE -> stringResource(R.string.rule_wizard_copy_title)
    CopyMode.TEXT -> stringResource(R.string.rule_wizard_copy_text)
    CopyMode.TITLE_AND_TEXT -> stringResource(R.string.rule_wizard_copy_title_and_text)
}

@Composable
private fun actionAccent(action: RuleAction): Color = when (action) {
    RuleAction.DISMISS -> MaterialTheme.notix.error
    RuleAction.CLICK_BUTTON -> MaterialTheme.colorScheme.secondary
    RuleAction.OPEN_NOTIFICATION -> MaterialTheme.notix.primary
    RuleAction.COPY -> MaterialTheme.colorScheme.secondary
    RuleAction.TTS -> MaterialTheme.colorScheme.tertiary
    RuleAction.STRONG_REMIND -> MaterialTheme.notix.error
    RuleAction.DELAY -> MaterialTheme.notix.primary
    RuleAction.POSTPONE -> MaterialTheme.colorScheme.tertiary
}

@Composable
private fun actionIcon(action: RuleAction): ImageVector = when (action) {
    RuleAction.DISMISS -> Icons.Default.NotificationsOff
    RuleAction.CLICK_BUTTON -> Icons.Default.TouchApp
    RuleAction.OPEN_NOTIFICATION -> Icons.Default.OpenInNew
    RuleAction.COPY -> Icons.Default.ContentCopy
    RuleAction.TTS -> Icons.Default.VolumeUp
    RuleAction.STRONG_REMIND -> Icons.Default.PriorityHigh
    RuleAction.DELAY -> Icons.Default.DateRange
    RuleAction.POSTPONE -> Icons.Default.Schedule
}

@Composable
private fun actionLabel(action: RuleAction): String = when (action) {
    RuleAction.DISMISS -> stringResource(R.string.rule_action_dismiss)
    RuleAction.CLICK_BUTTON -> stringResource(R.string.rule_action_click_button)
    RuleAction.OPEN_NOTIFICATION -> stringResource(R.string.rule_action_open_notification)
    RuleAction.COPY -> stringResource(R.string.rule_action_copy)
    RuleAction.TTS -> stringResource(R.string.rule_action_tts)
    RuleAction.STRONG_REMIND -> stringResource(R.string.rule_action_strong_remind)
    RuleAction.DELAY -> stringResource(R.string.rule_action_wait)
    RuleAction.POSTPONE -> stringResource(R.string.rule_action_postpone)
}

@Composable
private fun actionDescription(action: RuleAction): String = when (action) {
    RuleAction.DISMISS -> stringResource(R.string.rule_action_desc_dismiss)
    RuleAction.CLICK_BUTTON -> stringResource(R.string.rule_action_desc_click_button)
    RuleAction.OPEN_NOTIFICATION -> stringResource(R.string.rule_action_desc_open_notification)
    RuleAction.COPY -> stringResource(R.string.rule_action_desc_copy)
    RuleAction.TTS -> stringResource(R.string.rule_action_desc_tts)
    RuleAction.STRONG_REMIND -> stringResource(R.string.rule_action_desc_strong_remind)
    RuleAction.DELAY -> stringResource(R.string.rule_action_desc_wait)
    RuleAction.POSTPONE -> stringResource(R.string.rule_action_desc_postpone)
}
