package com.example.appandroidv2

import androidx.room.withTransaction

class ControleMovimentacao(
    private val banco: AppDatabase
) {

    suspend fun registrar(
        codigoBarra: String,
        tipo: String,
        quantidade: Int,
        motivo: String
    ): Int {

        // Verifica os dados recebidos
        require(codigoBarra.isNotBlank()) {
            "Informe o código de barras."
        }

        require(tipo == "ENTRADA" || tipo == "SAIDA") {
            "Tipo de movimentação inválido."
        }

        require(quantidade > 0) {
            "A quantidade deve ser maior que zero."
        }

        require(motivo.isNotBlank()) {
            "Informe o motivo da movimentação."
        }

        // Atualiza estoque e histórico juntos
        return banco.withTransaction {

            // Procura o produto cadastrado
            val produto = banco.produtoDao()
                .buscarPorCodigo(codigoBarra)
                ?: throw IllegalArgumentException(
                    "Produto não encontrado."
                )

            // Calcula a nova quantidade
            val novoEstoque = if (tipo == "ENTRADA") {

                produto.quantidade.toLong() +
                        quantidade.toLong()

            } else {

                produto.quantidade.toLong() -
                        quantidade.toLong()
            }

            // Impede estoque negativo ou acima do limite
            check(
                novoEstoque in 0L..Int.MAX_VALUE.toLong()
            ) {
                if (tipo == "SAIDA") {
                    "Estoque insuficiente."
                } else {
                    "Quantidade acima do limite permitido."
                }
            }

            // Atualiza a quantidade disponível
            banco.produtoDao().atualizarQuantidade(
                codigoBarra,
                novoEstoque.toInt()
            )

            // Registra a movimentação no histórico
            banco.movimentacaoDao().inserirMovimentacao(

                Movimentacao(
                    codigoBarra = produto.codigoBarra,
                    nomeProduto = produto.nome,
                    tipo = tipo,
                    quantidade = quantidade,
                    motivo = motivo.trim(),
                    estoqueAnterior = produto.quantidade,
                    estoquePosterior = novoEstoque.toInt()
                )
            )

            // Retorna a quantidade atualizada
            novoEstoque.toInt()
        }
    }
}