package com.example.studentmanagerwithroom

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val _id: Int = 0,
    var studentName: String,
    var studentId: String
)
