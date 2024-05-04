import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import com.example.myapplication.HashTable
import com.example.myapplication.Vector

class VectorViewModel : ViewModel() {
    val keyInput = mutableStateOf("")
    val searchResult = mutableStateOf<Vector?>(null)
    val isSearching = mutableStateOf(false)
    val lastSuccessfulKey = mutableStateOf<String?>(null)  // Для сохранения ключа последнего успешного поиска
    var hashTable = mutableStateOf(HashTable<Int, Vector>())
        private set
    val dataLoaded = mutableStateOf(false)

    fun clearSearch() {
        isSearching.value = false
        searchResult.value = null
    }

    fun performSearch(hashTable: HashTable<Int, Vector>) {
        val keyInt = keyInput.value.toIntOrNull()
        if (keyInt != null) {
            searchResult.value = hashTable.get(keyInt)
            if (searchResult.value != null) {
                lastSuccessfulKey.value = keyInput.value  // Сохраняем ключ только если поиск успешен
            }
        } else {
            searchResult.value = null
        }
        isSearching.value = true
    }
}