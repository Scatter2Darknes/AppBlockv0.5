package com.example.appblock.tasks

import android.app.*
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*
import android.graphics.Color
import java.util.*
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.fragment.app.Fragment
import com.example.appblock.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.util.Log


class TaskFragment : Fragment() {
    private lateinit var adapter: ArrayAdapter<Task>
    private val taskList = mutableListOf<Task>()
    private val filteredList = mutableListOf<Task>()
    private lateinit var spinner: Spinner
    private lateinit var listView: ListView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_task, container, false)

        listView = view.findViewById(R.id.task_list)
        val addButton = view.findViewById<Button>(R.id.add_task_button)
        spinner = view.findViewById(R.id.task_filter_spinner)

        adapter = object : ArrayAdapter<Task>(requireContext(), R.layout.task_list_item, filteredList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.task_list_item, parent, false)
                val task = getItem(position)!!
                val textView = view.findViewById<TextView>(R.id.task_text)
                val dueView = view.findViewById<TextView>(R.id.task_due_date)
                val checkBox = view.findViewById<CheckBox>(R.id.task_checkbox)

                textView.text = task.name

                if (task.completed) {
                    textView.setTextColor(Color.GRAY)
                    checkBox.visibility = View.GONE
                    dueView.visibility = View.GONE
                } else {
                    textView.setTextColor(Color.BLACK)
                    checkBox.visibility = View.VISIBLE
                    checkBox.isChecked = false
                    dueView.visibility = View.VISIBLE
                    dueView.text = "Due: ${task.dueDate}"

                    checkBox.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            task.completed = true
                            task.dueDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                            saveTasks()
                            filterTasks()
                        }
                    }
                }
                return view
            }
        }

        listView.adapter = adapter

        val prefs = requireContext().getSharedPreferences("tasks", Context.MODE_PRIVATE)
        val json = prefs.getString("task_list", "[]")
        val type = object : TypeToken<MutableList<Task>>() {}.type
        taskList.addAll(Gson().fromJson(json, type))

        addButton.setOnClickListener {
            showTaskDialog(null)
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val task = filteredList[position]
            Log.d("TaskClick", "Clicked task: ${task.name}, completed=${task.completed}")

            if (task.completed) {
                showCompletedDialog(task, taskList.indexOf(task))
            } else {
                showIncompleteTaskDialog(task, position)
            }
        }

        val options = listOf("Incomplete", "Completed")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                filterTasks()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        filterTasks()
        return view
    }

    private fun filterTasks() {
        val selected = spinner.selectedItem.toString()
        filteredList.clear()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val sorted = taskList.sortedWith(compareBy<Task> { !it.completed }.thenBy {
            try { sdf.parse(it.dueDate) } catch (e: Exception) { null }
        })

        when (selected) {
            "Completed" -> filteredList.addAll(sorted.filter { it.completed })
            "Incomplete" -> filteredList.addAll(sorted.filter { !it.completed })
        }
        adapter.notifyDataSetChanged()
    }

    private fun showTaskDialog(task: Task?, index: Int? = null) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_task, null)

        val calendar = Calendar.getInstance()
        val nameField = dialogView.findViewById<EditText>(R.id.task_name)
        val descField = dialogView.findViewById<EditText>(R.id.task_description)
        val dueField = dialogView.findViewById<EditText>(R.id.task_due)

        dueField.isFocusable = false
        dueField.isClickable = true
        dueField.setOnClickListener {
            DatePickerDialog(requireContext(), { _, year, month, day ->
                TimePickerDialog(requireContext(), { _, hour, minute ->
                    calendar.set(year, month, day, hour, minute)
                    val formatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(calendar.time)
                    dueField.setText(formatted)
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        nameField.setText(task?.name ?: "")
        descField.setText(task?.description ?: "")
        dueField.setText(task?.dueDate ?: "")

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(if (task == null) "Create Task" else "Edit Task")
            .setView(dialogView)
            .setPositiveButton(if (task == null) "Add" else "Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val name = nameField.text.toString().trim()

                if (name.isBlank()) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Error")
                        .setMessage("Task name empty")
                        .setPositiveButton("OK", null)
                        .show()
                    return@setOnClickListener
                }

                val newTask = Task(
                    name = name,
                    description = descField.text.toString().trim(),
                    dueDate = dueField.text.toString().trim(),
                    completed = task?.completed ?: false
                )

                if (index != null) {
                    val realIndex = taskList.indexOf(filteredList[index])
                    taskList[realIndex] = newTask
                } else {
                    taskList.add(newTask)
                }
                saveTasks()
                filterTasks()
                dialog.dismiss()
            }
        }

        if (task != null && index != null) {
            dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Delete") { _, _ ->
                val realIndex = taskList.indexOf(filteredList[index])
                taskList.removeAt(realIndex)
                saveTasks()
                filterTasks()
            }
        }

        dialog.show()
    }

    private fun showCompletedDialog(task: Task, index: Int) {
        val message = "Description: ${task.description}\nCompleted on: ${task.dueDate}"
        AlertDialog.Builder(requireContext())
            .setTitle(task.name)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNegativeButton("Delete") { _, _ ->
                taskList.removeAt(index)
                saveTasks()
                filterTasks()
            }
            .show()
    }

    private fun showIncompleteTaskDialog(task: Task, index: Int) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_task_incomplete, null)

        val titleView = dialogView.findViewById<TextView>(R.id.task_title)
        val descView = dialogView.findViewById<TextView>(R.id.task_description)
        val dueView = dialogView.findViewById<TextView>(R.id.task_due_date)
        val checkBox = dialogView.findViewById<CheckBox>(R.id.task_finished_checkbox)

        titleView.text = task.name
        descView.text = task.description
        dueView.text = "Due: ${task.dueDate}"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .setNegativeButton("Edit", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                dialog.dismiss()
                showTaskDialog(task, index)
            }

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (checkBox.isChecked) {
                    task.completed = true
                    task.dueDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                    saveTasks()
                    filterTasks()
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }


    private fun saveTasks() {
        val prefs = requireContext().getSharedPreferences("tasks", Context.MODE_PRIVATE)
        val json = Gson().toJson(taskList)
        prefs.edit().putString("task_list", json).apply()
    }
}
