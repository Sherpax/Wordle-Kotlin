package com.example.wordle.Entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "word", indices = [Index(value = ["word_es"], unique = true)])
data class Word (

    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "word_es") val words: String

)