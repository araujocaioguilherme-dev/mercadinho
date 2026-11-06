package com.example.appandroidv2

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Produto::class,
        Movimentacao::class,
        Venda::class,
        ItemVenda::class,
        Cliente::class,
        Fiado::class,
        PagamentoFiado::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun produtoDao(): ProdutoDao
    abstract fun movimentacaoDao(): MovimentacaoDao
    abstract fun vendaDao(): VendaDao
    // Acesso ao cadastro de clientes
    abstract fun clienteDao(): ClienteDao

    // Acesso às compras fiadas
    abstract fun fiadoDao(): FiadoDao

    // Acesso aos pagamentos dos fiados
    abstract fun pagamentoFiadoDao(): PagamentoFiadoDao
    companion object {


        // Migração do banco: versão 1 para versão 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {

            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS movimentacoes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        codigoBarra TEXT NOT NULL,
                        nomeProduto TEXT NOT NULL,
                        tipo TEXT NOT NULL,
                        quantidade INTEGER NOT NULL,
                        motivo TEXT NOT NULL,
                        estoqueAnterior INTEGER NOT NULL,
                        estoquePosterior INTEGER NOT NULL,
                        dataHora INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
        // Migração do banco: versão 2 para versão 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {

            override fun migrate(db: SupportSQLiteDatabase) {

                // Cria a tabela de vendas
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS vendas (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        dataHora INTEGER NOT NULL,
                        totalCentavos INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // Cria a tabela dos produtos vendidos
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS itens_venda (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        vendaId INTEGER NOT NULL,
                        codigoBarra TEXT NOT NULL,
                        nomeProduto TEXT NOT NULL,
                        quantidade INTEGER NOT NULL,
                        precoUnitarioCentavos INTEGER NOT NULL,
                        subtotalCentavos INTEGER NOT NULL,
                        FOREIGN KEY(vendaId)
                            REFERENCES vendas(id)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                // Cria o índice para localizar os itens de cada venda
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_itens_venda_vendaId
                    ON itens_venda (vendaId)
                    """.trimIndent()
                )
            }
        }
        // Migração do banco: versão 3 para versão 4
        private val MIGRATION_3_4 = object : Migration(3, 4) {

            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    ALTER TABLE vendas
                    ADD COLUMN formaPagamento TEXT NOT NULL
                    DEFAULT 'NAO_INFORMADO'
                    """.trimIndent()
                )
            }
        }
        // Migração do banco: versão 4 para versão 5
        private val MIGRATION_4_5 = object : Migration(4, 5) {

            override fun migrate(db: SupportSQLiteDatabase) {

                // Cria a tabela de clientes
                db.execSQL(
                    """
    CREATE TABLE IF NOT EXISTS clientes (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        nome TEXT NOT NULL,
        telefone TEXT NOT NULL
    )
    """.trimIndent()
                )
// Cria a tabela de compras fiadas
                db.execSQL(
                    """
    CREATE TABLE IF NOT EXISTS fiados (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        clienteId INTEGER NOT NULL,
        vendaId INTEGER NOT NULL,
        valorOriginalCentavos INTEGER NOT NULL,
        dataHora INTEGER NOT NULL,
        FOREIGN KEY(clienteId)
            REFERENCES clientes(id)
            ON UPDATE NO ACTION ON DELETE RESTRICT,
        FOREIGN KEY(vendaId)
            REFERENCES vendas(id)
            ON UPDATE NO ACTION ON DELETE RESTRICT
    )
    """.trimIndent()
                )

                // Índice para consultar as dívidas por cliente
                // Índice para consultar as dívidas por cliente
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_fiados_clienteId ON fiados (clienteId)"
                )

                // Impede registrar a mesma venda duas vezes como fiado
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_fiados_vendaId ON fiados (vendaId)"
                )
// Cria a tabela de pagamentos dos fiados
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pagamentos_fiado (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        fiadoId INTEGER NOT NULL,
                        valorCentavos INTEGER NOT NULL,
                        formaPagamento TEXT NOT NULL,
                        dataHora INTEGER NOT NULL,
                        FOREIGN KEY(fiadoId)
                            REFERENCES fiados(id)
                            ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )

                // Índice para consultar os pagamentos de cada dívida
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_pagamentos_fiado_fiadoId ON pagamentos_fiado (fiadoId)"
                )

            }
        }
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                val database = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mercadinho_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,MIGRATION_4_5 )
                    .build()

                INSTANCE ?: database.also {
                    INSTANCE = it
                }
            }
        }
    }
}