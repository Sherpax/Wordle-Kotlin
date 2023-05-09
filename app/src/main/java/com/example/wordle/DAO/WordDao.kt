package com.example.wordle.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.wordle.Entities.Word
import java.util.LinkedList

@Dao
interface WordDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(words: LinkedList<Word>)

    @Query("SELECT we.word_es FROM word we ORDER BY RANDOM() LIMIT 1")
    fun getRandomWord(): String?

    @Query("SELECT COUNT(*) FROM word we WHERE we.word_es LIKE :wordIntroduced")
    fun isValidWord(wordIntroduced: String?): Int

}