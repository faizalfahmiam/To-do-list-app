package com.todolistapptest

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mbexample.alarmmanager.alarm.AlarmSchedulerImpl
import com.todolistapptest.adapters.SwipeController
import com.todolistapptest.adapters.SwipeControllerActions
import com.todolistapptest.adapters.TaskListAdapter
import com.todolistapptest.databinding.ActivityMainBinding
import com.todolistapptest.models.Task
import com.todolistapptest.utils.Status
import com.todolistapptest.utils.StatusResult
import com.todolistapptest.utils.StatusResult.Added
import com.todolistapptest.utils.StatusResult.Deleted
import com.todolistapptest.utils.StatusResult.Updated
import com.todolistapptest.utils.clearEditText
import com.todolistapptest.utils.hideKeyBoard
import com.todolistapptest.utils.longToastShow
import com.todolistapptest.utils.setupDialog
import com.todolistapptest.utils.validateEditText
import com.todolistapptest.viewmodels.TaskViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.UUID


class MainActivity : AppCompatActivity() {
    var date_time = ""
    var mYear = 0
    var mMonth = 0
    var mDay = 0
    var mHour = 0
    var mMinute = 0
    var hasNotificationPermissionGranted = false
    var task: Task? = null
    var recyclerView: RecyclerView? = null
    var taskListAdapter: TaskListAdapter? = null
    var swipeController: SwipeController? = null
    lateinit var alarmSchedulerImpl: AlarmSchedulerImpl
    private val mainBinding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val addTaskDialog: Dialog by lazy {
        Dialog(this, R.style.DialogCustomTheme).apply {
            setupDialog(R.layout.add_task_dialog)
        }
    }
    private val updateTaskDialog: Dialog by lazy {
        Dialog(this, R.style.DialogCustomTheme).apply {
            setupDialog(R.layout.update_task_dialog)
        }
    }
    private val loadingDialog: Dialog by lazy {
        Dialog(this, R.style.DialogCustomTheme).apply {
            setupDialog(R.layout.loading_dialog)
        }
    }
    private val taskViewModel: TaskViewModel by lazy {
        ViewModelProvider(this)[TaskViewModel::class.java]
    }
    private val isListMutableLiveData = MutableLiveData<Boolean>().apply {
        postValue(true)
    }
    private fun isPermissionGranted() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            hasNotificationPermissionGranted = isGranted
            if (!isGranted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Build.VERSION.SDK_INT >= 33) {
                        if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                            showNotificationPermissionRationale()
                        } else {
                            showSettingDialog()
                        }
                    }
                }
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mainBinding.root)
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            hasNotificationPermissionGranted = true
            alarmSchedulerImpl = AlarmSchedulerImpl(this)
        }
        recyclerView = mainBinding.taskRV
        // Add task start
        val addCloseImg = addTaskDialog.findViewById<ImageView>(R.id.closeImg)
        addCloseImg.setOnClickListener { addTaskDialog.dismiss() }

        val addETTitle = addTaskDialog.findViewById<TextInputEditText>(R.id.edTaskTitle)
        val addETTitleL = addTaskDialog.findViewById<TextInputLayout>(R.id.edTaskTitleL)

        addETTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateEditText(addETTitle, addETTitleL)
            }

        })

        val addETDesc = addTaskDialog.findViewById<TextInputEditText>(R.id.edTaskDesc)
        val addETDescL = addTaskDialog.findViewById<TextInputLayout>(R.id.edTaskDescL)

        addETDesc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateEditText(addETDesc, addETDescL)
            }
        })

        mainBinding.addTaskFABtn.setOnClickListener {
            if (isPermissionGranted()) {
                clearEditText(addETTitle, addETTitleL)
                clearEditText(addETDesc, addETDescL)
                addTaskDialog.show()
            } else {
                activityResultLauncher.launch(
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
        val addDateTimeTxt = addTaskDialog.findViewById<TextInputEditText>(R.id.edDateTime)
        addDateTimeTxt.setOnClickListener {
            datePicker(addDateTimeTxt)
        }
        val saveTaskBtn = addTaskDialog.findViewById<Button>(R.id.saveTaskBtn)
        val statusComplateYes = addTaskDialog.findViewById<RadioButton>(R.id.radio_complated_yes)
        val statusComplateNot = addTaskDialog.findViewById<RadioButton>(R.id.radio_complated_not)
        statusComplateYes.setOnClickListener {
            statusComplateNot.isChecked = false
        }
        statusComplateNot.setOnClickListener {
            statusComplateYes.isChecked = false
        }
        saveTaskBtn.setOnClickListener {
            if (validateEditText(addETTitle, addETTitleL)
                && validateEditText(addETDesc, addETDescL)
            ) {
                val dateNew = SimpleDateFormat("dd-MM-yyyy HH:mm").parse(date_time)
                var statusComplate = statusComplateYes.isChecked

                val newTask = Task(
                    UUID.randomUUID().toString(),
                    addETTitle.text.toString().trim(),
                    addETDesc.text.toString().trim(),
                    dateNew,statusComplate
                )
                hideKeyBoard(it)
                addTaskDialog.dismiss()
                newTask.let { AlarmSchedulerImpl::schedule }
                taskViewModel.insertTask(newTask)
            }
        }
        // Add task end

        // Update Task Start
        val updateETTitle = updateTaskDialog.findViewById<TextInputEditText>(R.id.edTaskTitle)
        val updateETTitleL = updateTaskDialog.findViewById<TextInputLayout>(R.id.edTaskTitleL)
        val updateDateTimeTxt = updateTaskDialog.findViewById<TextInputEditText>(R.id.edDateTime)
        updateDateTimeTxt.setOnClickListener {
            datePicker(updateDateTimeTxt)
        }
        updateETTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateEditText(updateETTitle, updateETTitleL)
            }

        })

        val updateETDesc = updateTaskDialog.findViewById<TextInputEditText>(R.id.edTaskDesc)
        val updateETDescL = updateTaskDialog.findViewById<TextInputLayout>(R.id.edTaskDescL)
        val edDateTime = updateTaskDialog.findViewById<TextInputEditText>(R.id.edDateTime)

        updateETDesc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateEditText(updateETDesc, updateETDescL)
            }
        })
        val updateCloseImg = updateTaskDialog.findViewById<ImageView>(R.id.closeImg)
        updateCloseImg.setOnClickListener { updateTaskDialog.dismiss() }

        val updateTaskBtn = updateTaskDialog.findViewById<Button>(R.id.updateTaskBtn)

        // Update Task End

        isListMutableLiveData.observe(this){
            if (it){
                mainBinding.taskRV.layoutManager = LinearLayoutManager(
                    this,LinearLayoutManager.VERTICAL,false
                )
            }else{
                mainBinding.taskRV.layoutManager = StaggeredGridLayoutManager(
                    2,LinearLayoutManager.VERTICAL
                )
            }
        }



        val statusUpdateComplateYes = updateTaskDialog.findViewById<RadioButton>(R.id.radio_complated_yes)
        val statusUpdateComplateNot = updateTaskDialog.findViewById<RadioButton>(R.id.radio_complated_not)

        statusUpdateComplateYes.setOnClickListener {
            statusComplateNot.isChecked = false
        }
        statusUpdateComplateNot.setOnClickListener {
            statusComplateYes.isChecked = false
        }
        taskListAdapter = TaskListAdapter(isListMutableLiveData ) { type, position, task ->
            if (type == "delete") {
                taskViewModel
                    // Deleted Task
                    .deleteTaskUsingId(task.id)
                task.let(alarmSchedulerImpl::cancel)
                // Restore Deleted task
                restoreDeletedTask(task)
            } else if (type == "update") {
                updateETTitle.setText(task.title)
                updateETDesc.setText(task.description)
                date_time = SimpleDateFormat("dd-MM-yyyy HH:mm").format(task.date)
                edDateTime.setText(date_time)
                if(task.status){
                    statusUpdateComplateYes.isChecked=true
                    statusUpdateComplateNot.isChecked=false
                }else{
                    statusUpdateComplateYes.isChecked=false
                    statusUpdateComplateNot.isChecked=true
                }
                updateTaskBtn.setOnClickListener {
                    if (validateEditText(updateETTitle, updateETTitleL)
                        && validateEditText(updateETDesc, updateETDescL)
                    ) {
                        val dateNew = SimpleDateFormat("dd-MM-yyyy HH:mm").parse(date_time)
                        var statusComplate = statusUpdateComplateYes.isChecked

                        val updateTask = Task(
                            task.id,
                            updateETTitle.text.toString().trim(),
                            updateETDesc.text.toString().trim(),
//                           here i Date updated
                           dateNew,statusComplate
                        )
                        hideKeyBoard(it)
                        updateTaskDialog.dismiss()
                        updateTask.let(alarmSchedulerImpl::cancel)
                        updateTask.let(alarmSchedulerImpl::schedule)
                        taskViewModel
                            .updateTask(updateTask)
                    }
                }
                updateTaskDialog.show()
            }
            else if (type == "cbstatus") {
                val updateTask = Task(
                    task.id,
                    task.title,
                    task.description,
                    task.date,
                    !task.status
                )
                taskViewModel
                    .updateTask(updateTask)
            }
        }
        mainBinding.taskRV.adapter = taskListAdapter
        ViewCompat.setNestedScrollingEnabled(mainBinding.taskRV,false)
        taskListAdapter?.registerAdapterDataObserver(object :
            RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                mainBinding.nestedScrollView.smoothScrollTo(0,positionStart)
            }
        })
        callGetTaskList(taskListAdapter!!)
        callSortByLiveData()
        statusCallback()
        callSearch()
        val swipeController = SwipeController(object : SwipeControllerActions() {
            override fun onRightClicked(position: Int) {
                task = taskListAdapter?.currentList?.get(position)
                taskViewModel
                    // Deleted Task
                    .deleteTaskUsingId(task!!.id)
                alarmSchedulerImpl.cancel(task!!)
                // Restore Deleted task
                restoreDeletedTask(task!!)
            }

            override fun onLeftClicked(position: Int) {
                super.onLeftClicked(position)
                task = taskListAdapter?.currentList?.get(position)
                updateETTitle.setText(task?.title)
                updateETDesc.setText(task?.description)
                date_time = SimpleDateFormat("dd-MM-yyyy HH:mm").format(task?.date)
                edDateTime.setText(date_time)
                if(task?.status==true){
                    statusUpdateComplateYes.isChecked=true
                    statusUpdateComplateNot.isChecked=false
                }else{
                    statusUpdateComplateYes.isChecked=false
                    statusUpdateComplateNot.isChecked=true
                }
                updateTaskBtn.setOnClickListener {
                    if (validateEditText(updateETTitle, updateETTitleL)
                        && validateEditText(updateETDesc, updateETDescL)
                    ) {
                        val dateNew = SimpleDateFormat("dd-MM-yyyy HH:mm").parse(date_time)
                        var statusComplate = statusUpdateComplateYes.isChecked

                        val updateTask = Task(
                            task?.id!!,
                            updateETTitle.text.toString().trim(),
                            updateETDesc.text.toString().trim(),
//                           here i Date updated
                            dateNew,statusComplate
                        )
                        hideKeyBoard(it)
                        updateTaskDialog.dismiss()
                        updateTask.let(alarmSchedulerImpl::cancel)
                        updateTask.let(alarmSchedulerImpl::schedule)
                        taskViewModel
                            .updateTask(updateTask)
                    }
                }
                updateTaskDialog.show()
            }
        })

        val itemTouchhelper = ItemTouchHelper(swipeController)
        itemTouchhelper.attachToRecyclerView(recyclerView)

        recyclerView!!.addItemDecoration(object : ItemDecoration() {
            override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                swipeController.onDraw(c)
            }
        })
    }
    private fun restoreDeletedTask(deletedTask : Task){
        val snackBar = Snackbar.make(
            mainBinding.root, "Deleted '${deletedTask.title}'",
            Snackbar.LENGTH_LONG
        )
        snackBar.setAction("Undo"){
            taskViewModel.insertTask(deletedTask)
        }
        snackBar.show()
    }
    private fun callSearch() {
        mainBinding.edSearch.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(query: Editable) {
                if (query.toString().isNotEmpty()){
                    taskViewModel.searchTaskList(query.toString())
                }else{
                    callSortByLiveData()
                }
            }
        })

        mainBinding.edSearch.setOnEditorActionListener{ v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH){
                hideKeyBoard(v)
                return@setOnEditorActionListener true
            }
            false
        }

    }
    private fun callSortByLiveData(){
        taskViewModel.sortByLiveData.observe(this){
            taskViewModel.getTaskList(it.second,it.first)
        }
    }
    private fun statusCallback() {
        taskViewModel
            .statusLiveData
            .observe(this) {
                when (it.status) {
                    Status.LOADING -> {
                        loadingDialog.show()
                    }

                    Status.SUCCESS -> {
                        loadingDialog.dismiss()
                        when (it.data as StatusResult) {
                            Added -> {
                                Log.d("StatusResult", "Added")
                            }

                            Deleted -> {
                                Log.d("StatusResult", "Deleted")

                            }

                            Updated -> {
                                Log.d("StatusResult", "Updated")

                            }
                        }
                        it.message?.let { it1 -> longToastShow(it1) }
                    }

                    Status.ERROR -> {
                        loadingDialog.dismiss()
                        it.message?.let { it1 -> longToastShow(it1) }
                    }
                }
            }
    }
    private fun callGetTaskList(taskRecyclerViewAdapter: TaskListAdapter) {
        CoroutineScope(Dispatchers.Main).launch {
            taskViewModel
                .taskStateFlow
                .collectLatest {
                    Log.d("status", it.status.toString())

                    when (it.status) {
                        Status.LOADING -> {
                            loadingDialog.show()
                        }

                        Status.SUCCESS -> {
                            loadingDialog.dismiss()
                            it.data?.collect { taskList ->
                                taskRecyclerViewAdapter.submitList(taskList)
                            }
                        }

                        Status.ERROR -> {
                            loadingDialog.dismiss()
                            it.message?.let { it1 -> longToastShow(it1) }
                        }
                    }

                }
        }
    }
    private fun datePicker(addDateTimeTxt: TextInputEditText) {

        // Get Current Date
        val c = Calendar.getInstance()
        mYear = c[Calendar.YEAR]
        mMonth = c[Calendar.MONTH]
        mDay = c[Calendar.DAY_OF_MONTH]
        val datePickerDialog = DatePickerDialog(this,
            { view, year, monthOfYear, dayOfMonth ->
                date_time = dayOfMonth.toString() + "-" + (monthOfYear + 1) + "-" + year
                //*************Call Time Picker Here ********************
                timePicker(addDateTimeTxt)
            }, mYear, mMonth, mDay
        )
        datePickerDialog.show()
    }
    private fun timePicker(addDateTimeTxt: TextInputEditText) {
        // Get Current Time
        val c = Calendar.getInstance()
        mHour = c[Calendar.HOUR_OF_DAY]
        mMinute = c[Calendar.MINUTE]

        // Launch Time Picker Dialog
        val timePickerDialog = TimePickerDialog(this,
            { view, hourOfDay, minute ->
                mHour = hourOfDay
                mMinute = minute
                date_time += " $hourOfDay:$minute"
                addDateTimeTxt.setText("$date_time $hourOfDay:$minute")
            }, mHour, mMinute, false
        )
        timePickerDialog.show()
    }
    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissions ->
        if (permissions) {
            val addETTitle = addTaskDialog.findViewById<TextInputEditText>(R.id.edTaskTitle)
            val addETTitleL = addTaskDialog.findViewById<TextInputLayout>(R.id.edTaskTitleL)
            val addETDesc = addTaskDialog.findViewById<TextInputEditText>(R.id.edTaskDesc)
            val addETDescL = addTaskDialog.findViewById<TextInputLayout>(R.id.edTaskDescL)
            clearEditText(addETTitle, addETTitleL)
            clearEditText(addETDesc, addETDescL)
            addTaskDialog.show()
        } else {
            showEducationalDialog()
        }
    }
    private fun showEducationalDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_denied)
            .setMessage(R.string.request_msg)
            .setNegativeButton(R.string.close) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setPositiveButton(R.string.settings) { dialog, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
                dialog.dismiss()
            }
            .setCancelable(false)
        dialog.show()
    }
    private fun showSettingDialog() {
        this.let {
            MaterialAlertDialogBuilder(it, com.google.android.material.R.style.MaterialAlertDialog_Material3)
                .setTitle("Notification Permission")
                .setMessage("Notification permission is required, Please allow notification permission from setting")
                .setPositiveButton("Ok") { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:$this.packageName")
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
    private fun showNotificationPermissionRationale() {
        this.let {
            MaterialAlertDialogBuilder(it, com.google.android.material.R.style.MaterialAlertDialog_Material3)
                .setTitle("Alert")
                .setMessage("Notification permission is required, to show notification")
                .setPositiveButton("Ok") { _, _ ->
                    if (Build.VERSION.SDK_INT >= 33) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}