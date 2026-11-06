
package com.example.appandroidv2

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pagamentos_fiado",
    foreignKeys = [
        ForeignKey(
            entity = Fiado::class,
            parentColumns = ["id"],
            childColumns = ["fiadoId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("fiadoId")
    ]
)
data class PagamentoFiado(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val fiadoId: Long,

    val valorCentavos: Long,

    val formaPagamento: String,

    val dataHora: Long = System.currentTimeMillis()
)