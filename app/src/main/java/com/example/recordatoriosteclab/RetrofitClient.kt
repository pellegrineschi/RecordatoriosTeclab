
package com.example.recordatoriosteclab

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val clienteHttp = OkHttpClient.Builder()

        .connectTimeout(5, TimeUnit.SECONDS)

        .readTimeout(8, TimeUnit.SECONDS)

        .writeTimeout(8, TimeUnit.SECONDS)

        .callTimeout(12, TimeUnit.SECONDS)

        .retryOnConnectionFailure(false)

        // Diagnóstico de las conexiones
        .eventListener(object : EventListener() {

            override fun callStart(call: Call) {

                Log.d(
                    "RecordatoriosHTTP",
                    "Inicio: ${call.request().method} " +
                            "${call.request().url}"
                )
            }

            override fun requestHeadersEnd(
                call: Call,
                request: okhttp3.Request
            ) {

                Log.d(
                    "RecordatoriosHTTP",
                    "Cabeceras enviadas"
                )
            }

            override fun requestBodyEnd(
                call: Call,
                byteCount: Long
            ) {

                Log.d(
                    "RecordatoriosHTTP",
                    "Datos enviados: $byteCount bytes"
                )
            }

            override fun responseHeadersEnd(
                call: Call,
                response: Response
            ) {

                Log.d(
                    "RecordatoriosHTTP",
                    "Respuesta HTTP: ${response.code}"
                )
            }

            override fun responseBodyEnd(
                call: Call,
                byteCount: Long
            ) {

                Log.d(
                    "RecordatoriosHTTP",
                    "Datos recibidos: $byteCount bytes"
                )
            }

            override fun callFailed(
                call: Call,
                ioe: IOException
            ) {

                Log.e(
                    "RecordatoriosHTTP",
                    "ERROR DE CONEXION",
                    ioe
                )
            }
        })

        .build()

    private val gson = GsonBuilder()
        .serializeNulls()
        .create()

    fun crear(
        context: Context,
        baseUrl: String =
            context.getString(R.string.recordatorios_api_url)
    ): RecordatoriosApi {

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(clienteHttp)
            .addConverterFactory(
                GsonConverterFactory.create(gson)
            )
            .build()
            .create(RecordatoriosApi::class.java)
    }
}
