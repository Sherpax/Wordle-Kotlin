package com.example.wordle

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import com.example.wordle.R.anim
import com.example.wordle.R.string
import com.example.wordle.dao.WordDao
import com.example.wordle.databinding.ActivityMainBinding
import com.example.wordle.entities.Word
import com.google.android.material.R.id
import com.google.android.material.behavior.SwipeDismissBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.LinkedList
import java.util.stream.IntStream.range


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var linerLayoutsArray: Array<LinearLayout>
    private val wordMap = hashMapOf<Char, Int>()
    private val FINAL_ROW: Int = 6
    private val WORDS_TXT: String = "words.txt"
    private var editTextList = mutableListOf<EditText>()
    private var currentRow: Int = 0
    private var wordToGuess: String = "VIVAZ"
    private var currPos = 0
    private val arrTextViews = arrayListOf<TextView>()
    private var db: AppDatabase? = null
    private var dao: WordDao? = null
    private lateinit var currentEditText: EditText
    private var thread: Thread? = null

    private fun readWordsFromFile(context: Context): LinkedList<Word> {
        val words = LinkedList<Word>()
        val assetManager = context.assets
        val inputStream = assetManager.open(WORDS_TXT)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))

        bufferedReader.useLines { lines ->
            lines.forEach {
                if (it.length == 5)
                    words.add(Word(id = 0, words = it.trim()))
            }
        }
        return words
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)
        dao = db?.wordDao()

        val listWords = readWordsFromFile(this)

        dao?.insertAll(listWords)

        wordToGuess = dao?.getRandomWord().toString()
        println(wordToGuess)

        supportActionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#1F1F1F")))
        supportActionBar?.setDisplayShowTitleEnabled(false)  // required to force redraw, without, gray color
        supportActionBar?.setDisplayShowTitleEnabled(true)

        loadEditTexts()

        currentEditText = editTextList[0]
        currentEditText.requestFocus()

        linerLayoutsArray = arrayOf(
            binding.firstLinear, binding.secondLinear,
            binding.thirdLayout
        )

        txtViewHandler()

        wordToGuess.forEach { char ->
            wordMap[char] = wordMap.getOrDefault(char, 0) + 1
        }

        binding.checkButton.setOnClickListener {
            if (currentRow < FINAL_ROW) {
                checkWord()
            }
        }

        binding.resetButton.setOnClickListener {
            if (it.isVisible) {
                resetGame()
            }
        }

        binding.btnErase.setOnClickListener {
            keyBoardDelBtnHandler(it)
            binding.btnErase.startAnimation(rotateAnimation)
        }

        // This callback will only be called when MyFragment is at least Started.
        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    // TODO: Go back to home activity
                }
            }
        this.onBackPressedDispatcher.addCallback(this, callback)

    }

    private fun resetGame() {
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun txtViewHandler() {
        for (e in linerLayoutsArray) {
            for (ie in e.children) {
                if (ie.id == binding.btnErase.id) continue
                val txtView = ie as TextView
                txtView.tag = txtView.text
                txtView.isClickable = true
                txtView.setOnClickListener { event ->
                    keyboardHandler(event)
                }
                println(ie.text)
                if (ie !is MaterialButton) {
                    arrTextViews.add(txtView)
                }
            }
        }
        arrTextViews.sortBy { it.text.toString() }
    }

    private fun keyboardHandler(event: View?) {
        if (thread?.isAlive == true) return
        if (!binding.btnErase.isClickable) return
        if(!currentEditText.hasFocus()) return
        val txt = event as TextView
        val maxFocus = currentRow * 5 + 5
        if (txt.id != binding.btnErase.id && currPos < maxFocus) {
            currentEditText.apply {
                setText(txt.text)
            }
        }
        val animation = AnimationUtils.loadAnimation(this, anim.view_pressed)
        event.startAnimation(animation)
    }

    private fun keyBoardDelBtnHandler(event: View?) {
        if (!binding.btnErase.isClickable) return
        val txt = event as TextView
        val maxFocus = currentRow * 5 + 5
        if (currentEditText.id == currentRow * 5) currentEditText.setText(txt.text)
        if (txt.id == binding.btnErase.id && currPos > maxFocus - 5) {
            if (editTextList[currPos].text.isNotEmpty()) {
                editTextList[currPos].apply {
                    editTextList[currPos].requestFocus()
                    setText(txt.text)
                }
            } else {
                editTextList[--currPos].apply {
                    editTextList[currPos].requestFocus()
                    setText(txt.text)
                }
            }
        }
    }

    private fun wordIntroduced(ini: Int, end: Int): String {
        val arrWords: MutableList<EditText> = editTextList.subList(ini, end)
        val word = StringBuilder()
        var cont = 0
        while (cont < arrWords.size && arrWords[cont].text.isNotEmpty()) {
            word.append(arrWords[cont].text)
            cont++
        }
        return word.toString()
    }

    private fun closeKeyBoard() {
        // Close KeyBoard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)

    }

    private fun checkWord() {

        closeKeyBoard()

        val ini = currentRow * 5
        val maxCol = ini + 5

        val wordIntroduced = wordIntroduced(ini, maxCol)

        when {
            wordIntroduced.length != 5 -> showErrorWordAlert(
                getString(string.word_must_contain_5_letters)
            )

            !isValidWord(wordIntroduced) -> showErrorWordAlert(
                getString(string.the_word_introduced_is_not_valid)
            )

            else -> validateWord(ini, maxCol)
        }
    }

    private fun validateWord(ini: Int, maxCol: Int) {
        val shallowWordMap = wordMap.toMutableMap()
        val introducedWord: StringBuilder = StringBuilder()
        var isWinner = true
        //Button check disabled to avoid spam
        binding.checkButton.isEnabled = false
        binding.btnErase.isClickable = false

        thread = Thread {
            for (i in ini until maxCol) {
                val contIntWord = i - ini
                val char = editTextList[i].text.toString().first()
                introducedWord.append(char)
                val pair = getIsWinnerAndColor(contIntWord, char, shallowWordMap, isWinner)
                val color = pair.first
                isWinner = pair.second

                runOnUiThread {
                    updateUIRows(i, color)
                    if (i == maxCol - 1) {
                        if (currentRow != FINAL_ROW) {
                            ++currentRow
                            if (currentRow * 5 < FINAL_ROW * 5) {
                                currentEditText = editTextList[currentRow * 5]
                            } else {
                                currentEditText.clearFocus()
                            }
                            makeNextRowEditable()
                            currentEditText.requestFocus()
                            updateUIKeyColors(introducedWord.toString())
                            binding.checkButton.isEnabled = true
                            binding.btnErase.isClickable = true
                        }
                        checkEndGame(isWinner)
                    }
                }
                Thread.sleep(390)
            }
        }
        thread?.start()
    }

    private fun updateUIKeyColors(
        introducedWord: String
    ) {
        val shallowWordMap = wordMap.toMutableMap()
        var contPosWord = 0
        for (char in introducedWord) {
            var pos = 0
            pos = if (char == 'Ñ') {
                26 // last element in the list
            } else {
                char.code - 65
            }
            val color = when {
                wordToGuess[contPosWord++] == char && shallowWordMap[char] != 0 -> {
                    shallowWordMap[char]?.minus(1)?.let {
                        shallowWordMap[char] = it
                    }
                    "GREEN"
                }

                shallowWordMap.containsKey(char) && shallowWordMap[char] != 0 -> {
                    shallowWordMap[char]?.minus(1)?.let {
                        shallowWordMap[char] = it

                    }
                    "YELLOW"
                }

                else -> {
                    "BLACK"
                }
            }

            if (arrTextViews[pos].tag == "GREEN" || arrTextViews[pos].tag == "BLACK") continue
            var buttonDrawable: Drawable = arrTextViews[pos].background
            buttonDrawable = DrawableCompat.wrap(buttonDrawable)
            val isGreen = color == "GREEN"
            val isYellow = color == "YELLOW"
            if (isGreen && arrTextViews[pos].tag != "GREEN") {
                DrawableCompat.setTint(buttonDrawable, COLORS.GREEN.getRGB())
                arrTextViews[pos].tag = color
            } else if (isYellow) {
                DrawableCompat.setTint(buttonDrawable, COLORS.YELLOW.getRGB())
                arrTextViews[pos].tag = color
            } else {
                if (arrTextViews[pos].tag != "YELLOW") {
                    DrawableCompat.setTint(buttonDrawable, COLORS.BLACK.getRGB())
                }
            }
            arrTextViews[pos].setTextColor(COLORS.WHITE.getRGB())
        }

    }

    private fun getIsWinnerAndColor(
        contIntWord: Int,
        char: Char,
        shallowWordMap: MutableMap<Char, Int>,
        isWinner: Boolean
    ): Pair<Int, Boolean> {
        var isWinner1 = isWinner
        val color = when {
            wordToGuess[contIntWord] == char && shallowWordMap[char] != 0 -> {
                shallowWordMap[char]?.minus(1)?.let { shallowWordMap.put(char, it) }
                COLORS.GREEN.getRGB()
            }

            shallowWordMap.containsKey(char) && shallowWordMap[char] != 0 -> {
                shallowWordMap[char]?.minus(1)?.let { shallowWordMap.put(char, it) }
                isWinner1 = false
                COLORS.YELLOW.getRGB()
            }

            else -> {
                isWinner1 = false
                COLORS.BLACK.getRGB()
            }
        }
        return Pair(color, isWinner1)
    }

    private fun updateUIRows(i: Int, color: Int) {
        editTextList[i].isFocusable = false
        editTextList[i].setBackgroundColor(color)
        flipperAnimation(editTextList[i])
//        editTextList[i].startAnimation(rotateAnimation)
    }


    private fun showErrorWordAlert(s: String) {
        val snackbar = Snackbar.make(binding.coordLayout, s, LENGTH_SHORT)
        val view = snackbar.view
        view.findViewById<TextView>(id.snackbar_text)
            .apply {
                gravity = Gravity.CENTER_HORIZONTAL
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                textSize = 18f
            }
        binding.coordLayout.bringToFront()
        val behavior = BaseTransientBottomBar.Behavior().apply {
            setSwipeDirection(SwipeDismissBehavior.SWIPE_DIRECTION_ANY)
        }
        snackbar.setBehavior(behavior)
            .show()
    }

    private fun loadEditTexts() {

        // Add every EditText to list & TextWatcher for every item
        var contTag = 0
        for (i in range(0, binding.table.childCount)) {
            val row = binding.table.getChildAt(i) as TableRow
            for (j in range(0, 5)) {
                val item = row.getChildAt(j) as EditText
                editTextHandler(item)
                item.id = contTag
                item.tag = contTag++
                item.setOnFocusChangeListener { view, hasFocus ->
                    if (hasFocus) {
                        currentEditText = view as EditText
                        currentEditText.requestFocus()
                        currPos = currentEditText.id
                    }
                }
                item.inputType = InputType.TYPE_NULL
                item.isFocusableInTouchMode = true
                item.isFocusable = true
                item.isCursorVisible = false
                item.setTextColor(COLORS.WHITE.getRGB())
                item.setBackgroundResource(R.drawable.custom_editext)
                editTextList.add(item)
            }
        }
    }

    private fun editTextHandler(item: EditText) {
        item.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                item.removeTextChangedListener(this)
                val regex = Regex(pattern = "[a-zA-ZñÑ|\b]+")
                // Get current cursor position
                if (regex.matches(p0.toString())) {
                    item.setText(p0.toString().uppercase())
//                    item.setSelection(1) // Cursor position next to letter
                    val nextTagNumber = item.tag.toString().toInt() + 1
                    if (nextTagNumber < currentRow * 5 + 5) {
                        editTextList[nextTagNumber].requestFocus()
                    } else {
                        editTextList[nextTagNumber - 1].isCursorVisible = false
                        editTextList[nextTagNumber - 1].clearFocus()
                        closeKeyBoard()
                        editTextList[nextTagNumber - 1].isCursorVisible = true
                    }
                } else {
                    item.setText("")
                }
                item.addTextChangedListener(this)
            }
        })
    }

    private fun makeNextRowEditable() {
        val ini = currentRow * 5
        val max = ini + 5
        editTextList.takeIf { max <= it.size }?.subList(ini, max)?.forEach { e ->
            e.isEnabled = true
            e.isFocusable = true
        }
    }

    private fun isValidWord(str: String): Boolean {
        return dao?.isValidWord(str) == 1
    }

    private fun checkEndGame(isWinner: Boolean) {
        if (isWinner) {
            binding.checkButton.isEnabled = false
            showEndGameDialog(getString(string.win_message))
        } else if (currentRow == FINAL_ROW) {
            binding.checkButton.isEnabled = false
            showEndGameDialog(getString(string.lose_message_template, wordToGuess))
        }
    }

    private fun showEndGameDialog(messageResId: String) {
        AlertDialog.Builder(this)
            .setMessage(messageResId)
            .setPositiveButton(string.reset_game) { _, _ ->
                resetGame()
            }.create().show()
        // Show reset button
        binding.resetButton.visibility = View.VISIBLE
    }


    override fun onDestroy() {
        super.onDestroy()
        db?.close()
    }
}

