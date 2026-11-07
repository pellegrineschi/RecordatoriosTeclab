package com.example.recordatoriosteclab


// Define las operaciones disponibles para los recordatorios.
interface RecordatoriosRepository {

    // Obtener todos los recordatorios.
    suspend fun obtenerTodos(): List<Recordatorio>

    // Buscar un recordatorio por su identificador.
    suspend fun obtenerPorId(id: Long): Recordatorio?

    // Guardar un recordatorio nuevo o actualizar uno existente.
    suspend fun guardar(recordatorio: Recordatorio)
}

// Implementación que guarda los datos en memoria.
class RecordatoriosRepositoryEnMemoria :
    RecordatoriosRepository {

    // Copiamos los datos de la Actividad 1.
    private val recordatorios =
        DatosSimulados.recordatorios.toMutableList()

    // Devolvemos una copia de la lista.
    override suspend fun obtenerTodos(): List<Recordatorio> {
        return recordatorios.toList()
    }

    // Buscamos una tarea por su ID.
    override suspend fun obtenerPorId(
        id: Long
    ): Recordatorio? {
        return recordatorios.find { it.id == id }
    }

    // Guardamos una tarea en nuestra lista.
    override suspend fun guardar(
        recordatorio: Recordatorio
    ) {
        val indice = recordatorios.indexOfFirst {
            it.id == recordatorio.id
        }

        if (indice == -1) {
            // No existe: agregamos el recordatorio.
            recordatorios.add(recordatorio)
        } else {
            // Ya existe: reemplazamos el registro.
            recordatorios[indice] = recordatorio
        }
    }


}
