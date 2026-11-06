package com.example.appandroidv2

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "produtos")
data class Produto(

    @PrimaryKey
    val codigoBarra: String,

    val nome: String,

    val quantidade: Int,

    val preco: Double

)