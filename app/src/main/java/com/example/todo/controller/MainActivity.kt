package com.example.todo.controller

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.todo.R
import com.example.todo.adapter.TaskAdapter
import com.example.todo.dao.TaskDAO
import com.example.todo.dao.TaskFilter

class MainActivity : AppCompatActivity() {

    private lateinit var dao: TaskDAO
    private lateinit var taskAdapter: TaskAdapter

    private lateinit var etSearch: EditText
    private lateinit var spFilter: Spinner
    private lateinit var tvProgress: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var lvTasks: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        dao = TaskDAO(this)

        etSearch = findViewById(R.id.etSearch)
        spFilter = findViewById(R.id.spFilter)
        tvProgress = findViewById(R.id.tvProgress)
        tvEmpty = findViewById(R.id.tvEmpty)
        lvTasks = findViewById(R.id.lvTasks)

        val btnSearch = findViewById<Button>(R.id.btnSearch)
        val btnClearSearch = findViewById<Button>(R.id.btnClearSearch)
        val btnAddTask = findViewById<Button>(R.id.btnAddTask)

        taskAdapter = TaskAdapter(this, emptyList())
        lvTasks.adapter = taskAdapter

        // Only after the adapter exists, because the spinner fires its initial selection.
        setUpFilterSpinner()

        lvTasks.setOnItemClickListener { _, _, position, _ ->
            val task = taskAdapter.getItem(position) ?: return@setOnItemClickListener
            openForm(task.id)
        }

        btnSearch.setOnClickListener { loadTasks() }

        btnClearSearch.setOnClickListener {
            etSearch.setText("")
            spFilter.setSelection(TaskFilter.ALL.ordinal)
            loadTasks()
        }

        btnAddTask.setOnClickListener { openForm(NEW_TASK_ID) }
    }

    override fun onResume() {
        super.onResume()
        // Reload after the form closes, so new, edited and deleted tasks show up.
        loadTasks()
    }

    override fun onDestroy() {
        dao.close()
        super.onDestroy()
    }

    private fun setUpFilterSpinner() {
        val filterAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.task_filters,
            android.R.layout.simple_spinner_item
        )
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spFilter.adapter = filterAdapter

        spFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                loadTasks()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun loadTasks() {
        val search = etSearch.text.toString()
        val filter = TaskFilter.fromPosition(spFilter.selectedItemPosition)
        val tasks = dao.list(search, filter)

        taskAdapter.replaceAll(tasks)
        tvEmpty.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE

        updateProgress()
    }

    private fun updateProgress() {
        val total = dao.countAll()
        val completed = dao.countCompleted()
        val percentage = if (total == 0) 0 else completed * 100 / total

        tvProgress.text = getString(R.string.progress_format, completed, total, percentage)
    }

    private fun openForm(taskId: Int) {
        val intent = Intent(this, TaskFormActivity::class.java)
        intent.putExtra(TaskFormActivity.EXTRA_TASK_ID, taskId)
        startActivity(intent)
    }

    companion object {
        private const val NEW_TASK_ID = 0
    }
}
