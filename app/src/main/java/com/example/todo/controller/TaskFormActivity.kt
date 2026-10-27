package com.example.todo.controller

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.todo.R
import com.example.todo.dao.TaskDAO
import com.example.todo.model.Task
import com.example.todo.util.DateUtils

/** Receives EXTRA_TASK_ID: 0 creates a new task, any id above zero edits that task. */
class TaskFormActivity : AppCompatActivity() {

    private lateinit var dao: TaskDAO

    private lateinit var tvFormTitle: TextView
    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var etDueDate: EditText
    private lateinit var rgPriority: RadioGroup
    private lateinit var cbCompleted: CheckBox
    private lateinit var btnDeleteTask: Button

    private var taskId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_task_form)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.taskFormRoot)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dao = TaskDAO(this)

        tvFormTitle = findViewById(R.id.tvFormTitle)
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        etDueDate = findViewById(R.id.etDueDate)
        rgPriority = findViewById(R.id.rgPriority)
        cbCompleted = findViewById(R.id.cbCompleted)
        btnDeleteTask = findViewById(R.id.btnDeleteTask)

        val btnSaveTask = findViewById<Button>(R.id.btnSaveTask)
        val btnCancel = findViewById<Button>(R.id.btnCancel)

        taskId = intent.getIntExtra(EXTRA_TASK_ID, 0)
        if (taskId > 0) {
            loadTask(taskId)
        }

        btnSaveTask.setOnClickListener { save() }
        btnDeleteTask.setOnClickListener { delete() }
        btnCancel.setOnClickListener { finish() }
    }

    override fun onDestroy() {
        dao.close()
        super.onDestroy()
    }

    private fun loadTask(id: Int) {
        val task = dao.getById(id)
        if (task == null) {
            Toast.makeText(this, R.string.error_task_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvFormTitle.setText(R.string.edit_task)
        etTitle.setText(task.title)
        etDescription.setText(task.description)
        etDueDate.setText(
            if (task.dueDate.isBlank()) "" else DateUtils.toDisplayDate(task.dueDate)
        )
        cbCompleted.isChecked = task.completed
        rgPriority.check(
            when (Task.normalizePriority(task.priority)) {
                Task.PRIORITY_HIGH -> R.id.rbPriorityHigh
                Task.PRIORITY_LOW -> R.id.rbPriorityLow
                else -> R.id.rbPriorityMedium
            }
        )
        btnDeleteTask.visibility = View.VISIBLE
    }

    private fun save() {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val typedDate = etDueDate.text.toString().trim()

        if (title.isEmpty()) {
            showError(R.string.error_title_required, etTitle)
            return
        }

        if (title.length > MAX_TITLE_LENGTH) {
            showError(R.string.error_title_too_long, etTitle)
            return
        }

        if (description.length > MAX_DESCRIPTION_LENGTH) {
            showError(R.string.error_description_too_long, etDescription)
            return
        }

        val isoDueDate = if (typedDate.isEmpty()) {
            ""
        } else {
            DateUtils.toIsoDate(typedDate) ?: run {
                showError(R.string.error_invalid_date, etDueDate)
                return
            }
        }

        val task = Task(
            id = taskId,
            title = title,
            description = description,
            dueDate = isoDueDate,
            completed = cbCompleted.isChecked,
            priority = selectedPriority()
        )

        if (taskId > 0) {
            dao.update(task)
            Toast.makeText(this, R.string.task_updated, Toast.LENGTH_SHORT).show()
        } else {
            dao.insert(task)
            Toast.makeText(this, R.string.task_created, Toast.LENGTH_SHORT).show()
        }

        finish()
    }

    private fun delete() {
        if (taskId <= 0) return

        dao.delete(taskId)
        Toast.makeText(this, R.string.task_deleted, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun selectedPriority(): Int = when (rgPriority.checkedRadioButtonId) {
        R.id.rbPriorityHigh -> Task.PRIORITY_HIGH
        R.id.rbPriorityLow -> Task.PRIORITY_LOW
        else -> Task.PRIORITY_MEDIUM
    }

    private fun showError(messageRes: Int, field: EditText) {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
        field.requestFocus()
    }

    companion object {
        const val EXTRA_TASK_ID = "com.example.todo.EXTRA_TASK_ID"

        private const val MAX_TITLE_LENGTH = 100
        private const val MAX_DESCRIPTION_LENGTH = 500
    }
}
