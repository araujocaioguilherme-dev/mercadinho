package com.example.appandroidv2

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vendas")
data class Venda(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val dataHora: Long = System.currentTimeMillis(),

    val totalCentavos: Long,

    @androidx.room.ColumnInfo(defaultValue = "'NAO_INFORMADO'")
    val formaPagamento: String = "NAO_INFORMADO"
)