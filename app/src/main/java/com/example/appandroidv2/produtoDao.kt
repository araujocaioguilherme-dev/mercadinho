package com.example.appandroidv2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
@Dao
interface ProdutoDao {

    // Salvar um produto no estoque
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarProduto(produto: Produto)

    // Buscar um produto pelo código de barras
    @Query(
        "SELECT * FROM produtos " +
                "WHERE codigoBarra = :codigoBarra LIMIT 1"
    )
    suspend fun buscarPorCodigo(
        codigoBarra: String
    ): Produto?

    // Consultar todos os produtos cadastrados
    @Query("SELECT * FROM produtos ORDER BY nome ASC")
    suspend fun listarProdutos(): List<Produto>

    // Atualizar a quantidade disponível
    @Query(
        "UPDATE produtos SET quantidade = :novaQuantidade " +
                "WHERE codigoBarra = :codigoBarra"
    )
    suspend fun atualizarQuantidade(
        codigoBarra: String,
        novaQuantidade: Int
    )

    // Excluir um produto
    @Delete
    suspend fun excluirProduto(produto: Produto)
    // Atualizar os dados de um produto existente
    @Update
    suspend fun atualizarProduto(produto: Produto): Int

    // Desconta a quantidade vendida somente
    // quando existe estoque suficiente
    @Query(
        """
        UPDATE produtos
        SET quantidade = quantidade - :quantidadeVendida
        WHERE codigoBarra = :codigoBarra
        AND quantidade >= :quantidadeVendida
        """
    )
    suspend fun baixarEstoqueVenda(
        codigoBarra: String,
        quantidadeVendida: Int
    ): Int
}
