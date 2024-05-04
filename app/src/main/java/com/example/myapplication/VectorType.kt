package com.example.myapplication

interface VectorType {
    fun create(): Vector
    fun clone(vector: Vector): Vector
    fun parse(s: String): Vector
    val typeName: String
}
