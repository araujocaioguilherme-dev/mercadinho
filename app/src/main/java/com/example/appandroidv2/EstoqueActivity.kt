package com.example.appandroidv2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.appandroidv2.databinding.ActivityMainBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class EstoqueActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var codigoLido: String? = null

    private val cameraExecutor =
        Executors.newSingleThreadExecutor()

    private lateinit var barcodeAnalyzer: BarcodeAnalyzer

    // Solicita permissão para utilizar a câmera
    private val solicitarPermissao =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { autorizada ->

            if (autorizada) {
                iniciarCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissão da câmera negada",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Conecta o Kotlin ao XML
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        configurarBotoesDaTela ( )

        // Prepara o leitor de códigos de barras
        barcodeAnalyzer = BarcodeAnalyzer { codigo ->

            runOnUiThread {

                if (codigoLido != codigo) {
                    codigoLido = codigo

                    binding.txtCodigoLido.text =
                        "Código lido: $codigo"
                }
            }
        }

        // Verifica a permissão da câmera
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            iniciarCamera()
        } else {
            solicitarPermissao.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    // Inicia a câmera traseira
        private fun configurarBotoesDaTela() {

            // Botão Voltar, no cabeçalho
            binding.btnVoltarScanner.setOnClickListener {
                finish()
            }

            // Menu inferior: Início
            binding.navInicio.setOnClickListener {
                finish()
            }

            // Menu inferior: Venda
            binding.navVenda.setOnClickListener {
                Toast.makeText(
                    this,
                    "Você já está na tela do leitor",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Menu inferior: Estoque
            binding.navEstoque.setOnClickListener {
                Toast.makeText(
                    this,
                    "Cadastro de estoque disponível nesta tela",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Menu inferior: Mais
            binding.navMais.setOnClickListener {
                Toast.makeText(
                    this,
                    "Mais opções em desenvolvimento",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Botão Salvar no Estoque
        binding.btnSalvarEstoque.setOnClickListener {

            // Recupera as informações do produto
            val codigo = codigoLido

            val nome = binding.edtNomeProduto.text
                .toString()
                .trim()

            val quantidade = binding.edtQuantidade.text
                .toString()
                .trim()
                .toIntOrNull()

            val preco = binding.edtPreco.text
                .toString()
                .trim()
                .replace(",", ".")
                .toDoubleOrNull()

            // Verifica o código de barras
            if (codigo.isNullOrBlank()) {
                Toast.makeText(
                    this,
                    "Leia o código de barras primeiro",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Verifica o nome
            if (nome.isBlank()) {
                binding.edtNomeProduto.error =
                    "Informe o nome do produto"
                return@setOnClickListener
            }

            // Verifica a quantidade
            if (quantidade == null || quantidade <= 0) {
                binding.edtQuantidade.error =
                    "Informe uma quantidade válida"
                return@setOnClickListener
            }

            // Verifica o preço
            if (preco == null || !preco.isFinite() || preco <= 0.0) {
                binding.edtPreco.error =
                    "Informe um preço válido"
                return@setOnClickListener
            }

            // Cria o produto
            val produto = Produto(
                codigoBarra = codigo,
                nome = nome,
                quantidade = quantidade,
                preco = preco
            )

            // Evita vários cliques durante o salvamento
            binding.btnSalvarEstoque.isEnabled = false

            // Salva no banco de dados Room
            lifecycleScope.launch {

                try {
                    withContext(Dispatchers.IO) {

                        val banco = AppDatabase.getDatabase(
                            applicationContext
                        )

                        banco.produtoDao().salvarProduto(produto)
                    }

                    Toast.makeText(
                        this@EstoqueActivity,
                        "Produto salvo com sucesso!",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()

                } catch (erro: Exception) {

                    Log.e(
                        "EstoqueActivity",
                        "Erro ao salvar produto",
                        erro
                    )

                    Toast.makeText(
                        this@EstoqueActivity,
                        "Erro ao salvar o produto",
                        Toast.LENGTH_LONG
                    ).show()

                    binding.btnSalvarEstoque.isEnabled = true
                }
            }
        }
        }
    private fun iniciarCamera() {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            try {

                val cameraProvider =
                    cameraProviderFuture.get()

                // Configura a visualização
                val preview = Preview.Builder()
                    .build()

                preview.setSurfaceProvider(
                    binding.viewFinder.surfaceProvider
                )

                // Configura a análise das imagens
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

                // Conecta a câmera ao ciclo de vida
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )

            } catch (erro: Exception) {

                Log.e(
                    "EstoqueActivity",
                    "Erro ao iniciar câmera",
                    erro
                )

                Toast.makeText(
                    this,
                    "Não foi possível abrir a câmera",
                    Toast.LENGTH_LONG
                ).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        barcodeAnalyzer.fechar()
        cameraExecutor.shutdown()
        super.onDestroy()
    }
}