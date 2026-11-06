package com.example.appandroidv2

import androidx.room.withTransaction

class FiadoRepository(
    private val banco: AppDatabase
) {

    // Registra um pagamento de uma compra fiada
    suspend fun registrarPagamento(
        fiadoId: Long,
        valorCentavos: Long,
        formaPagamento: String
    ): Long {

        require(valorCentavos > 0L) {
            "O pagamento deve ser maior que zero"
        }

        require(
            formaPagamento in listOf(
                "DINHEIRO", "PIX", "CARTAO"
            )
        ) {
            "Forma de pagamento inválida"
        }

        return banco.withTransaction {

            val fiado = banco.fiadoDao()
                .buscarFiadoPorId(fiadoId)
                ?: throw IllegalStateException(
                    "Compra fiada não encontrada"
                )

            val totalPago = banco.pagamentoFiadoDao()
                .calcularTotalPago(fiadoId)

            val saldo = fiado.valorOriginalCentavos - totalPago

            check(saldo > 0L) {
                "Esta dívida já está quitada"
            }

            check(valorCentavos <= saldo) {
                "O pagamento não pode superar o saldo devedor"
            }

            banco.pagamentoFiadoDao().inserirPagamento(
                PagamentoFiado(
                    fiadoId = fiadoId,
                    valorCentavos = valorCentavos,
                    formaPagamento = formaPagamento
                )
            )
        }
    }
}