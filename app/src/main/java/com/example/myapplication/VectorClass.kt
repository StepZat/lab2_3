package com.example.myapplication

data class PolarVector(val length: Double, val angle: Double) : Vector {
    override val keyRepresentation: String get() = "Polar: Length=$length, Angle=$angle"
    val valueRepresentation: String get() = "($length, $angle)"
}

data class CartesianVector(val x: Double, val y: Double) : Vector {
    override val keyRepresentation: String get() = "Cartesian: X=$x, Y=$y"
    val valueRepresentation: String get() = "($x, $y)"
}

class PolarVectorType : VectorType {
    override fun create(): Vector = PolarVector(0.0, 0.0)
    override fun clone(obj: Vector): Vector = (obj as PolarVector).copy()
    override fun parse(input: String): PolarVector {
        val parts = input.split(",")
        return PolarVector(parts[0].toDouble(), parts[1].toDouble())
    }
    override val typeName: String = "Polar Vector"
}

class CartesianVectorType : VectorType {
    override fun create(): Vector = CartesianVector(0.0, 0.0)
    override fun clone(obj: Vector): Vector = (obj as CartesianVector).copy()
    override fun parse(input: String): CartesianVector {
        val parts = input.split(",")
        return CartesianVector(parts[0].toDouble(), parts[1].toDouble())
    }
    override val typeName: String = "Cartesian Vector"
}
