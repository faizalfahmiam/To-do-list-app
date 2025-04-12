package com.todolistapptest.alarm

import com.todolistapptest.models.Task

interface AlarmScheduler {
    fun schedule(task: Task)
    fun cancel(task: Task)
}