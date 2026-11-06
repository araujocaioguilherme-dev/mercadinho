package com.example.appandroidv2

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.content.res.ColorStateList
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class InicioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_inicio)

        // Função para abrir a câmera
        fun abrirScanner() {
            val intent = Intent(
                this,
                EstoqueActivity::class.java
            )
            startActivity(intent)
        }

        // BOTÕES PRINCIPAIS

        // Nova Venda
        // Nova Venda
        findViewById<View>(R.id.btnNovaVenda)
            .setOnClickListener {
                startActivity(
                    android.content.Intent(
                        this,
                        NovaVendaActivity::class.java
                    )
                )
            }

        // Estoque
        findViewById<View>(R.id.btnEstoque)
            .setOnClickListener {
                startActivity(
                    android.content.Intent(
                        this,
                        ConsultaEstoqueActivity::class.java
                    )
                )
            }

        // Abre o cadastro de clientes do Fiado
        findViewById<View>(R.id.btnFiado)
            .setOnClickListener {

                startActivity(
                    android.content.Intent(
                        this,
                        ClientesActivity::class.java
                    )
                )
            }

        // Movimento
        findViewById<View>(R.id.btnMovimento)
            .setOnClickListener {
                startActivity(
                    android.content.Intent(
                        this,
                        MovimentacaoActivity::class.java
                    )
                )
            }
        // MENU INFERIOR

        // Início
        findViewById<View>(R.id.navInicio)
            .setOnClickListener {
                Toast.makeText(
                    this,
                    "Você já está na tela inicial",
                    Toast.LENGTH_SHORT
                ).show()
            }

        // Venda
        findViewById<View>(R.id.navVenda)
            .setOnClickListener {
                abrirScanner()
            }

        // Estoque
        findViewById<View>(R.id.navEstoque)
            .setOnClickListener {
                abrirScanner()
            }

        // Mais
        findViewById<View>(R.id.navMais)
            .setOnClickListener {
                Toast.makeText(
                    this,
                    "Mais opções em desenvolvimento",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}