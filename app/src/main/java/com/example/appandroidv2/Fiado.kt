package com.example.appandroidv2

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fiados",
    foreignKeys = [
        ForeignKey(
            entity = Cliente::class,
            parentColumns = ["id"],
            childColumns = ["clienteId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Venda::class,
            parentColumns = ["id"],
            childColumns = ["vendaId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("clienteId"),
        Index(value = ["vendaId"], unique = true)
    ]
)
data class Fiado(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val clienteId: Long,

    val vendaId: Long,

    val valorOriginalCentavos: Long,

    val dataHora: Long = System.currentTimeMillis()
)