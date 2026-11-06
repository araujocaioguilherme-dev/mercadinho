package com.example.appandroidv2

import android.os.Bundle
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import com.example.appandroidv2.databinding.ActivityEditarProdutoBinding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.util.Locale

class EditarProdutoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CODIGO_BARRA = "codigoBarra"
    }

    private lateinit var binding: ActivityEditarProdutoBinding

    private var produtoAtual: Produto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Conecta o Kotlin ao XML
        binding = ActivityEditarProdutoBinding.inflate(
            layoutInflater
        )

        setContentView(binding.root)

        // Impede salvar antes de carregar o produto
        binding.btnSalvarAlteracoes.isEnabled = false

        // Botão Cancelar
        binding.btnCancelarEdicao.setOnClickListener {
            finish()
        }

        // Recupera o código de barras recebido
        val codigoBarra = intent.getStringExtra(
            EXTRA_CODIGO_BARRA
        )

        if (codigoBarra.isNullOrBlank()) {

            Toast.makeText(
                this,
                "Código do produto não informado",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        // Busca o produto no banco
        carregarProduto(codigoBarra)

        // Botão Salvar alterações
        binding.btnSalvarAlteracoes.setOnClickListener {
            salvarAlteracoes()
        }
    }

    // Carrega os dados do produto cadastrado
    private fun carregarProduto(codigoBarra: String) {

        lifecycleScope.launch {

            try {
                val produto = withContext(Dispatchers.IO) {

                    AppDatabase.getDatabase(
                        applicationContext
                    ).produtoDao().buscarPorCodigo(codigoBarra)
                }

                if (produto == null) {

                    Toast.makeText(
                        this@EditarProdutoActivity,
                        "Produto não encontrado",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                    return@launch
                }

                produtoAtual = produto

                // Preenche os campos da tela
                binding.txtEditarCodigoBarra.text =
                    produto.codigoBarra

                binding.edtEditarNome.setText(
                    produto.nome
                )

                binding.edtEditarQuantidade.setText(
                    produto.quantidade.toString()
                )

                binding.edtEditarPreco.setText(
                    String.format(
                        Locale.forLanguageTag("pt-BR"),
                        "%.2f",
                        produto.preco
                    )
                )

                binding.btnSalvarAlteracoes.isEnabled = true

            } catch (erro: Exception) {

                Toast.makeText(
                    this@EditarProdutoActivity,
                    "Erro ao carregar produto",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Salva as alterações no banco Room
    private fun salvarAlteracoes() {

        val produto = produtoAtual ?: return

        val nome = binding.edtEditarNome.text
            .toString()
            .trim()

        val quantidade = binding.edtEditarQuantidade.text
            .toString()
            .trim()
            .toIntOrNull()

        val preco = binding.edtEditarPreco.text
            .toString()
            .trim()
            .replace(",", ".")
            .toDoubleOrNull()

        // Validação do nome
        if (nome.isBlank()) {

            binding.edtEditarNome.error =
                "Informe o nome do produto"

            return
        }

        // Validação da quantidade
        if (quantidade == null || quantidade < 0) {

            binding.edtEditarQuantidade.error =
                "Informe uma quantidade válida"

            return
        }

        // Validação do preço
        if (preco == null || !preco.isFinite() || preco < 0.0) {

            binding.edtEditarPreco.error =
                "Informe um preço válido"

            return
        }

        // Mantém o mesmo código de barras
        val produtoAtualizado = produto.copy(
            nome = nome,
            quantidade = quantidade,
            preco = preco
        )

        binding.btnSalvarAlteracoes.isEnabled = false

        lifecycleScope.launch {

            try {

                val registros = withContext(Dispatchers.IO) {

                    AppDatabase.getDatabase(
                        applicationContext
                    ).produtoDao().atualizarProduto(
                        produtoAtualizado
                    )
                }

                if (registros > 0) {

                    Toast.makeText(
                        this@EditarProdutoActivity,
                        "Produto atualizado com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()

                    setResult(RESULT_OK)
                    finish()

                } else {

                    Toast.makeText(
                        this@EditarProdutoActivity,
                        "Produto não encontrado para atualização",
                        Toast.LENGTH_LONG
                    ).show()

                    binding.btnSalvarAlteracoes.isEnabled = true
                }

            } catch (erro: Exception) {

                binding.btnSalvarAlteracoes.isEnabled = true

                Toast.makeText(
                    this@EditarProdutoActivity,
                    "Erro ao salvar alterações",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}