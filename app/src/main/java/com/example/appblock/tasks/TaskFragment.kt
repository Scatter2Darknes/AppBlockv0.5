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
    private var currentlyShowingCompleted = false
    private lateinit var adapter: ArrayAdapter<Task>
    private val taskList = mutableListOf<Task>()
    private val filteredList = mutableListOf<Task>()
    private lateinit var listView: ListView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_task, container, false)

        listView = view.findViewById(R.id.task_list)
        val addButton = view.findViewById<Button>(R.id.add_task_button)

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

                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    val now = Date()
                    val dueDate = try { sdf.parse(task.dueDate) } catch (e: Exception) { null }

                    if (dueDate != null && dueDate.before(now)) {
                        dueView.text = "Overdue: ${task.dueDate}"
                        dueView.setTextColor(Color.RED)
                    } else {

                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        val now = Date()
                        val dueDateParsed = try { sdf.parse(task.dueDate) } catch (e: Exception) { null }

                        if (dueDateParsed != null && dueDateParsed.before(now)) {
                            dueView.text = "Overdue: ${task.dueDate}"
                            dueView.setTextColor(Color.RED)
                        } else {
                            dueView.text = "Due: ${task.dueDate}"
                            dueView.setTextColor(Color.BLACK)
                        }

                        dueView.setTextColor(Color.BLACK)
                    }


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

        val buttonIncomplete = view.findViewById<Button>(R.id.button_incomplete)
        val buttonComplete = view.findViewById<Button>(R.id.button_complete)

        buttonIncomplete.setOnClickListener {
            filterTasks(showCompleted = false)
        }

        buttonComplete.setOnClickListener {
            filterTasks(showCompleted = true)
        }

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

        filterTasks()
        return view
    }

    private fun filterTasks(showCompleted: Boolean = false) {
        currentlyShowingCompleted = showCompleted
        val selected = if (showCompleted) "Completed" else "Incomplete"
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
                filterTasks(currentlyShowingCompleted)
                //scheduleNotification(newTask)
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

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val now = Date()
        val completedDate = try { sdf.parse(task.dueDate) } catch (e: Exception) { null }
        val overdueTag = if (completedDate != null && completedDate.before(now)) "" else ""
        val message = "Description: ${task.description}\nDue: ${task.dueDate}\nCompleted on: ${task.dueDate}$overdueTag"

        AlertDialog.Builder(requireContext())
            .setTitle(task.name)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNegativeButton("Delete") { _, _ ->
                taskList.removeAt(index)
                saveTasks()
                filterTasks(showCompleted = true)
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

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val now = Date()
        val dueDate = try { sdf.parse(task.dueDate) } catch (e: Exception) { null }

        if (dueDate != null && dueDate.before(now)) {
            dueView.text = "Overdue: ${task.dueDate}"
            dueView.setTextColor(Color.RED)
        } else {

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val now = Date()
            val dueDateParsed = try { sdf.parse(task.dueDate) } catch (e: Exception) { null }

            if (dueDateParsed != null && dueDateParsed.before(now)) {
                dueView.text = "Overdue: ${task.dueDate}"
                dueView.setTextColor(Color.RED)
            } else {
                dueView.text = "Due: ${task.dueDate}"
                dueView.setTextColor(Color.BLACK)
            }

            dueView.setTextColor(Color.BLACK)
        }


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

/*private fun scheduleNotification(task: Task) {
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dueDate = sdf.parse(task.dueDate) ?: return
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), TaskDueReceiver::class.java).apply {
            putExtra("task_name", task.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            task.name.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, dueDate.time, pendingIntent)
    } catch (e: Exception) {
        Log.e("Alarm", "Failed to schedule alarm: ${e.message}")
    }
}
*/