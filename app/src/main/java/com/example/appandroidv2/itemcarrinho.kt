package com.example.appandroidv2

data class ItemCarrinho(

    val produto: Produto,

    var quantidade: Int,

    val precoUnitario: Double = produto.preco

) {

    val subtotal: Double
        get() = precoUnitario * quantidade

}