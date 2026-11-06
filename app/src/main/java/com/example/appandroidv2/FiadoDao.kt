package com.example.appandroidv2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FiadoDao {

    // Registra uma nova compra fiada
    @Insert
    suspend fun inserirFiado(fiado: Fiado): Long

    // Consulta uma dívida pelo seu ID
    @Query("SELECT * FROM fiados WHERE id = :fiadoId")
    suspend fun buscarFiadoPorId(fiadoId: Long): Fiado?

    // Lista as compras fiadas de um cliente
    @Query(
        """
        SELECT * FROM fiados
        WHERE clienteId = :clienteId
        ORDER BY dataHora DESC
        """
    )
    suspend fun listarFiadosDoCliente(
        clienteId: Long
    ): List<Fiado>

    // Lista todas as compras fiadas
    @Query(
        """
        SELECT * FROM fiados
        ORDER BY dataHora DESC
        """
    )
    suspend fun listarTodosFiados(): List<Fiado>
}