
package com.example.appandroidv2
import android.content.res.ColorStateList
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import com.example.appandroidv2.databinding.ActivityConsultaEstoqueBinding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.text.NumberFormat
import java.util.Locale

class ConsultaEstoqueActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConsultaEstoqueBinding

    private var produtos = emptyList<Produto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Conecta o Kotlin à interface XML
        binding = ActivityConsultaEstoqueBinding.inflate(
            layoutInflater
        )

        setContentView(binding.root)

        // Botão Voltar
        binding.btnVoltarEstoque.setOnClickListener {
            finish()
        }

        // Botão Buscar
        binding.btnBuscarEstoque.setOnClickListener {
            mostrarProdutos()
        }

        // Botão Atualizar
        binding.btnAtualizarEstoque.setOnClickListener {
            binding.edtBuscaEstoque.text.clear()
            carregarProdutos()
        }

        // Botão Cadastrar novo produto
        binding.btnCadastrarProduto.setOnClickListener {

            val intent = Intent(
                this,
                EstoqueActivity::class.java
            )

            startActivity(intent)
        }
    }

    // Atualiza a lista ao voltar para esta tela
    override fun onResume() {
        super.onResume()
        carregarProdutos()
    }

    // Consulta os produtos no banco Room
    private fun carregarProdutos() {

        lifecycleScope.launch {

            try {

                val lista = withContext(Dispatchers.IO) {

                    val banco = AppDatabase.getDatabase(
                        applicationContext
                    )

                    banco.produtoDao().listarProdutos()
                }

                produtos = lista

                mostrarProdutos()

            } catch (erro: Exception) {

                Log.e(
                    "ConsultaEstoque",
                    "Erro ao consultar produtos",
                    erro
                )

                binding.containerProdutosEstoque
                    .removeAllViews()

                binding.txtMensagemEstoque.text =
                    "Não foi possível carregar o estoque."

                binding.txtMensagemEstoque.visibility =
                    View.VISIBLE
            }
        }
    }

    // Mostra os produtos encontrados
    private fun mostrarProdutos() {

        val pesquisa = binding.edtBuscaEstoque.text
            .toString()
            .trim()

        val encontrados = produtos.filter { produto ->

            produto.nome.contains(
                pesquisa,
                ignoreCase = true
            ) ||
                    produto.codigoBarra.contains(pesquisa)
        }

        binding.txtTotalProdutos.text =
            "Produtos cadastrados: ${produtos.size}"

        binding.containerProdutosEstoque.removeAllViews()

        // Verifica se existem produtos
        if (encontrados.isEmpty()) {

            binding.txtMensagemEstoque.text =
                if (produtos.isEmpty()) {
                    "Nenhum produto cadastrado."
                } else {
                    "Nenhum produto encontrado."
                }

            binding.txtMensagemEstoque.visibility =
                View.VISIBLE

            return
        }

        binding.txtMensagemEstoque.visibility = View.GONE

        val formatoMoeda = NumberFormat.getCurrencyInstance(
            Locale.forLanguageTag("pt-BR")
        )

        // Cria um cartão para cada produto
        encontrados.forEach { produto ->

            val precoFormatado = formatoMoeda.format(
                produto.preco
            )

            val item = TextView(this).apply {

                text = """
                    ${produto.nome}
                    Código: ${produto.codigoBarra}
                    Quantidade: ${produto.quantidade}
                    Preço: $precoFormatado
                """.trimIndent()

                textSize = 16f

                setTextColor(
                    Color.parseColor("#17365D")
                )

                setPadding(
                    dp(16),
                    dp(12),
                    dp(16),
                    dp(12)
                )

                background = GradientDrawable().apply {

                    setColor(Color.WHITE)

                    cornerRadius = dp(12).toFloat()

                    setStroke(
                        dp(1),
                        Color.parseColor("#E1E7EF")
                    )
                }

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(10)
                }
            }
            item.setOnClickListener {

                val intent = Intent(
                    this@ConsultaEstoqueActivity,
                    EditarProdutoActivity::class.java
                )

                intent.putExtra(
                    EditarProdutoActivity.EXTRA_CODIGO_BARRA,
                    produto.codigoBarra
                )

                startActivity(intent)
            }
            val botaoExcluir = Button(
                this@ConsultaEstoqueActivity
            ).apply {

                text = "Excluir produto"

                contentDescription =
                    "Excluir ${produto.nome} do estoque"

                setTextColor(Color.WHITE)

                backgroundTintList = ColorStateList.valueOf(
                    Color.parseColor("#B42318")
                )

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(48)
                ).apply {
                    bottomMargin = dp(14)
                }

                setOnClickListener {
                    confirmarExclusao(produto)
                }
            }

// Adiciona o produto e o botão à lista
            binding.containerProdutosEstoque.addView(item)
            binding.containerProdutosEstoque.addView(botaoExcluir)

        }
    }
    // Converte medidas para dp
    private fun confirmarExclusao(produto: Produto)

    {

        AlertDialog.Builder(this)
            .setTitle("Excluir produto?")
            .setMessage(
                "Deseja realmente excluir " +
                        "\"${produto.nome}\" do estoque?\n\n" +
                        "Esta ação não poderá ser desfeita."
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ ->

                lifecycleScope.launch {

                    try {

                        // Executa a exclusão no banco Room
                        withContext(Dispatchers.IO) {

                            val banco = AppDatabase.getDatabase(
                                applicationContext
                            )

                            banco.produtoDao().excluirProduto(
                                produto
                            )
                        }

                        Toast.makeText(
                            this@ConsultaEstoqueActivity,
                            "Produto excluído com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Atualiza a lista
                        carregarProdutos()

                    } catch (erro: Exception) {

                        Log.e(
                            "ConsultaEstoque",
                            "Erro ao excluir produto",
                            erro
                        )

                        Toast.makeText(
                            this@ConsultaEstoqueActivity,
                            "Não foi possível excluir o produto",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }
    private fun dp(valor: Int): Int {
        return (
                valor * resources.displayMetrics.density
                ).toInt()
    }
}
