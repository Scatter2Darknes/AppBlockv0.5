package com.example.appblock.tasks

data class Task(
    var name: String,
    var description: String,
    var dueDate: String
, var completed: Boolean = false)