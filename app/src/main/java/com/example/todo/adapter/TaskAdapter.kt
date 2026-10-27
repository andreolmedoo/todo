package com.example.todo.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.todo.R
import com.example.todo.model.Task
import com.example.todo.util.DateUtils

/** Builds each row of the task ListView. */
class TaskAdapter(
    context: Context,
    tasks: List<Task>
) : ArrayAdapter<Task>(context, R.layout.item_task, tasks.toMutableList()) {

    fun replaceAll(tasks: List<Task>) {
        clear()
        addAll(tasks)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_task, parent, false)

        val holder = view.tag as? ViewHolder ?: ViewHolder(view).also { view.tag = it }
        val task = getItem(position) ?: return view

        holder.title.text = task.title

        if (task.description.isBlank()) {
            holder.description.visibility = View.GONE
        } else {
            holder.description.visibility = View.VISIBLE
            holder.description.text = task.description
        }

        val priorityLabel = context.getString(
            when (Task.normalizePriority(task.priority)) {
                Task.PRIORITY_HIGH -> R.string.priority_high
                Task.PRIORITY_LOW -> R.string.priority_low
                else -> R.string.priority_medium
            }
        )

        holder.details.text = if (task.dueDate.isBlank()) {
            context.getString(R.string.item_details_without_date, priorityLabel)
        } else {
            context.getString(
                R.string.item_details_with_date,
                priorityLabel,
                DateUtils.toDisplayDate(task.dueDate)
            )
        }

        val overdue = DateUtils.isOverdue(task.dueDate, task.completed)
        val statusRes = when {
            task.completed -> R.string.status_completed
            overdue -> R.string.status_overdue
            else -> R.string.status_pending
        }
        val colorRes = when {
            task.completed -> R.color.status_completed
            overdue -> R.color.status_overdue
            else -> R.color.status_pending
        }

        holder.status.setText(statusRes)
        holder.status.setTextColor(ContextCompat.getColor(context, colorRes))

        return view
    }

    private class ViewHolder(view: View) {
        val title: TextView = view.findViewById(R.id.tvItemTitle)
        val description: TextView = view.findViewById(R.id.tvItemDescription)
        val details: TextView = view.findViewById(R.id.tvItemDetails)
        val status: TextView = view.findViewById(R.id.tvItemStatus)
    }
}
