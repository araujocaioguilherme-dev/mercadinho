package com.example.appandroidv2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PagamentoFiadoDao {

    // Registra um pagamento de uma dívida
    @Insert
    suspend fun inserirPagamento(
        pagamento: PagamentoFiado
    ): Long

    // Lista os pagamentos de uma dívida
    @Query(
        """
        SELECT * FROM pagamentos_fiado
        WHERE fiadoId = :fiadoId
        ORDER BY dataHora DESC
        """
    )
    suspend fun listarPagamentos(
        fiadoId: Long
    ): List<PagamentoFiado>

    // Calcula o total já pago de uma dívida
    @Query(
        """
        SELECT COALESCE(SUM(valorCentavos), 0)
        FROM pagamentos_fiado
        WHERE fiadoId = :fiadoId
        """
    )
    suspend fun calcularTotalPago(
        fiadoId: Long
    ): Long
}