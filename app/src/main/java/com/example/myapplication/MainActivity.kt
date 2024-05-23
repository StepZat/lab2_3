package com.example.myapplication

import VectorViewModel
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.MainActivity.Companion.REQUEST_CODE_IMPORT_CSV
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var viewModelVector: VectorViewModel
    private lateinit var filePickerLauncher: ActivityResultLauncher<String>
    companion object {
        const val REQUEST_CODE_IMPORT_CSV = 1001  // Уникальный код запроса
        private const val PERMISSION_REQUEST_CODE = 101
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModelVector = ViewModelProvider(this).get(VectorViewModel::class.java)
        setupFilePickerLauncher()
        setContent {
            MaterialTheme {
                Surface {
                    VectorFactory.registerType("Polar", PolarVectorType())
                    VectorFactory.registerType("Cartesian", CartesianVectorType())
                    VectorManagementApp(viewModelVector.hashTable, VectorFactory, viewModelVector, ::launchFilePicker)
                }
            }
        }
    }

    override fun onBackPressed() {
        if (viewModelVector.isSearching.value) {
            viewModelVector.clearSearch()
        } else {
            super.onBackPressed()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMPORT_CSV && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                importFromCSV(uri, applicationContext, viewModelVector.hashTable.value)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun importFromCSV(uri: Uri, context: Context, hashTable: HashTable<Int, Vector>) {
        println("Starting CSV import")
        val inputStream = context.contentResolver.openInputStream(uri)
        inputStream?.bufferedReader().use { reader ->
            reader?.lineSequence()?.drop(1)?.forEach { line ->
                val (key, valueRepresentation) = line.split(";")
                val vector = parseVector(valueRepresentation)
                hashTable.put(key.toInt(), vector)
                println("Added vector: $vector with key $key")
            }
        }

        println("Import completed, hashTable size: ${hashTable.size}")
        viewModelVector.dataLoaded.value = true
    }

    fun parseVector(valueRepresentation: String): Vector {
        return if (valueRepresentation.startsWith("Polar")) {
            val params = valueRepresentation.removePrefix("Polar: Length=").removeSuffix(")").split(", Angle=")
            PolarVector(params[0].toDouble(), params[1].toDouble())
        } else {
            val params = valueRepresentation.removePrefix("Cartesian: X=").removeSuffix(")").split(", Y=")
            CartesianVector(params[0].toDouble(), params[1].toDouble())
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchFilePicker()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setupFilePickerLauncher() {
        filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                importFromCSV(it, applicationContext, viewModelVector.hashTable.value)
            }
        }
    }

    private fun launchFilePicker() {
        println("yes")
        filePickerLauncher.launch("*/*")
    }




}



@Composable
fun CustomTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = Color.Black,
            backgroundColor = Color.White,
            focusedBorderColor = Color.Blue,
            unfocusedBorderColor = Color.Gray
        ),
        shape = RoundedCornerShape(8.dp), // Закругление углов
        modifier = modifier
            .padding(4.dp)  // Уменьшим отступ для лучшего использования пространства
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun VectorManagementApp(
    //hashTable: HashTable<Int, Vector>,
    hashTable: MutableState<HashTable<Int, Vector>>,
    vectorFactory: VectorFactory,
    viewModel: VectorViewModel,
    launchFilePicker: () -> Unit,
    context: Context = LocalContext.current) {
    var selectedVectorType by remember { mutableStateOf("Polar") }
    val vectorTypes = listOf("Polar", "Cartesian")
    var keyInput by remember { mutableStateOf("") }
    var valuePartOneInput by remember { mutableStateOf("") }
    var valuePartTwoInput by remember { mutableStateOf("") }
    var keyInputError by remember { mutableStateOf("") }
    var valuePartOneError by remember { mutableStateOf("") }
    var valuePartTwoError by remember { mutableStateOf("") }
    var refreshUI by remember { mutableStateOf(false) }
    val dataLoaded = viewModel.dataLoaded.value


    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xff88a5af))
        .padding(16.dp)) {


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DropdownMenuComponent(items = vectorTypes, selectedItem = selectedVectorType) {
                selectedVectorType = it
                valuePartOneInput = ""
                valuePartTwoInput = ""
            }
            Button(onClick = { exportDataToCSV(viewModel.hashTable.value, context) }) {
                Text("Export")
            }
            if (dataLoaded) {
                TableDisplay(viewModel.hashTable.value, refreshUI)
                viewModel.dataLoaded.value = false  // Сброс состояния после отображения
            }
            Button(onClick = {
                launchFilePicker()
                refreshUI = !refreshUI
            }
            ) {
                Text("Import")
            }
        }


        CustomTextField(value = viewModel.keyInput.value, onValueChange = { viewModel.keyInput.value = it }, label = "Key")
        if (keyInputError.isNotEmpty()) {
            Text(keyInputError, color = Color.Red, style = MaterialTheme.typography.body2)
        }
        CustomTextField(value = valuePartOneInput, onValueChange = { valuePartOneInput = it }, label = if (selectedVectorType == "Polar") "Length" else "X")
        if (valuePartOneError.isNotEmpty()) {
            Text(valuePartOneError, color = Color.Red, style = MaterialTheme.typography.body2)
        }
        CustomTextField(value = valuePartTwoInput, onValueChange = { valuePartTwoInput = it }, label = if (selectedVectorType == "Polar") "Angle" else "Y")
        if (valuePartTwoError.isNotEmpty()) {
            Text(valuePartTwoError, color = Color.Red, style = MaterialTheme.typography.body2)
        }

        Row {
            Button(onClick = {
                val keyInt = viewModel.keyInput.value.toIntOrNull()
                val valuePartOneDouble = valuePartOneInput.toDoubleOrNull()
                val valuePartTwoDouble = valuePartTwoInput.toDoubleOrNull()
                keyInputError = ""
                valuePartOneError = ""
                valuePartTwoError = ""

                if (keyInt == null) {
                    keyInputError = "Key must be a number"
                }
                if (valuePartOneDouble == null) {
                    valuePartOneError = "Value must be a number"
                }
                if (valuePartTwoDouble == null) {
                    valuePartTwoError = "Value must be a number"
                }
                if (keyInputError.isEmpty() && valuePartOneError.isEmpty() && valuePartTwoError.isEmpty()) {
                    val vector = vectorFactory.createVector(selectedVectorType)
                    when (vector) {
                        is PolarVector -> viewModel.hashTable.value.put(keyInt!!, PolarVector(valuePartOneDouble!!, valuePartTwoDouble!!))
                        is CartesianVector -> viewModel.hashTable.value.put(keyInt!!, CartesianVector(valuePartOneDouble!!, valuePartTwoDouble!!))
                    }
                    print(vector.keyRepresentation)
                    viewModel.keyInput.value = ""
                    valuePartOneInput = ""
                    valuePartTwoInput = ""
                }
            }) {
                Text("Add")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = {
                val keyInt = viewModel.keyInput.value.toIntOrNull()
                keyInputError = ""
                if (keyInt == null) {
                    keyInputError = "Key must be a number"
                }
                if (keyInt != null) {
                    viewModel.hashTable.value.remove(keyInt)
                    refreshUI = !refreshUI
                    viewModel.keyInput.value = ""
                }
            }) {
                Text("Delete")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = {
                viewModel.hashTable.value.clear()
                refreshUI = !refreshUI  // Обновляем UI после очистки таблицы
            }) {
                Text("Clear")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = {
                viewModel.performSearch(viewModel.hashTable.value)
            }) {
                Text("Find")
            }
        }

        if (viewModel.isSearching.value) {
            if (viewModel.searchResult.value != null) {
                Text("Key: ${viewModel.lastSuccessfulKey.value}, Value: ${viewModel.searchResult.value!!.keyRepresentation}", style = MaterialTheme.typography.h6)
            } else {
                Text("Element not found", style = MaterialTheme.typography.h6)
            }
        } else {
            TableDisplay(viewModel.hashTable.value, refreshUI)
        }
    }
}

@Composable
fun TableDisplay(hashTable: HashTable<Int, Vector>, refreshUI: Boolean) {
    LazyColumn(modifier = Modifier.padding(8.dp)) {
        items(items = hashTable.getBucketData()) { bucketData ->
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)) {
                Text(
                    text = "Bucket ${bucketData.first}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .background(Color.LightGray)
                        .padding(4.dp),
                    style = MaterialTheme.typography.subtitle1
                )
                bucketData.second.forEach { vector ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                            .padding(4.dp)
                    ) {
                        Text(
                            text = "Key: ${vector.key}",
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            style = MaterialTheme.typography.body1
                        )
                        Text(
                            text = "Value: ${vector.value.keyRepresentation}",
                            modifier = Modifier
                                .weight(2f)
                                .padding(end = 8.dp),
                            style = MaterialTheme.typography.body1
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun DropdownMenuComponent(items: List<String>, selectedItem: String, onItemSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }) {
            Text(selectedItem)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { label ->
                DropdownMenuItem(onClick = { expanded = false; onItemSelected(label) }) {
                    Text(label)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun exportDataToCSV(hashTable: HashTable<Int, Vector>, context: Context) {
    val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    val currentTime = dateFormat.format(Date()) // Текущее время

    val fileName = "HashTableData_$currentTime.csv"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName) // Название файла
        put(MediaStore.MediaColumns.MIME_TYPE, "text/csv") // Тип MIME
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MyApp") // Путь сохранения
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

    uri?.let {
        resolver.openOutputStream(it)?.use { outputStream ->
            OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                writer.write("Key;Value\n")
                hashTable.getBucketData().forEach { bucket ->
                    bucket.second.forEach { node ->
                        writer.write("${node.key};${node.value.keyRepresentation}\n")
                    }
                }
            }
            Toast.makeText(context, "Data exported to Downloads/MyApp/$fileName", Toast.LENGTH_LONG).show()
        }
    } ?: run {
        Toast.makeText(context, "Failed to create file", Toast.LENGTH_LONG).show()
    }
}












