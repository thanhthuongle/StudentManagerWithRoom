package com.example.studentmanagerwithroom

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface StudentDAO {
    @Query("select * from students order by _id DESC")
    fun getAllStudents(): Array<Student>

    @Query("select * from students where studentId = :studentId")
    fun getStudentsByMssv(studentId: String): Array<Student>

    @Query("select * from students where studentName like '%' || :studentName || '%'")
    fun getStudentsByName(studentName: String): Array<Student>

    @Insert
    fun insertStudent(student: Student): Long

    @Update
    fun updateStudent(student: Student): Int

    @Delete
    fun deleteStudent(student: Student): Int

    @Query("delete from students where studentId = :studentId")
    fun deleteByMssv(studentId: String): Int

    @Query("SELECT COUNT(*) FROM students")
    fun getStudentCount(): Long

    // Xóa tất cả sinh viên
    @Query("DELETE FROM students")
    suspend fun deleteAllStudents()


}