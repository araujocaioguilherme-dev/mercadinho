package com.example.appandroidv2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ClienteDao {

    // Cadastra um novo cliente
    @Insert
    suspend fun inserirCliente(cliente: Cliente): Long

    // Consulta um cliente pelo seu ID
    @Query("SELECT * FROM clientes WHERE id = :clienteId")
    suspend fun buscarClientePorId(clienteId: Long): Cliente?

    // Lista todos os clientes em ordem alfabética
    @Query("SELECT * FROM clientes ORDER BY nome COLLATE NOCASE ASC")
    suspend fun listarClientes(): List<Cliente>
}