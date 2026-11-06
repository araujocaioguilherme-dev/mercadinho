package com.example.appandroidv2

import android.os.Bundle
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import com.example.appandroidv2.databinding.ActivityMovimentacaoBinding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MovimentacaoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMovimentacaoBinding

    private var produtoSelecionado: Produto? = null

    private val banco by lazy {
        AppDatabase.getDatabase(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMovimentacaoBinding.inflate(
            layoutInflater
        )

        setContentView(binding.root)

        // Impede movimentação sem produto selecionado
        binding.btnRegistrarMovimentacao.isEnabled = false

        // Buscar produto
        binding.btnBuscarProdutoMovimentacao.setOnClickListener {
            buscarProduto()
        }

        // Registrar entrada ou saída
        binding.btnRegistrarMovimentacao.setOnClickListener {
            registrarMovimentacao()
        }

        // Cancelar e voltar
        binding.btnCancelarMovimentacao.setOnClickListener {
            finish()
        }
        // Abrir histórico de movimentações
        binding.btnVerHistoricoMovimentacoes.setOnClickListener {

            startActivity(
                android.content.Intent(
                    this,
                    HistoricoMovimentacoesActivity::class.java
                )
            )
        }
    }

    // Buscar o produto pelo código de barras
    private fun buscarProduto() {

        val codigo = binding.edtCodigoMovimentacao.text
            .toString()
            .trim()

        produtoSelecionado = null
        binding.btnRegistrarMovimentacao.isEnabled = false

        binding.txtProdutoMovimentacao.text =
            "Nenhum produto selecionado"

        binding.txtEstoqueAtualMovimentacao.text =
            "Estoque atual: --"

        if (codigo.isBlank()) {

            binding.edtCodigoMovimentacao.error =
                "Informe o código de barras"

            return
        }

        binding.btnBuscarProdutoMovimentacao.isEnabled = false

        lifecycleScope.launch {

            try {

                val produto = withContext(Dispatchers.IO) {
                    banco.produtoDao().buscarPorCodigo(codigo)
                }

                // Evita selecionar um produto se o código
                // foi alterado durante a pesquisa
                if (
                    binding.edtCodigoMovimentacao.text
                        .toString().trim() != codigo
                ) {
                    return@launch
                }

                if (produto == null) {

                    Toast.makeText(
                        this@MovimentacaoActivity,
                        "Produto não encontrado",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }

                produtoSelecionado = produto

                binding.txtProdutoMovimentacao.text =
                    produto.nome

                binding.txtEstoqueAtualMovimentacao.text =
                    "Estoque atual: ${produto.quantidade}"

                binding.btnRegistrarMovimentacao.isEnabled = true

            } catch (erro: Exception) {

                Toast.makeText(
                    this@MovimentacaoActivity,
                    "Erro ao buscar produto",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                binding.btnBuscarProdutoMovimentacao.isEnabled = true
            }
        }
    }

    // Registrar entrada ou saída de mercadorias
    private fun registrarMovimentacao() {

        val produto = produtoSelecionado ?: return

        // Confirma que o código não foi alterado
        val codigoInformado = binding.edtCodigoMovimentacao.text
            .toString().trim()

        if (codigoInformado != produto.codigoBarra) {

            binding.edtCodigoMovimentacao.error =
                "Busque novamente o produto"

            produtoSelecionado = null
            binding.btnRegistrarMovimentacao.isEnabled = false

            return
        }

        val tipo = if (binding.radioEntrada.isChecked) {
            "ENTRADA"
        } else {
            "SAIDA"
        }

        val quantidade = binding.edtQuantidadeMovimentacao.text
            .toString()
            .trim()
            .toIntOrNull()

        val motivo = binding.edtMotivoMovimentacao.text
            .toString()
            .trim()

        // Validar quantidade
        if (quantidade == null || quantidade <= 0) {

            binding.edtQuantidadeMovimentacao.error =
                "Informe uma quantidade maior que zero"

            return
        }

        // Validar motivo
        if (motivo.isBlank()) {

            binding.edtMotivoMovimentacao.error =
                "Informe o motivo da movimentação"

            return
        }

        binding.btnRegistrarMovimentacao.isEnabled = false

        lifecycleScope.launch {

            try {

                val novoEstoque = withContext(Dispatchers.IO) {

                    ControleMovimentacao(banco).registrar(
                        codigoBarra = produto.codigoBarra,
                        tipo = tipo,
                        quantidade = quantidade,
                        motivo = motivo
                    )
                }

                // Atualiza a informação na tela
                produtoSelecionado = produto.copy(
                    quantidade = novoEstoque
                )

                binding.txtEstoqueAtualMovimentacao.text =
                    "Estoque atual: $novoEstoque"

                // Limpa os campos da operação
                binding.edtQuantidadeMovimentacao.text.clear()
                binding.edtMotivoMovimentacao.text.clear()

                Toast.makeText(
                    this@MovimentacaoActivity,
                    "Movimentação registrada com sucesso!",
                    Toast.LENGTH_LONG
                ).show()

            } catch (erro: IllegalArgumentException) {

                Toast.makeText(
                    this@MovimentacaoActivity,
                    erro.message ?: "Dados inválidos",
                    Toast.LENGTH_LONG
                ).show()

            } catch (erro: IllegalStateException) {

                Toast.makeText(
                    this@MovimentacaoActivity,
                    erro.message ?: "Movimentação inválida",
                    Toast.LENGTH_LONG
                ).show()

            } catch (erro: Exception) {

                Toast.makeText(
                    this@MovimentacaoActivity,
                    "Erro ao registrar movimentação",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                binding.btnRegistrarMovimentacao.isEnabled =
                    produtoSelecionado != null
            }
        }
    }
}