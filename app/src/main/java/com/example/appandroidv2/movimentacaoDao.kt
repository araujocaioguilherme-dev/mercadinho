package com.example.appandroidv2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MovimentacaoDao {

    // Registrar entrada ou saída de mercadoria
    @Insert
    suspend fun inserirMovimentacao(
        movimentacao: Movimentacao
    ): Long

    // Consultar todo o histórico de movimentações
    @Query(
        "SELECT * FROM movimentacoes " +
                "ORDER BY dataHora DESC, id DESC"
    )
    suspend fun listarMovimentacoes(): List<Movimentacao>

    // Consultar o histórico de um produto específico
    @Query(
        "SELECT * FROM movimentacoes " +
                "WHERE codigoBarra = :codigoBarra " +
                "ORDER BY dataHora DESC, id DESC"
    )
    suspend fun listarMovimentacoesPorProduto(
        codigoBarra: String
    ): List<Movimentacao>
}