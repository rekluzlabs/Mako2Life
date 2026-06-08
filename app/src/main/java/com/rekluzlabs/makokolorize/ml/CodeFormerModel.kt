package com.rekluzlabs.makokolorize.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession

class CodeFormerModel(modelPath: String) : AutoCloseable {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val inputName: String

    init {
        val opts = OrtSession.SessionOptions().apply {
            // NNAPI disabled: was producing grey/red noise artifacts on device
        }
        session = env.createSession(modelPath, opts)
        inputName = session.inputNames?.firstOrNull() ?: "input"
    }

    fun run(inputTensor: OnnxTensor, fidelityWeight: Float): Pair<FloatArray, LongArray> {
        // The model expects a double scalar for the weight input.
        val weightTensor = OnnxTensor.createTensor(env, doubleArrayOf(fidelityWeight.toDouble()))
        return try {
            val inputs = mapOf(inputName to inputTensor, "weight" to weightTensor)
            session.run(inputs).use { result ->
                val outputTensor = result[0] as OnnxTensor
                val shape = outputTensor.info.shape
                val buf = outputTensor.floatBuffer
                val data = FloatArray(buf.remaining())
                buf.get(data)
                Pair(data, shape)
            }
        } finally {
            weightTensor.close()
        }
    }

    override fun close() {
        session.close()
    }
}
