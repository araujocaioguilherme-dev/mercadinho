package com.example.appandroidv2

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import com.example.appandroidv2.databinding.ActivityHistoricoMovimentacoesBinding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoricoMovimentacoesActivity : AppCompatActivity() {

    private lateinit var binding:
            ActivityHistoricoMovimentacoesBinding

    private val banco by lazy {
        AppDatabase.getDatabase(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityHistoricoMovimentacoesBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        // Botão Atualizar histórico
        binding.btnAtualizarHistorico.setOnClickListener {
            carregarHistorico()
        }

        // Botão Voltar
        binding.btnVoltarHistorico.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        // Atualiza a lista ao abrir ou retornar à tela
        carregarHistorico()
    }

    // Consulta as movimentações no banco Room
    private fun carregarHistorico() {

        binding.btnAtualizarHistorico.isEnabled = false

        lifecycleScope.launch {

            try {

                val movimentacoes =
                    withContext(Dispatchers.IO) {

                        banco.movimentacaoDao()
                            .listarMovimentacoes()
                    }

                mostrarMovimentacoes(movimentacoes)

            } catch (erro: Exception) {

                Toast.makeText(
                    this@HistoricoMovimentacoesActivity,
                    "Erro ao carregar histórico",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                binding.btnAtualizarHistorico.isEnabled = true
            }
        }
    }

    // Mostra as movimentações na tela
    private fun mostrarMovimentacoes(
        movimentacoes: List<Movimentacao>
    ) {

        // Limpa a lista anterior
        binding.containerHistoricoMovimentacoes
            .removeAllViews()

        // Atualiza o total
        binding.txtTotalMovimentacoes.text =
            "Total: ${movimentacoes.size} movimentações"

        // Verifica se existem registros
        if (movimentacoes.isEmpty()) {

            binding.txtHistoricoVazio.visibility =
                View.VISIBLE

            return
        }

        binding.txtHistoricoVazio.visibility =
            View.GONE

        val formatoData = SimpleDateFormat(
            "dd/MM/yyyy 'às' HH:mm",
            Locale.forLanguageTag("pt-BR")
        )

        // Cria um cartão para cada movimentação
        movimentacoes.forEach { movimentacao ->

            val entrada =
                movimentacao.tipo == "ENTRADA"

            val tipoTexto = if (entrada) {
                "ENTRADA (+${movimentacao.quantidade})"
            } else {
                "SAÍDA (-${movimentacao.quantidade})"
            }

            val corDestaque = if (entrada) {
                Color.parseColor("#087B46")
            } else {
                Color.parseColor("#B42318")
            }

            val dataFormatada = formatoData.format(
                Date(movimentacao.dataHora)
            )

            val textoMovimentacao = """
                ${movimentacao.nomeProduto}

                $tipoTexto

                Código: ${movimentacao.codigoBarra}
                Data: $dataFormatada
                Motivo: ${movimentacao.motivo}

                Estoque anterior: ${movimentacao.estoqueAnterior}
                Estoque posterior: ${movimentacao.estoquePosterior}
            """.trimIndent()

            // Cria o cartão do histórico
            val cartao = TextView(
                this@HistoricoMovimentacoesActivity
            ).apply {

                text = textoMovimentacao

                textSize = 15f

                setTextColor(
                    Color.parseColor("#17365D")
                )

                val espacamento = (
                        16 * resources.displayMetrics.density
                        ).toInt()

                setPadding(
                    espacamento,
                    espacamento,
                    espacamento,
                    espacamento
                )

                background = GradientDrawable().apply {

                    setColor(Color.WHITE)

                    cornerRadius = (
                            12 * resources.displayMetrics.density
                            )

                    setStroke(
                        (
                                2 * resources.displayMetrics.density
                                ).toInt(),
                        corDestaque
                    )
                }

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {

                    bottomMargin = (
                            12 * resources.displayMetrics.density
                            ).toInt()
                }
            }

            // Adiciona o cartão à lista
            binding.containerHistoricoMovimentacoes
                .addView(cartao)
        }
    }
}