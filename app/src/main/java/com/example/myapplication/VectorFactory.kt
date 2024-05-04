package com.example.myapplication

object VectorFactory {
    private var types: MutableMap<String, VectorType> = mutableMapOf(
        "Polar Vector" to PolarVectorType(),
        "Cartesian Vector" to CartesianVectorType()
    )

    fun registerType(name: String, vectorType: VectorType) {
        types[name] = vectorType
    }

    fun createVector(typeName: String): Vector {
        return types[typeName]?.create() ?: throw IllegalArgumentException("No such vector type registered")
    }
}
