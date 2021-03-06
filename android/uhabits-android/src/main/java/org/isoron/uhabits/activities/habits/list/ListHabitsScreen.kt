/*
 * Copyright (C) 2016 Álinson Santos Xavier <isoron@gmail.com>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.activities.habits.list

import android.app.*
import android.content.*
import android.util.*
import androidx.annotation.*
import androidx.appcompat.app.*
import dagger.*
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.dialogs.*
import org.isoron.uhabits.activities.habits.edit.*
import org.isoron.uhabits.activities.habits.list.views.*
import org.isoron.uhabits.core.commands.*
import org.isoron.uhabits.core.models.*
import org.isoron.uhabits.core.tasks.*
import org.isoron.uhabits.core.ui.*
import org.isoron.uhabits.core.ui.callbacks.*
import org.isoron.uhabits.core.ui.screens.habits.list.*
import org.isoron.uhabits.core.ui.screens.habits.list.ListHabitsBehavior.Message.*
import org.isoron.uhabits.inject.*
import org.isoron.uhabits.intents.*
import org.isoron.uhabits.tasks.*
import org.isoron.uhabits.utils.*
import java.io.*
import javax.inject.*

const val RESULT_IMPORT_DATA = 101
const val RESULT_EXPORT_CSV = 102
const val RESULT_EXPORT_DB = 103
const val RESULT_BUG_REPORT = 104
const val RESULT_REPAIR_DB = 105
const val REQUEST_OPEN_DOCUMENT = 106
const val REQUEST_SETTINGS = 107

@ActivityScope
class ListHabitsScreen
@Inject constructor(
        @ActivityContext val context: Context,
        private val commandRunner: CommandRunner,
        private val intentFactory: IntentFactory,
        private val themeSwitcher: ThemeSwitcher,
        private val adapter: HabitCardListAdapter,
        private val taskRunner: TaskRunner,
        private val exportDBFactory: ExportDBTaskFactory,
        private val importTaskFactory: ImportDataTaskFactory,
        private val confirmDeleteDialogFactory: ConfirmDeleteDialogFactory,
        private val confirmSyncKeyDialogFactory: ConfirmSyncKeyDialogFactory,
        private val colorPickerFactory: ColorPickerDialogFactory,
        private val numberPickerFactory: NumberPickerFactory,
        private val behavior: Lazy<ListHabitsBehavior>
) : CommandRunner.Listener,
    ListHabitsBehavior.Screen,
    ListHabitsMenuBehavior.Screen,
    ListHabitsSelectionMenuBehavior.Screen {

    val activity = (context as AppCompatActivity)

    fun onAttached() {
        commandRunner.addListener(this)
        if(activity.intent.action == "android.intent.action.VIEW") {
            val uri = activity.intent.data!!.toString()
            val parts = uri.replace(Regex("^.*sync/"), "").split("#")
            val syncKey = parts[0]
            val encKey = parts[1]
            Log.i("ListHabitsScreen", "sync: $syncKey enc: $encKey")
            behavior.get().onSyncKeyOffer(syncKey, encKey)
        }
    }

    fun onDettached() {
        commandRunner.removeListener(this)
    }

    override fun onCommandFinished(command: Command) {
        val msg = getExecuteString(command)
        if (msg != null) activity.showMessage(msg)
    }

    fun onResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_OPEN_DOCUMENT -> onOpenDocumentResult(resultCode, data)
            REQUEST_SETTINGS -> onSettingsResult(resultCode)
        }
    }

    private fun onOpenDocumentResult(resultCode: Int, data: Intent?) {
        if (data == null) return
        if (resultCode != Activity.RESULT_OK) return
        try {
            val inStream = activity.contentResolver.openInputStream(data.data!!)!!
            val cacheDir = activity.externalCacheDir
            val tempFile = File.createTempFile("import", "", cacheDir)
            inStream.copyTo(tempFile)
            onImportData(tempFile) { tempFile.delete() }
        } catch (e: IOException) {
            activity.showMessage(activity.resources.getString(R.string.could_not_import))
            e.printStackTrace()
        }
    }

    private fun onSettingsResult(resultCode: Int) {
        when (resultCode) {
            RESULT_IMPORT_DATA -> showImportScreen()
            RESULT_EXPORT_CSV -> behavior.get().onExportCSV()
            RESULT_EXPORT_DB -> onExportDB()
            RESULT_BUG_REPORT -> behavior.get().onSendBugReport()
            RESULT_REPAIR_DB -> behavior.get().onRepairDB()
        }
    }

    override fun applyTheme() {
        themeSwitcher.apply()
        activity.restartWithFade(ListHabitsActivity::class.java)
    }

    override fun showAboutScreen() {
        val intent = intentFactory.startAboutActivity(activity)
        activity.startActivity(intent)
    }

    override fun showSelectHabitTypeDialog() {
        val dialog = HabitTypeDialog()
        dialog.show(activity.supportFragmentManager, "habitType")
    }

    override fun showDeleteConfirmationScreen(callback: OnConfirmedCallback, quantity: Int) {
        confirmDeleteDialogFactory.create(callback, quantity).show()
    }

    override fun showEditHabitsScreen(habits: List<Habit>) {
        val intent = intentFactory.startEditActivity(activity, habits[0])
        activity.startActivity(intent)
    }

    override fun showFAQScreen() {
        val intent = intentFactory.viewFAQ(activity)
        activity.startActivity(intent)
    }

    override fun showHabitScreen(habit: Habit) {
        val intent = intentFactory.startShowHabitActivity(activity, habit)
        activity.startActivity(intent)
    }

    fun showImportScreen() {
        val intent = intentFactory.openDocument()
        activity.startActivityForResult(intent, REQUEST_OPEN_DOCUMENT)
    }

    override fun showIntroScreen() {
        val intent = intentFactory.startIntroActivity(activity)
        activity.startActivity(intent)
    }

    override fun showMessage(m: ListHabitsBehavior.Message) {
        activity.showMessage(activity.resources.getString(when (m) {
            COULD_NOT_EXPORT -> R.string.could_not_export
            IMPORT_SUCCESSFUL -> R.string.habits_imported
            IMPORT_FAILED -> R.string.could_not_import
            DATABASE_REPAIRED -> R.string.database_repaired
            COULD_NOT_GENERATE_BUG_REPORT -> R.string.bug_report_failed
            FILE_NOT_RECOGNIZED -> R.string.file_not_recognized
            SYNC_ENABLED -> R.string.sync_enabled
            SYNC_KEY_ALREADY_INSTALLED -> R.string.sync_key_already_installed
        }))
    }

    override fun showSendBugReportToDeveloperScreen(log: String) {
        val to = R.string.bugReportTo
        val subject = R.string.bugReportSubject
        activity.showSendEmailScreen(to, subject, log)
    }

    override fun showSendFileScreen(filename: String) {
        activity.showSendFileScreen(filename)
    }

    override fun showSettingsScreen() {
        val intent = intentFactory.startSettingsActivity(activity)
        activity.startActivityForResult(intent, REQUEST_SETTINGS)
    }

    override fun showColorPicker(defaultColor: PaletteColor,
                                 callback: OnColorPickedCallback) {
        val picker = colorPickerFactory.create(defaultColor)
        picker.setListener(callback)
        picker.show(activity.supportFragmentManager, "picker")
    }

    override fun showNumberPicker(value: Double,
                                  unit: String,
                                  callback: ListHabitsBehavior.NumberPickerCallback) {
        numberPickerFactory.create(value, unit, callback).show()
    }

    override fun showConfirmInstallSyncKey(callback: OnConfirmedCallback) {
        confirmSyncKeyDialogFactory.create(callback).show()
    }

    private fun getExecuteString(command: Command): String? {
        when (command) {
            is ArchiveHabitsCommand -> {
                return activity.resources.getQuantityString(R.plurals.toast_habits_archived,
                                                            command.selected.size)
            }
            is ChangeHabitColorCommand -> {
                return activity.resources.getQuantityString(R.plurals.toast_habits_changed,
                                                            command.selected.size)
            }
            is CreateHabitCommand -> {
                return activity.resources.getString(R.string.toast_habit_created)
            }
            is DeleteHabitsCommand -> {
                return activity.resources.getQuantityString(R.plurals.toast_habits_deleted,
                                                            command.selected.size)
            }
            is EditHabitCommand -> {
                return activity.resources.getQuantityString(R.plurals.toast_habits_changed, 1)
            }
            is UnarchiveHabitsCommand -> {
                return activity.resources.getQuantityString(R.plurals.toast_habits_unarchived,
                                                            command.selected.size)
            }
            else -> return null
        }
    }

    private fun onImportData(file: File, onFinished: () -> Unit) {
        taskRunner.execute(importTaskFactory.create(file) { result ->
            if (result == ImportDataTask.SUCCESS) {
                adapter.refresh()
                activity.showMessage(activity.resources.getString(R.string.habits_imported))
            } else if (result == ImportDataTask.NOT_RECOGNIZED) {
                activity.showMessage(activity.resources.getString(R.string.file_not_recognized))
            } else {
                activity.showMessage(activity.resources.getString(R.string.could_not_import))
            }
            onFinished()
        })
    }

    private fun onExportDB() {
        taskRunner.execute(exportDBFactory.create { filename ->
            if (filename != null) activity.showSendFileScreen(filename)
            else activity.showMessage(activity.resources.getString(R.string.could_not_export))
        })
    }
}
