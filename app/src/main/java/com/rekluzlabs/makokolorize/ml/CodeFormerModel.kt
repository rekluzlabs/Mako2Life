package com.rekluzlabs.makokolorize.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer

class CodeFormerModel(modelPath: String) : AutoCloseable {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        val opts = OrtSession.SessionOptions().apply {
            // Disabled NNAPI because it was failing and causing corrupted output (grey/red noise)
            // setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        }
        session = env.createSession(modelPath, opts)
    }

    fun run(inputTensor: OnnxTensor, fidelityWeight: Float): Pair<FloatArray, LongArray> {
        val weightTensor = OnnxTensor.createTensor(env, doubleArrayOf(fidelityWeight.toDouble()))
        val inputs = mapOf("input" to inputTensor, "weight" to weightTensor)
        
        return session.run(inputs).use { result ->
            val outputTensor = result[0] as OnnxTensor
            val shape = outputTensor.info.shape
            val buf = outputTensor.floatBuffer
            val data = FloatArray(buf.remaining())
            buf.get(data)
            weightTensor.close()
            Pair(data, shape)
        }
    }

    override fun close() {
        session.close()
    }
}
