package com.example.appandroidv2

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movimentacoes")
data class Movimentacao(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val codigoBarra: String,

    val nomeProduto: String,

    val tipo: String,

    val quantidade: Int,

    val motivo: String,

    val estoqueAnterior: Int,

    val estoquePosterior: Int,

    val dataHora: Long = System.currentTimeMillis()
)