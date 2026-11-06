package com.example.appandroidv2
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider

import androidx.core.content.ContextCompat

import com.example.appandroidv2.databinding.ActivityNovaVendaBinding

import java.util.concurrent.Executors
import android.view.View
import android.widget.TextView
import java.text.NumberFormat

class NovaVendaActivity : AppCompatActivity() {
    private var clienteFiadoSelecionado: Cliente? = null

    private lateinit var binding: ActivityNovaVendaBinding

    // Executor utilizado para analisar as imagens
    private val cameraExecutor =
        Executors.newSingleThreadExecutor()

    // Leitor de códigos de barras
    private lateinit var barcodeAnalyzer: BarcodeAnalyzer
    private var produtoSelecionado: Produto? = null
    // Produtos adicionados à venda atual
    private val carrinho = mutableListOf<ItemCarrinho>()

    // Impede leituras repetidas
    private var leituraBloqueada = false

    // Solicita autorização para utilizar a câmera
    private val solicitarPermissaoCamera =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { autorizada ->

            if (autorizada) {
                iniciarCamera()
            } else {
                binding.txtStatusScannerVenda.text =
                    "Permissão da câmera negada"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNovaVendaBinding.inflate(
            layoutInflater
        )

        setContentView(binding.root)

        // Mostra os campos de dinheiro somente
        // quando essa forma de pagamento é selecionada
        binding.grupoFormaPagamento.setOnCheckedChangeListener {
                _, checkedId ->

            // Limpa o cliente ao sair da opção Fiado
            if (checkedId != R.id.radioPagamentoFiado) {

                clienteFiadoSelecionado = null

                binding.btnSelecionarClienteFiado.text =
                    "Selecionar cliente para fiado"
            }
            binding.containerPagamentoDinheiro.visibility =
                if (checkedId == R.id.radioPagamentoDinheiro) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            binding.btnSelecionarClienteFiado.visibility =
                if (checkedId == R.id.radioPagamentoFiado) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
        }
        binding.btnSelecionarClienteFiado.setOnClickListener {

            lifecycleScope.launch {
                try {
                    val clientes = withContext(Dispatchers.IO) {
                        AppDatabase.getDatabase(
                            applicationContext
                        ).clienteDao().listarClientes()
                    }

                    if (clientes.isEmpty()) {

                        androidx.appcompat.app.AlertDialog.Builder(
                            this@NovaVendaActivity
                        )
                            .setTitle("Nenhum cliente cadastrado")
                            .setMessage(
                                "Cadastre um cliente antes de registrar uma venda fiada."
                            )
                            .setPositiveButton("OK", null)
                            .show()

                    } else {

                        val nomes = clientes.map { cliente ->
                            cliente.nome
                        }.toTypedArray()

                        androidx.appcompat.app.AlertDialog.Builder(
                            this@NovaVendaActivity
                        )
                            .setTitle("Selecione o cliente")
                            .setItems(nomes) { _, posicao ->

                                clienteFiadoSelecionado =
                                    clientes[posicao]

                                binding.btnSelecionarClienteFiado.text =
                                    "Cliente: ${clientes[posicao].nome}"
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }

                } catch (erro: Exception) {

                    android.widget.Toast.makeText(
                        this@NovaVendaActivity,
                        "Erro ao consultar clientes: ${erro.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
// Atualiza o troco enquanto o valor é digitado
        binding.edtValorRecebido.addTextChangedListener(
            object : android.text.TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    atualizarTrocoVenda()
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {
                }
            }
        )
        // Prepara os campos da venda
        prepararTela()

        // Configura o leitor de código de barras
        barcodeAnalyzer = BarcodeAnalyzer { codigo ->

            runOnUiThread {

                // Aceita somente uma leitura por vez
                if (
                    !leituraBloqueada &&
                    !isFinishing &&
                    !isDestroyed
                ) {

                    leituraBloqueada = true

                    // Preenche o código encontrado
                    binding.edtCodigoVenda.setText(codigo)

                    // Informa o resultado da leitura
                    binding.txtStatusScannerVenda.text =
                        "Código identificado: $codigo"

                    buscarProduto()

                }

            }
        }

        // Botão Cancelar
        binding.btnCancelarVenda.setOnClickListener {
            finish()
        }

// Buscar produto pelo código informado
        binding.btnBuscarProdutoVenda.setOnClickListener {
            buscarProduto()

        }
        // Adicionar produto ao carrinho
        binding.btnAdicionarCarrinho.setOnClickListener {
            adicionarAoCarrinho()
        }
// Finalizar a venda
        binding.btnFinalizarVenda.setOnClickListener {
            confirmarFinalizacaoVenda()
        }
        // Abrir o histórico de vendas
        binding.btnHistoricoVendas.setOnClickListener {

            startActivity(
                android.content.Intent(
                    this,
                    HistoricoVendasActivity::class.java
                )
            )
        }
        // Verifica a permissão antes de abrir a câmera
        verificarPermissaoCamera()
    }

    // Prepara as informações iniciais da venda
    private fun prepararTela() {

        binding.txtStatusScannerVenda.text =
            "Aguardando câmera"

        binding.txtProdutoVenda.text =
            "Nenhum produto selecionado"

        binding.txtPrecoVenda.text =
            "Preço: R$ 0,00"

        binding.txtEstoqueVenda.text =
            "Estoque disponível: --"

        binding.txtCarrinhoVazio.text =
            "Nenhum produto adicionado"

        binding.txtTotalVenda.text =
            "R$ 0,00"

        binding.edtQuantidadeVenda.setText("1")

        // Permanecem desativados até implementarmos
        // a busca, o carrinho e a finalização
        binding.btnBuscarProdutoVenda.isEnabled = true
        binding.btnAdicionarCarrinho.isEnabled = false
        binding.btnFinalizarVenda.isEnabled = false
    }

    // Verifica se a câmera já foi autorizada
    private fun verificarPermissaoCamera() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            iniciarCamera()

        } else {

            solicitarPermissaoCamera.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    // Inicia a câmera utilizando CameraX
    private fun iniciarCamera() {

        binding.txtStatusScannerVenda.text =
            "Iniciando câmera..."

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            try {

                if (isFinishing || isDestroyed) {
                    return@addListener
                }

                val cameraProvider =
                    cameraProviderFuture.get()

                // Configura a visualização
                val preview = Preview.Builder()
                    .build()

                preview.setSurfaceProvider(
                    binding.viewFinderVenda.surfaceProvider
                )

                // Configura a análise de imagens
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build()

                // Conecta o leitor à câmera
                imageAnalysis.setAnalyzer(
                    cameraExecutor,
                    barcodeAnalyzer
                )

                // Seleciona a câmera traseira
                val cameraSelector =
                    CameraSelector.DEFAULT_BACK_CAMERA

                // Conecta a câmera ao ciclo de vida
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                binding.txtStatusScannerVenda.text =
                    "Aponte a câmera para o código de barras"

            } catch (erro: Exception) {

                Log.e(
                    "NovaVendaActivity",
                    "Erro ao iniciar câmera",
                    erro
                )

                binding.txtStatusScannerVenda.text =
                    "Não foi possível iniciar a câmera"
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // Libera os recursos quando a tela é destruída
    override fun onDestroy() {
        super.onDestroy()

        if (::barcodeAnalyzer.isInitialized) {
            barcodeAnalyzer.fechar()
        }

        cameraExecutor.shutdown()
    }
        // Consulta um produto cadastrado no banco Room
        private fun buscarProduto() {

            val codigo = binding.edtCodigoVenda.text
                .toString()
                .trim()

            // Limpa a seleção anterior
            produtoSelecionado = null

            binding.btnAdicionarCarrinho.isEnabled = false

            binding.txtProdutoVenda.text =
                "Nenhum produto selecionado"

            binding.txtPrecoVenda.text =
                "Preço: R$ 0,00"

            binding.txtEstoqueVenda.text =
                "Estoque disponível: --"

            // Verifica se foi informado um código
            if (codigo.isBlank()) {

                binding.edtCodigoVenda.error =
                    "Informe o código de barras"

                leituraBloqueada = false
                return
            }

            binding.btnBuscarProdutoVenda.isEnabled = false

            lifecycleScope.launch {

                try {

                    // Consulta o banco sem bloquear a tela
                    val produto = withContext(Dispatchers.IO) {

                        AppDatabase.getDatabase(
                            applicationContext
                        ).produtoDao().buscarPorCodigo(codigo)
                    }

                    // Ignora a busca se o código foi alterado
                    if (
                        binding.edtCodigoVenda.text
                            .toString().trim() != codigo
                    ) {
                        leituraBloqueada = false
                        return@launch
                    }

                    if (produto == null) {

                        binding.txtStatusScannerVenda.text =
                            "Produto não encontrado"

                        Toast.makeText(
                            this@NovaVendaActivity,
                            "Produto não cadastrado no estoque",
                            Toast.LENGTH_LONG
                        ).show()

                        leituraBloqueada = false
                        return@launch
                    }

                    // Guarda o produto encontrado
                    produtoSelecionado = produto

                    // Mostra o nome
                    binding.txtProdutoVenda.text =
                        produto.nome

                    // Mostra o preço em reais
                    binding.txtPrecoVenda.text =
                        String.format(
                            Locale.forLanguageTag("pt-BR"),
                            "Preço: R$ %.2f",
                            produto.preco
                        )

                    // Mostra a quantidade disponível
                    binding.txtEstoqueVenda.text =
                        "Estoque disponível: ${produto.quantidade}"

                    binding.txtStatusScannerVenda.text =
                        "Produto encontrado: ${produto.nome}"

                    // O botão de adicionar será programado
                    // na próxima etapa
                    binding.btnAdicionarCarrinho.isEnabled =
                        produto.quantidade > 0
                } catch (erro: Exception) {

                    binding.txtStatusScannerVenda.text =
                        "Erro ao consultar produto"

                    Toast.makeText(
                        this@NovaVendaActivity,
                        "Não foi possível consultar o estoque",
                        Toast.LENGTH_LONG
                    ).show()

                    leituraBloqueada = false

                } finally {

                    binding.btnBuscarProdutoVenda.isEnabled = true
                }
            }
        }
    // Adiciona um produto ao carrinho
    private fun adicionarAoCarrinho() {

        val produto = produtoSelecionado ?: return

        val codigoInformado = binding.edtCodigoVenda.text
            .toString()
            .trim()

        if (codigoInformado != produto.codigoBarra) {
            binding.edtCodigoVenda.error =
                "Busque novamente o produto"
            return
        }

        val quantidade = binding.edtQuantidadeVenda.text
            .toString()
            .trim()
            .toIntOrNull()

        if (quantidade == null || quantidade <= 0) {
            binding.edtQuantidadeVenda.error =
                "Informe uma quantidade válida"
            return
        }

        // Verifica se o produto já está no carrinho
        val itemExistente = carrinho.find {
            it.produto.codigoBarra == produto.codigoBarra
        }

        val quantidadeAnterior =
            itemExistente?.quantidade ?: 0

        val quantidadeTotal =
            quantidadeAnterior.toLong() + quantidade.toLong()

        // Não permite ultrapassar o estoque disponível
        if (quantidadeTotal > produto.quantidade) {
            binding.edtQuantidadeVenda.error =
                "Quantidade superior ao estoque disponível"
            return
        }

        if (itemExistente == null) {

            carrinho.add(
                ItemCarrinho(
                    produto = produto,
                    quantidade = quantidade
                )
            )

        } else {

            itemExistente.quantidade =
                quantidadeTotal.toInt()
        }

        // Atualiza a lista e o total
        atualizarCarrinho()

        // Prepara a tela para o próximo produto
        produtoSelecionado = null

        binding.btnAdicionarCarrinho.isEnabled = false

        binding.edtCodigoVenda.text.clear()
        binding.edtQuantidadeVenda.setText("1")

        binding.txtProdutoVenda.text =
            "Nenhum produto selecionado"

        binding.txtPrecoVenda.text =
            "Preço: R$ 0,00"

        binding.txtEstoqueVenda.text =
            "Estoque disponível: --"

        binding.txtStatusScannerVenda.text =
            "Produto adicionado. Pronto para nova leitura."

        leituraBloqueada = false
    }

    // Atualiza os itens e o valor total
    private fun atualizarCarrinho() {

        binding.containerItensVenda.removeAllViews()

        binding.txtCarrinhoVazio.visibility =
            if (carrinho.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

        val formatoMoeda =
            NumberFormat.getCurrencyInstance(
                Locale.forLanguageTag("pt-BR")
            )

        carrinho.forEach { item ->

            val textoItem = TextView(this).apply {

                text = "${item.produto.nome}\n" +
                        "${item.quantidade} x " +
                        formatoMoeda.format(item.precoUnitario) +
                        "\nSubtotal: " +
                        formatoMoeda.format(item.subtotal)

                textSize = 16f

                setPadding(16, 16, 16, 24)
            }

            binding.containerItensVenda.addView(textoItem)

            val botaoRemover = android.widget.Button(this).apply {

                text = "Remover ${item.produto.nome}"

                setOnClickListener {

                    androidx.appcompat.app.AlertDialog.Builder(
                        this@NovaVendaActivity
                    )
                        .setTitle("Remover produto?")
                        .setMessage(
                            "Deseja remover ${item.produto.nome} do carrinho?"
                        )
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Remover") { _, _ ->

                            carrinho.remove(item)

                            atualizarCarrinho()
                        }
                        .show()
                }
            }

            binding.containerItensVenda.addView(botaoRemover)
            // Botão para diminuir a quantidade
            val botaoMenos = android.widget.Button(this).apply {

                text = "−"

                setOnClickListener {

                    if (item.quantidade > 1) {

                        item.quantidade -= 1

                        atualizarCarrinho()
                    }
                }
            }

            // Botão para aumentar a quantidade
            val botaoMais = android.widget.Button(this).apply {

                text = "+"

                setOnClickListener {

                    if (item.quantidade < item.produto.quantidade) {

                        item.quantidade += 1

                        atualizarCarrinho()

                    } else {

                        android.widget.Toast.makeText(
                            this@NovaVendaActivity,
                            "Quantidade máxima do estoque atingida",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            // Coloca os dois botões lado a lado
            val linhaQuantidade =
                android.widget.LinearLayout(this).apply {

                    orientation =
                        android.widget.LinearLayout.HORIZONTAL

                    addView(botaoMenos)
                    addView(botaoMais)
                }

            binding.containerItensVenda.addView(linhaQuantidade)
        }

        // Calcula o total do carrinho
        val total = carrinho.sumOf {
            it.subtotal
        }

        binding.txtTotalVenda.text =
            formatoMoeda.format(total)

        // Ativa a finalização somente quando houver produtos
        binding.btnFinalizarVenda.isEnabled =
            carrinho.isNotEmpty()
        atualizarTrocoVenda()
    }
    // Solicita confirmação antes de registrar a venda
    private fun confirmarFinalizacaoVenda() {

        if (carrinho.isEmpty()) {
            android.widget.Toast.makeText(
                this,
                "O carrinho está vazio",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
// Valida o pagamento em dinheiro
        if (binding.radioPagamentoDinheiro.isChecked) {

            val valorRecebido = binding.edtValorRecebido.text
                .toString()
                .trim()
                .replace(",", ".")
                .toBigDecimalOrNull()

            if (valorRecebido == null ||
                valorRecebido.signum() < 0 ||
                valorRecebido.scale() > 2) {

                binding.edtValorRecebido.error =
                    "Informe um valor recebido válido"

                binding.edtValorRecebido.requestFocus()
                return
            }

            val totalCompra = carrinho.fold(
                java.math.BigDecimal.ZERO
            ) { acumulado, item ->

                val preco = java.math.BigDecimal
                    .valueOf(item.precoUnitario)
                    .setScale(
                        2,
                        java.math.RoundingMode.HALF_UP
                    )

                acumulado.add(
                    preco.multiply(
                        java.math.BigDecimal.valueOf(
                            item.quantidade.toLong()
                        )
                    )
                )
            }

            if (valorRecebido < totalCompra) {

                binding.edtValorRecebido.error =
                    "Valor insuficiente para finalizar a venda"

                binding.edtValorRecebido.requestFocus()
                return
            }
        }
        // Guarda uma cópia dos itens desta venda
        val itensDaVenda = carrinho.map { it.copy() }
        // Impede venda fiada sem cliente selecionado
        if (
            binding.radioPagamentoFiado.isChecked &&
            clienteFiadoSelecionado == null
        ) {
            android.widget.Toast.makeText(
                this,
                "Selecione um cliente para registrar o fiado",
                android.widget.Toast.LENGTH_LONG
            ).show()

            return
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Finalizar venda?")
            .setMessage(
                "Deseja confirmar a venda dos produtos " +
                        "adicionados ao carrinho?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar venda") { _, _ ->

                // Evita novos cliques durante o registro
                binding.btnFinalizarVenda.isEnabled = false

                lifecycleScope.launch {

                    try {

                        val banco = AppDatabase.getDatabase(
                            applicationContext
                        )

                        val repositorio = VendaRepository(banco)

                        val vendaId = repositorio.finalizarVenda(
                            carrinho = itensDaVenda,

                            formaPagamento = when {
                                binding.radioPagamentoDinheiro.isChecked -> "DINHEIRO"
                                binding.radioPagamentoPix.isChecked -> "PIX"
                                binding.radioPagamentoCartao.isChecked -> "CARTAO"
                                binding.radioPagamentoFiado.isChecked -> "FIADO"

                                else -> throw IllegalStateException(
                                    "Selecione uma forma de pagamento"
                                )
                            },

                            clienteFiadoId = if (binding.radioPagamentoFiado.isChecked) {
                                clienteFiadoSelecionado?.id
                            } else {
                                null
                            }
                        )

                        // Limpa o carrinho após salvar
                        carrinho.clear()
                        clienteFiadoSelecionado = null
                        binding.edtValorRecebido.text.clear()

                        binding.radioPagamentoDinheiro.isChecked = true

                        binding.btnSelecionarClienteFiado.text =
                            "Selecionar cliente para fiado"
                        atualizarCarrinho()

                        produtoSelecionado = null
                        binding.btnAdicionarCarrinho.isEnabled = false

                        binding.txtStatusScannerVenda.text =
                            "Venda número $vendaId registrada com sucesso"

                        android.widget.Toast.makeText(
                            this@NovaVendaActivity,
                            "Venda finalizada com sucesso!",
                            android.widget.Toast.LENGTH_LONG
                        ).show()

                    } catch (erro: Exception) {

                        android.widget.Toast.makeText(
                            this@NovaVendaActivity,
                            erro.message ?: "Erro ao finalizar venda",
                            android.widget.Toast.LENGTH_LONG
                        ).show()

                        // Permite tentar novamente
                        binding.btnFinalizarVenda.isEnabled =
                            carrinho.isNotEmpty()



                    }
                }
            }
            .show()
    }
    // Calcula o troco da venda em dinheiro
    private fun atualizarTrocoVenda() {

        val textoRecebido = binding.edtValorRecebido.text
            .toString()
            .trim()
            .replace(",", ".")

        val recebido = textoRecebido.toBigDecimalOrNull()

        if (recebido == null || recebido.signum() < 0 ||
            recebido.scale() > 2) {

            binding.txtTrocoVenda.text = "Troco: R$ 0,00"
            return
        }

        val total = carrinho.fold(java.math.BigDecimal.ZERO) {
                acumulado, item ->

            val preco = java.math.BigDecimal
                .valueOf(item.precoUnitario)
                .setScale(2, java.math.RoundingMode.HALF_UP)

            acumulado.add(
                preco.multiply(
                    java.math.BigDecimal.valueOf(
                        item.quantidade.toLong()
                    )
                )
            )
        }

        if (recebido < total) {
            binding.txtTrocoVenda.text =
                "Valor recebido insuficiente"
            return
        }

        val troco = recebido.subtract(total)

        val formato = java.text.NumberFormat
            .getCurrencyInstance(
                java.util.Locale.forLanguageTag("pt-BR")
            )

        binding.txtTrocoVenda.text =
            "Troco: ${formato.format(troco)}"
    }}
