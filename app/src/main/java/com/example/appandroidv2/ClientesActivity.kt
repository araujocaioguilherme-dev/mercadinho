package com.example.appandroidv2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.LinearLayout
import android.widget.TextView
class ClientesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Conecta a Activity à tela de cadastro de clientes
        setContentView(R.layout.activity_clientes)
        atualizarListaClientes()
        // Campos do formulário
        val edtNome = findViewById<EditText>(R.id.edtNomeCliente)
        val edtTelefone = findViewById<EditText>(R.id.edtTelefoneCliente)
        val btnSalvar = findViewById<Button>(R.id.btnSalvarCliente)

        // Salva o cliente no banco de dados
        btnSalvar.setOnClickListener {

            val nome = edtNome.text.toString().trim()
            val telefone = edtTelefone.text.toString().trim()

            if (nome.isBlank()) {
                edtNome.error = "Informe o nome do cliente"
                edtNome.requestFocus()
                return@setOnClickListener
            }

            btnSalvar.isEnabled = false

            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val banco = AppDatabase.getDatabase(
                            applicationContext
                        )

                        banco.clienteDao().inserirCliente(
                            Cliente(
                                nome = nome,
                                telefone = telefone
                            )
                        )
                    }

                    Toast.makeText(
                        this@ClientesActivity,
                        "Cliente cadastrado com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()

                    edtNome.text.clear()
                    edtTelefone.text.clear()
                    atualizarListaClientes()

                } catch (erro: Exception) {
                    Toast.makeText(
                        this@ClientesActivity,
                        "Erro ao cadastrar cliente: ${erro.message}",
                        Toast.LENGTH_LONG
                    ).show()

                } finally {
                    btnSalvar.isEnabled = true
                }
            }
        }
    }// Exibe os clientes cadastrados
    private fun atualizarListaClientes() {

        val container = findViewById<LinearLayout>(
            R.id.containerListaClientes
        )

        lifecycleScope.launch {
            try {
                val clientes = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(
                        applicationContext
                    ).clienteDao().listarClientes()
                }

                container.removeAllViews()

                if (clientes.isEmpty()) {
                    val mensagem = TextView(this@ClientesActivity)
                    mensagem.text = "Nenhum cliente cadastrado."
                    mensagem.textSize = 16f
                    container.addView(mensagem)
                } else {
                    clientes.forEach { cliente ->

                        val textoCliente = TextView(
                            this@ClientesActivity
                        )

                        textoCliente.text = buildString {
                            append(cliente.nome)

                            if (cliente.telefone.isNotBlank()) {
                                append("\nTelefone: ")
                                append(cliente.telefone)
                            }
                        }

                        textoCliente.textSize = 16f
                        textoCliente.setPadding(
                            12, 16, 12, 16
                        )

                        // Abre as dívidas do cliente selecionado
                        textoCliente.setOnClickListener {

                            val intent = android.content.Intent(
                                this@ClientesActivity,
                                FiadosClienteActivity::class.java
                            )

                            intent.putExtra("clienteId", cliente.id)

                            startActivity(intent)
                        }
                        container.addView(textoCliente)
                    }
                }

            } catch (erro: Exception) {
                Toast.makeText(
                    this@ClientesActivity,
                    "Erro ao carregar clientes: ${erro.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}