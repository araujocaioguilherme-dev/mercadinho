package com.example.appandroidv2

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.util.Log

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import com.example.appandroidv2.databinding.ActivityHistoricoVendasBinding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoricoVendasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoricoVendasBinding

    private val banco by lazy {
        AppDatabase.getDatabase(applicationContext)
    }

    private val localeBrasil = Locale.forLanguageTag("pt-BR")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHistoricoVendasBinding.inflate(
            layoutInflater
        )

        setContentView(binding.root)

        // Atualizar o histórico
        binding.btnAtualizarVendas.setOnClickListener {
            carregarVendas()
        }

        // Voltar para a tela anterior
        binding.btnVoltarHistoricoVendas.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        carregarVendas()
    }

    // Formata valores armazenados em centavos
    private fun formatarMoeda(centavos: Long): String {

        val formato = NumberFormat.getCurrencyInstance(
            localeBrasil
        )

        return formato.format(
            BigDecimal.valueOf(centavos, 2)
        )
    }

    // Recupera as vendas do banco Room
    private fun carregarVendas() {

        binding.btnAtualizarVendas.isEnabled = false

        lifecycleScope.launch {

            try {

                val registros = withContext(Dispatchers.IO) {

                    val vendas = banco.vendaDao().listarVendas()

                    vendas.map { venda ->

                        val itens = banco.vendaDao()
                            .listarItensDaVenda(venda.id)

                        Pair(venda, itens)
                    }
                }

                mostrarVendas(registros)

            } catch (erro: Exception) {

                Log.e(
                    "HistoricoVendas",
                    "Erro ao carregar vendas",
                    erro
                )

                Toast.makeText(
                    this@HistoricoVendasActivity,
                    "Não foi possível carregar as vendas",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                binding.btnAtualizarVendas.isEnabled = true
            }
        }
    }

    // Exibe o histórico na tela
    private fun mostrarVendas(
        registros: List<Pair<Venda, List<ItemVenda>>>
    ) {

        binding.containerHistoricoVendas.removeAllViews()

        binding.txtTotalVendas.text =
            "Total de vendas: ${registros.size}"

        val totalGeral = registros.fold(BigDecimal.ZERO) {
                acumulado, registro ->

            acumulado.add(
                BigDecimal.valueOf(
                    registro.first.totalCentavos
                )
            )
        }

        binding.txtValorTotalVendas.text =
            NumberFormat.getCurrencyInstance(localeBrasil)
                .format(totalGeral.movePointLeft(2))
                .let { "Valor vendido: $it" }

        if (registros.isEmpty()) {

            binding.txtVendasVazio.visibility = View.VISIBLE

            return
        }

        binding.txtVendasVazio.visibility = View.GONE

        val formatoData = SimpleDateFormat(
            "dd/MM/yyyy 'às' HH:mm",
            localeBrasil
        )

        registros.forEach { (venda, itens) ->

            val data = formatoData.format(
                Date(venda.dataHora)
            )

            // Monta a descrição dos produtos vendidos
            val descricaoItens = itens.joinToString("\n") { item ->

                "${item.nomeProduto}\n" +
                        "${item.quantidade} x " +
                        formatarMoeda(item.precoUnitarioCentavos) +
                        " = " +
                        formatarMoeda(item.subtotalCentavos)
            }
            val formaPagamentoTexto = when (venda.formaPagamento) {
                "DINHEIRO" -> "Dinheiro"
                "PIX" -> "Pix"
                "CARTAO" -> "Cartão"
                "FIADO" -> "Fiado"
                else -> "Não informado"
            }
            val textoVenda = """
                VENDA Nº ${venda.id}

                Data: $data

                PRODUTOS:
                $descricaoItens

                TOTAL: ${formatarMoeda(venda.totalCentavos)}
                PAGAMENTO: ${'$'}formaPagamentoTexto
            """.trimIndent()

            val cartao = TextView(
                this@HistoricoVendasActivity
            ).apply {

                text = textoVenda

                textSize = 15f

                setTextColor(
                    Color.parseColor("#17365D")
                )

                val margem = (
                        16 * resources.displayMetrics.density
                        ).toInt()

                setPadding(
                    margem,
                    margem,
                    margem,
                    margem
                )

                background = GradientDrawable().apply {

                    setColor(Color.WHITE)

                    cornerRadius = (
                            12 * resources.displayMetrics.density
                            )

                    setStroke(
                        (
                                1 * resources.displayMetrics.density
                                ).toInt(),
                        Color.parseColor("#D1D9E2")
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

            binding.containerHistoricoVendas.addView(
                cartao
            )
        }
    }
}