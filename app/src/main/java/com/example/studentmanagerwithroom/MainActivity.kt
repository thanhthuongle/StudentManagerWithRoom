package com.example.studentmanagerwithroom

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.edu.hust.roomexamples.R

class MainActivity : AppCompatActivity() {
    private lateinit var studentDao: StudentDAO
    private val students = mutableListOf<StudentModel>()
    private val studentAdapter = StudentAdapter(students)
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private lateinit var launcher1: ActivityResultLauncher<Intent>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        studentDao = StudentDatabase.getInstance(this).studentDao()

        launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(),{
            if(it.resultCode == Activity.RESULT_OK) {
                val newName = it.data?.getStringExtra("newName").toString()
                val newId = it.data?.getStringExtra("newId").toString()
                try {
                    addStudent(newName, newId)
                    Toast.makeText(this, "Thêm sinh viên mới thành công", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.e("TAG", "Database Error: ${e.message}")
                    Toast.makeText(this, "Thêm sinh viên mới thất bại", Toast.LENGTH_LONG).show()
                }

            }
        })

        launcher1 = registerForActivityResult(ActivityResultContracts.StartActivityForResult(),{
            if(it.resultCode == Activity.RESULT_OK) {
                val newName = it.data?.getStringExtra("newName").toString()
                val newId = it.data?.getStringExtra("newId").toString()
                val position = it.data?.getIntExtra("position", -1)
                val _id = it.data?.getIntExtra("_id", -1)
                if(position!! > -1){
//                    students[position].studentName = newName
//                    students[position].studentId = newId
                    try {
                        updateStudent(_id!!, newName, newId)
                        Toast.makeText(this, "Thay đổi thông tin sinh viên thành công", Toast.LENGTH_LONG).show()
                    } catch (e: Exception){
                        Log.e("TAG", "Database Error: ${e.message}")
                        Toast.makeText(this, "Thay đổi thông tin sinh viên thất bại", Toast.LENGTH_LONG).show()
                    }
                } else{
                    Toast.makeText(this, "Thay đổi thông tin sinh viên thất bại", Toast.LENGTH_LONG).show()
                }

            }
        })

        val studentList = findViewById<ListView>(R.id.student_list)
        studentList.adapter = studentAdapter

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val total = studentDao.getStudentCount()
                Log.v("TAG", "tong sv: $total")
                if (total.toInt() == 0) createDataSeed()
                getData()
            } catch (e: Exception) {
                Log.e("TAG", "Database Error: ${e.message}")
            }
        }

        getData()

        registerForContextMenu(studentList)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.button_add_student -> {
                val intent = Intent(this, AddStudentActivity::class.java)
                launcher.launch(intent)
//                startActivity(intent)
//                Toast.makeText(this, "Share action", Toast.LENGTH_LONG).show()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        menuInflater.inflate(R.menu.context_menu, menu)
        super.onCreateContextMenu(menu, v, menuInfo)
    }

    @SuppressLint("ShowToast")
    override fun onContextItemSelected(item: MenuItem): Boolean {
        val pos = (item.menuInfo as AdapterContextMenuInfo).position
        when(item.itemId) {
            R.id.button_edit_student -> {
                val _id = students[pos].id
                val studentName = students[pos].studentName
                val studentId = students[pos].studentId
                val intent = Intent(this, AddStudentActivity::class.java)
                intent.putExtra("studentName", studentName)
                intent.putExtra("studentId", studentId)
                intent.putExtra("position", pos)
                intent.putExtra("_id", _id)
                launcher1.launch(intent)
            }

            R.id.button_delete_student -> {
                AlertDialog.Builder(this)
                    .setIcon(R.drawable.baseline_question_mark_24)
                    .setTitle("Xác nhận xóa sinh viên!")
                    .setMessage("Bạn chắc chắn muốn xóa sinh viên:\n${students[pos].studentName}-${students[pos].studentId}")
                    .setPositiveButton("Ok") { _, _ ->
                        val _id = students[pos].id
                        val studentName = students[pos].studentName
                        val studentId = students[pos].studentId
                        students.removeAt(pos)
                        studentAdapter.notifyDataSetChanged()
                        Snackbar.make(findViewById(R.id.main), "Đã xóa 1 học sinh",  Snackbar.LENGTH_LONG)
                            .setAction("Hoàn tác") {
                                students.add(pos, StudentModel(4, studentName, studentId))
                                studentAdapter.notifyDataSetChanged()
                            }
                            .addCallback(object : Snackbar.Callback() {
                                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                    super.onDismissed(transientBottomBar, event)
                                    // Khi Snackbar bị đóng, dù là vì người dùng nhấn vào "Hoàn tác" hay tự động đóng
                                    if (event == Snackbar.Callback.DISMISS_EVENT_TIMEOUT || event == Snackbar.Callback.DISMISS_EVENT_CONSECUTIVE) {
                                        // Thực hiện hành động khi Snackbar đóng tự động (do hết thời gian)
                                        // Hoặc khi người dùng kéo hoặc nhấn ngoài để đóng Snackbar
                                        // Log.d("Snackbar", "Snackbar đã đóng")
                                        try {
                                            deleteStudent(_id, studentName, studentId)
                                            Log.d("Snackbar", "ĐÃ xóa sv khỏi db")
                                        } catch (e: Exception){
                                            Log.e("TAG", "Database Error: ${e.message}")
                                            Log.d("Snackbar", "Xóa sv khỏi DB gặp lỗi!")
                                        }

                                    }
                                }
                            })
                            .show()
                    }
                    .setNegativeButton("Cancle", null)
                    .setCancelable(false)
                    .show()

            }
        }
        return super.onContextItemSelected(item)
    }

    private fun createDataSeed() {
        val data = mutableListOf(
            StudentModel(1,"Nguyễn Văn An", "SV001"),
            StudentModel(1,"Trần Thị Bảo", "SV002"),
            StudentModel(1,"Lê Hoàng Cường", "SV003"),
            StudentModel(1,"Phạm Thị Dung", "SV004"),
            StudentModel(1,"Đỗ Minh Đức", "SV005"),
            StudentModel(1,"Vũ Thị Hoa", "SV006"),
            StudentModel(1,"Hoàng Văn Hải", "SV007"),
            StudentModel(1,"Bùi Thị Hạnh", "SV008"),
            StudentModel(1,"Đinh Văn Hùng", "SV009"),
            StudentModel(1,"Nguyễn Thị Linh", "SV010"),
            StudentModel(1,"Phạm Văn Long", "SV011"),
            StudentModel(1,"Trần Thị Mai", "SV012"),
            StudentModel(1,"Lê Thị Ngọc", "SV013"),
            StudentModel(1,"Vũ Văn Nam", "SV014"),
            StudentModel(1,"Hoàng Thị Phương", "SV015"),
            StudentModel(1,"Đỗ Văn Quân", "SV016"),
            StudentModel(1,"Nguyễn Thị Thu", "SV017"),
            StudentModel(1,"Trần Văn Tài", "SV018"),
            StudentModel(1,"Phạm Thị Tuyết", "SV019"),
            StudentModel(1,"Lê Văn Vũ", "SV020")
        )
            for (student in data) {
                val result = studentDao.insertStudent(Student(studentName = student.studentName, studentId = student.studentId))
                Log.v("TAG", "Result: $result")
            }
    }

    private fun getData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val studentList = studentDao.getAllStudents()
            students.clear()
            for (student in studentList) {
                Log.v("TAG", student.toString())
                students.add(StudentModel(student._id, student.studentName, student.studentId) )
            }
            lifecycleScope.launch(Dispatchers.Main) { studentAdapter.notifyDataSetChanged() }
        }
    }

    private fun addStudent(studentName: String, studentId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            studentDao.insertStudent(Student(studentName = studentName, studentId = studentId))
            getData()
        }
    }

    private fun updateStudent(_id: Int, studentName: String, studentId: String) {
        val studentNew = Student(_id, studentName, studentId)
        lifecycleScope.launch(Dispatchers.IO) {
            val result = studentDao.updateStudent(studentNew)
            getData()
        }
    }

    private fun deleteStudent(_id: Int, studentName: String, studentId: String) {
        val studentDelete = Student(_id, studentName, studentId)
        lifecycleScope.launch(Dispatchers.IO) {
            studentDao.deleteStudent(studentDelete)
        }
    }
}