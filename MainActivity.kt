package com.teju.expensetracker

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val allExpenses = mutableListOf<Expense>()
    private val displayExpenses = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private val PREFS_NAME = "ExpensePrefs"
    private val KEY_EXPENSES = "expenses_v2"

    private var currentMode = "daily" // daily, monthly, yearly
    private var currentCalendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etName = findViewById<EditText>(R.id.etExpenseName)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val btnAdd = findViewById<Button>(R.id.btnAdd)
        val btnClear = findViewById<Button>(R.id.btnClear)
        val tvTotal = findViewById<TextView>(R.id.tvTotal)
        val tvPeriod = findViewById<TextView>(R.id.tvPeriod)
        val lvExpenses = findViewById<ListView>(R.id.lvExpenses)
        val spinner = findViewById<Spinner>(R.id.spinnerCategory)
        val btnDaily = findViewById<Button>(R.id.btnDaily)
        val btnMonthly = findViewById<Button>(R.id.btnMonthly)
        val btnYearly = findViewById<Button>(R.id.btnYearly)
        val btnPrev = findViewById<Button>(R.id.btnPrev)
        val btnNext = findViewById<Button>(R.id.btnNext)

        // Categories
        val categories = listOf("🍔 Food", "🚗 Transport", "🛍️ Shopping", "💊 Health", "📚 Education", "🎮 Entertainment", "💡 Bills", "📦 Other")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinner.adapter = spinnerAdapter

        // Load saved data
        loadExpenses()

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayExpenses)
        lvExpenses.adapter = adapter
        updateView(tvTotal, tvPeriod)

        // Tab buttons
        btnDaily.setOnClickListener {
            currentMode = "daily"
            currentCalendar = Calendar.getInstance()
            updateView(tvTotal, tvPeriod)
        }
        btnMonthly.setOnClickListener {
            currentMode = "monthly"
            currentCalendar = Calendar.getInstance()
            updateView(tvTotal, tvPeriod)
        }
        btnYearly.setOnClickListener {
            currentMode = "yearly"
            currentCalendar = Calendar.getInstance()
            updateView(tvTotal, tvPeriod)
        }

        // Prev / Next navigation
        btnPrev.setOnClickListener {
            when (currentMode) {
                "daily" -> currentCalendar.add(Calendar.DAY_OF_YEAR, -1)
                "monthly" -> currentCalendar.add(Calendar.MONTH, -1)
                "yearly" -> currentCalendar.add(Calendar.YEAR, -1)
            }
            updateView(tvTotal, tvPeriod)
        }
        btnNext.setOnClickListener {
            when (currentMode) {
                "daily" -> currentCalendar.add(Calendar.DAY_OF_YEAR, 1)
                "monthly" -> currentCalendar.add(Calendar.MONTH, 1)
                "yearly" -> currentCalendar.add(Calendar.YEAR, 1)
            }
            updateView(tvTotal, tvPeriod)
        }

        // Add expense
        btnAdd.setOnClickListener {
            val name = etName.text.toString().trim()
            val amountText = etAmount.text.toString().trim()
            val category = spinner.selectedItem.toString()

            if (name.isEmpty() || amountText.isEmpty()) {
                Toast.makeText(this, "Please fill in both fields!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDouble()
            val now = Calendar.getInstance()
            val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(now.time)
            val day = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(now.time)
            val month = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(now.time)
            val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(now.time)

            val expense = Expense(
                name = name,
                amount = amount,
                category = category,
                date = date,
                day = day,
                month = month,
                year = year
            )

            allExpenses.add(0, expense)
            saveExpenses()
            updateView(tvTotal, tvPeriod)

            etName.text.clear()
            etAmount.text.clear()
        }

        // Delete on long press
        lvExpenses.setOnItemLongClickListener { _, _, position, _ ->
            AlertDialog.Builder(this)
                .setTitle("Delete Expense")
                .setMessage("Delete this expense?")
                .setPositiveButton("Delete") { _, _ ->
                    val displayedItem = displayExpenses[position]
                    val toRemove = allExpenses.find {
                        "${it.category}  |  ${it.name}\n₹${it.amount}  •  ${it.date}" == displayedItem
                    }
                    if (toRemove != null) {
                        allExpenses.remove(toRemove)
                        saveExpenses()
                        updateView(tvTotal, tvPeriod)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }

        // Clear current view
        btnClear.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear Current View")
                .setMessage("Delete all expenses in this view?")
                .setPositiveButton("Clear") { _, _ ->
                    val toDelete = getFilteredExpenses()
                    allExpenses.removeAll(toDelete.toSet())
                    saveExpenses()
                    updateView(tvTotal, tvPeriod)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun getFilteredExpenses(): List<Expense> {
        val dayFmt = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val monthFmt = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        val yearFmt = SimpleDateFormat("yyyy", Locale.getDefault())

        return when (currentMode) {
            "daily" -> {
                val target = dayFmt.format(currentCalendar.time)
                allExpenses.filter { it.day == target }
            }
            "monthly" -> {
                val target = monthFmt.format(currentCalendar.time)
                allExpenses.filter { it.month == target }
            }
            "yearly" -> {
                val target = yearFmt.format(currentCalendar.time)
                allExpenses.filter { it.year == target }
            }
            else -> allExpenses
        }
    }

    private fun updateView(tvTotal: TextView, tvPeriod: TextView) {
        val filtered = getFilteredExpenses()
        val total = filtered.sumOf { it.amount }

        displayExpenses.clear()
        for (e in filtered) {
            displayExpenses.add("${e.category}  |  ${e.name}\n₹${e.amount}  •  ${e.date}")
        }
        adapter.notifyDataSetChanged()
        tvTotal.text = "Total: ₹$total"

        val dayFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val yearFmt = SimpleDateFormat("yyyy", Locale.getDefault())

        tvPeriod.text = when (currentMode) {
            "daily" -> {
                val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
                val target = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(currentCalendar.time)
                if (today == target) "Today" else dayFmt.format(currentCalendar.time)
            }
            "monthly" -> monthFmt.format(currentCalendar.time)
            "yearly" -> yearFmt.format(currentCalendar.time)
            else -> ""
        }
    }

    private fun saveExpenses() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (e in allExpenses) {
            val obj = org.json.JSONObject()
            obj.put("id", e.id)
            obj.put("name", e.name)
            obj.put("amount", e.amount)
            obj.put("category", e.category)
            obj.put("date", e.date)
            obj.put("day", e.day)
            obj.put("month", e.month)
            obj.put("year", e.year)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_EXPENSES, jsonArray.toString()).apply()
    }

    private fun loadExpenses() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_EXPENSES, "[]")
        val jsonArray = JSONArray(jsonString)
        allExpenses.clear()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            allExpenses.add(
                Expense(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    amount = obj.getDouble("amount"),
                    category = obj.getString("category"),
                    date = obj.getString("date"),
                    day = obj.getString("day"),
                    month = obj.getString("month"),
                    year = obj.getString("year")
                )
            )
        }
    }
}