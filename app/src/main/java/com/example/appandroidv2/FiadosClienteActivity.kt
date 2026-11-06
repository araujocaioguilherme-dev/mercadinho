package com.example.appandroidv2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.LinearLayout
class FiadosClienteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Abre a tela de compras fiadas do cliente
        setContentView(R.layout.activity_fiados_cliente)
        // Recebe a identificação do cliente selecionado
        val clienteId = intent.getLongExtra("clienteId", -1L)

        if (clienteId <= 0L) {
            Toast.makeText(
                this,
                "Cliente não identificado",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        // Localiza o campo que apresenta o nome
        val txtNomeCliente = findViewById<TextView>(
            R.id.txtNomeClienteFiado
        )

        // Consulta o cliente no banco Room
        lifecycleScope.launch {
            try {
                val cliente = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(
                        applicationContext
                    ).clienteDao().buscarClientePorId(clienteId)
                }

                if (cliente == null) {
                    Toast.makeText(
                        this@FiadosClienteActivity,
                        "Cliente não encontrado",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                    return@launch
                }

                // Apresenta o nome do cliente na tela
                txtNomeCliente.text = cliente.nome
                atualizarFiadosCliente(clienteId)

            } catch (erro: Exception) {
                Toast.makeText(
                    this@FiadosClienteActivity,
                    "Erro ao consultar cliente: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    // Consulta as compras fiadas e calcula o saldo devedor
    private fun atualizarFiadosCliente(clienteId: Long) {

        val txtSaldo = findViewById<TextView>(
            R.id.txtSaldoTotalCliente
        )

        val container = findViewById<LinearLayout>(
            R.id.containerFiadosCliente
        )

        lifecycleScope.launch {
            try {
                val resultado = withContext(Dispatchers.IO) {

                    val banco = AppDatabase.getDatabase(
                        applicationContext
                    )

                    val fiados = banco.fiadoDao()
                        .listarFiadosDoCliente(clienteId)

                    fiados.map { fiado ->

                        val totalPago = banco.pagamentoFiadoDao()
                            .calcularTotalPago(fiado.id)

                        Pair(fiado, totalPago)
                    }
                }

                val saldoTotal = resultado.sumOf { (fiado, pago) ->
                    (fiado.valorOriginalCentavos - pago)
                        .coerceAtLeast(0L)
                }

                val formato = java.text.NumberFormat
                    .getCurrencyInstance(
                        java.util.Locale.forLanguageTag("pt-BR")
                    )

                fun moeda(centavos: Long): String {
                    return formato.format(
                        java.math.BigDecimal.valueOf(centavos, 2)
                    )
                }

                txtSaldo.text = moeda(saldoTotal)
                container.removeAllViews()

                if (resultado.isEmpty()) {

                    val mensagem = TextView(this@FiadosClienteActivity)
                    mensagem.text = "Nenhuma compra fiada registrada."
                    mensagem.textSize = 16f
                    container.addView(mensagem)

                } else {

                    resultado.forEach { (fiado, pago) ->

                        val saldo = (
                                fiado.valorOriginalCentavos - pago
                                ).coerceAtLeast(0L)
                        val situacao = when {
                            saldo == 0L -> "QUITADA"
                            pago > 0L -> "PARCIALMENTE PAGA"
                            else -> "PENDENTE"
                        }
                        val texto = TextView(this@FiadosClienteActivity)

                        texto.text = """
                            Compra fiada nº ${fiado.id}
                            Valor: ${moeda(fiado.valorOriginalCentavos)}
                            Pago: ${moeda(pago)}
                            Saldo: ${moeda(saldo)}
                            Situação: ${'$'}situacao
                        """.trimIndent()

                        texto.textSize = 16f
                        texto.setPadding(12, 20, 12, 20)
// Abre a janela de pagamento da dívida
                        texto.setOnClickListener {
                            if (saldo <= 0L) {
                                android.widget.Toast.makeText(
                                    this@FiadosClienteActivity,
                                    "Esta dívida já está quitada",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()

                                return@setOnClickListener
                            }

                            val formulario = layoutInflater.inflate(
                                R.layout.dialog_pagamento_fiado,
                                null
                            )

                            val dialogo = androidx.appcompat.app.AlertDialog.Builder(
                                this@FiadosClienteActivity
                            )
                                .setTitle(
                                    "Pagamento da dívida nº ${fiado.id}"
                                )
                                .setView(formulario)
                                .setNegativeButton("Cancelar", null)
                                .setPositiveButton("Registrar pagamento", null)
                                .show()
                            val btnConfirmar = dialogo.getButton(
                                android.content.DialogInterface.BUTTON_POSITIVE
                            )

                            btnConfirmar.setOnClickListener {

                                val campoValor =
                                    formulario.findViewById<android.widget.EditText>(
                                        R.id.edtValorPagamentoFiado
                                    )

                                val grupoPagamento =
                                    formulario.findViewById<android.widget.RadioGroup>(
                                        R.id.grupoPagamentoFiado
                                    )

                                val valor = campoValor.text.toString()
                                    .trim()
                                    .replace(",", ".")
                                    .toBigDecimalOrNull()

                                if (valor == null ||
                                    valor.signum() <= 0 ||
                                    valor.scale() > 2) {

                                    campoValor.error = "Informe um valor válido"
                                    campoValor.requestFocus()
                                    return@setOnClickListener
                                }

                                val saldoDisponivel =
                                    java.math.BigDecimal.valueOf(saldo, 2)

                                if (valor > saldoDisponivel) {
                                    campoValor.error = "Valor superior ao saldo devedor"
                                    campoValor.requestFocus()
                                    return@setOnClickListener
                                }

                                val formaPagamento = when (
                                    grupoPagamento.checkedRadioButtonId
                                ) {
                                    R.id.radioFiadoDinheiro -> "DINHEIRO"
                                    R.id.radioFiadoPix -> "PIX"
                                    R.id.radioFiadoCartao -> "CARTAO"
                                    else -> {
                                        android.widget.Toast.makeText(
                                            this@FiadosClienteActivity,
                                            "Selecione a forma de pagamento",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                        return@setOnClickListener
                                    }
                                }

                                val valorCentavos =
                                    valor.movePointRight(2).longValueExact()

                                btnConfirmar.isEnabled = false

                                lifecycleScope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            val banco = AppDatabase.getDatabase(
                                                applicationContext
                                            )

                                            FiadoRepository(banco).registrarPagamento(
                                                fiadoId = fiado.id,
                                                valorCentavos = valorCentavos,
                                                formaPagamento = formaPagamento
                                            )
                                        }

                                        android.widget.Toast.makeText(
                                            this@FiadosClienteActivity,
                                            "Pagamento registrado com sucesso!",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()

                                        dialogo.dismiss()
                                        atualizarFiadosCliente(clienteId)

                                    } catch (erro: Exception) {
                                        android.widget.Toast.makeText(
                                            this@FiadosClienteActivity,
                                            "Erro no pagamento: ${erro.message}",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()

                                    } finally {
                                        btnConfirmar.isEnabled = true
                                    }
                                }
                            }
                        }
                        container.addView(texto)
                    }
                }

            } catch (erro: Exception) {

                Toast.makeText(
                    this@FiadosClienteActivity,
                    "Erro ao consultar fiados: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }}