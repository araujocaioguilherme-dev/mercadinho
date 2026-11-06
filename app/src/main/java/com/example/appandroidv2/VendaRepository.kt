package com.example.appandroidv2

import androidx.room.withTransaction
import java.math.BigDecimal
import java.math.RoundingMode

class VendaRepository(
    private val banco: AppDatabase
) {

    // Registra a venda e atualiza o estoque
    // dentro de uma única transação
    suspend fun finalizarVenda(
        carrinho: List<ItemCarrinho>,
        formaPagamento: String,
        clienteFiadoId: Long? = null
    ): Long {

        require(carrinho.isNotEmpty()) {
            "O carrinho está vazio"
        }

        return banco.withTransaction {
            // Verifica o cliente responsável pela venda fiada
            if (formaPagamento == "FIADO") {

                val clienteId = requireNotNull(clienteFiadoId) {
                    "Selecione um cliente para a venda fiada"
                }

                val cliente = banco.clienteDao()
                    .buscarClientePorId(clienteId)

                check(cliente != null) {
                    "Cliente não encontrado no cadastro"
                }
            }

            // Prepara os produtos vendidos
            val itens = carrinho.map { item ->

                require(item.quantidade > 0) {
                    "Quantidade inválida"
                }

                val precoCentavos =
                    BigDecimal.valueOf(item.precoUnitario)
                        .movePointRight(2)
                        .setScale(0, RoundingMode.HALF_UP)
                        .longValueExact()

                require(precoCentavos >= 0) {
                    "Preço inválido"
                }

                val subtotal = Math.multiplyExact(
                    precoCentavos,
                    item.quantidade.toLong()
                )

                ItemVenda(
                    vendaId = 0,
                    codigoBarra = item.produto.codigoBarra,
                    nomeProduto = item.produto.nome,
                    quantidade = item.quantidade,
                    precoUnitarioCentavos = precoCentavos,
                    subtotalCentavos = subtotal
                )
            }

            // Calcula o valor total em centavos
            val totalCentavos = itens.fold(0L) {
                    total, item ->

                Math.addExact(
                    total,
                    item.subtotalCentavos
                )
            }

            // Desconta os produtos do estoque
            for (item in itens) {

                val alterados =
                    banco.produtoDao().baixarEstoqueVenda(
                        item.codigoBarra,
                        item.quantidade
                    )

                check(alterados == 1) {
                    "Estoque insuficiente para ${item.nomeProduto}"
                }
            }

            // Registra a venda
            val vendaId = banco.vendaDao().inserirVenda(
                Venda(
                    totalCentavos = totalCentavos,
                    formaPagamento = formaPagamento
                )
            )

            // Registra os produtos vendidos
            banco.vendaDao().inserirItensVenda(
                itens.map { item ->
                    item.copy(vendaId = vendaId)
                }
            )
// Registra a dívida quando a venda é fiada
            if (formaPagamento == "FIADO") {

                val clienteId = requireNotNull(clienteFiadoId) {
                    "Cliente não selecionado"
                }

                banco.fiadoDao().inserirFiado(
                    Fiado(
                        clienteId = clienteId,
                        vendaId = vendaId,
                        valorOriginalCentavos = totalCentavos
                    )
                )
            }
            // Retorna a identificação da venda
            vendaId

        }
    }
}