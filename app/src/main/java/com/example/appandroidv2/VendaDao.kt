package com.example.appandroidv2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface VendaDao {

    // Registra uma nova venda e retorna seu ID
    @Insert
    suspend fun inserirVenda(venda: Venda): Long

    // Registra os produtos de uma venda
    @Insert
    suspend fun inserirItensVenda(
        itens: List<ItemVenda>
    )

    // Consulta todas as vendas realizadas
    @Query(
        "SELECT * FROM vendas " +
                "ORDER BY dataHora DESC, id DESC"
    )
    suspend fun listarVendas(): List<Venda>

    // Consulta uma venda pelo ID
    @Query(
        "SELECT * FROM vendas " +
                "WHERE id = :vendaId LIMIT 1"
    )
    suspend fun buscarVendaPorId(
        vendaId: Long
    ): Venda?

    // Consulta os produtos de uma venda
    @Query(
        "SELECT * FROM itens_venda " +
                "WHERE vendaId = :vendaId ORDER BY id ASC"
    )
    suspend fun listarItensDaVenda(
        vendaId: Long
    ): List<ItemVenda>
}