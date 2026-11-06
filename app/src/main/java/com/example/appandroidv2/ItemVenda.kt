package com.example.appandroidv2

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "itens_venda",
    foreignKeys = [
        ForeignKey(
            entity = Venda::class,
            parentColumns = ["id"],
            childColumns = ["vendaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["vendaId"])
    ]
)
data class ItemVenda(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Identifica a venda à qual pertence
    val vendaId: Long,

    // Identificação do produto vendido
    val codigoBarra: String,

    val nomeProduto: String,

    // Quantidade vendida
    val quantidade: Int,

    // Preço de uma unidade, em centavos
    val precoUnitarioCentavos: Long,

    // Valor total deste item, em centavos
    val subtotalCentavos: Long

)