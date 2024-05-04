package com.example.myapplication

class HashTable<K, V> where V : Vector {
    private var initCapacity = 5
    private var numBuckets = initCapacity
    private var buckets: Array<MutableList<HashNode<K, V>>> = Array(numBuckets) { arrayListOf() }
    var onRehash: () -> Unit = {}

    fun getBucketIndex(key: K): Int = key.hashCode() % numBuckets

    fun get(key: K): V? {
        val index = getBucketIndex(key)
        return buckets[index].find { it.key == key }?.value
    }

    fun put(key: K, value: V) {
        val index = getBucketIndex(key)
        val bucket = buckets[index]
        val node = bucket.find { it.key == key }
        if (node != null) {
            node.value = value
        } else {
            bucket.add(HashNode(key, value))
            if ((size.toDouble() / numBuckets) >= 0.7) {
                rehash()
            }
        }
    }

    fun remove(key: K): V? {
        val index = getBucketIndex(key)
        val bucket = buckets[index]
        val nodeIndex = bucket.indexOfFirst { it.key == key }
        return if (nodeIndex != -1) bucket.removeAt(nodeIndex).value else null
    }

    //fun clear() {
    //    buckets.forEach { it.clear() }
    //}
    fun clear() {
        println("Очищаем таблицу")
        buckets = Array(numBuckets) { arrayListOf() }  // Пересоздаем массив корзин
    }
    private fun rehash() {
        val oldBuckets = buckets
        numBuckets *= 2
        buckets = Array(numBuckets) { arrayListOf() }
        oldBuckets.flatMap { it }.forEach { node -> put(node.key, node.value) }
    }

    val size: Int
        get() = buckets.sumOf { it.size }

    //fun getBucketData(): List<HashNode<K, V>> {
    //    return buckets.flatMap { it }
    //}

    // В классе HashTable
    fun getBucketData(): List<Pair<Int, List<HashNode<K, V>>>> {
        return buckets.mapIndexed { index, bucket ->
            index to bucket.toList() // Преобразуем MutableList в List
        }.filter { it.second.isNotEmpty() } // Опционально: фильтруем пустые корзины
    }



}
