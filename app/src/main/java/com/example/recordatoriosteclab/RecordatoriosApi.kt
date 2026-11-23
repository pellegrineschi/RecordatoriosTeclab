

package com.example.recordatoriosteclab

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface RecordatoriosApi {

    // Comprobar el estado del servidor
    @GET("salud")
    suspend fun comprobarServidor():
            Response<SaludDto>


    // Obtener los recordatorios remotos
    @GET("recordatorios")
    suspend fun obtenerRecordatorios():
            Response<List<RecordatorioDto>>


    // Crear, actualizar o registrar una eliminación
    @PUT("recordatorios/{clave}")
    suspend fun guardarRecordatorio(

        @Path("clave")
        clave: String,

        @Body
        recordatorio: RecordatorioDto

    ): Response<RecordatorioDto>
}
