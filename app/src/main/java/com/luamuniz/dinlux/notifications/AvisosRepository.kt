package com.luamuniz.dinlux.notifications

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.luamuniz.dinlux.core.FirestoreCollections
import com.luamuniz.dinlux.core.DebugClock
import com.luamuniz.dinlux.finance.Bank
import com.luamuniz.dinlux.finance.BankRepository
import com.luamuniz.dinlux.finance.Card
import com.luamuniz.dinlux.simulation.PaymentMethod
import com.luamuniz.dinlux.simulation.SimulationCalculator
import com.luamuniz.dinlux.simulation.SimulationEntry
import com.luamuniz.dinlux.simulation.SimulationEntryRepository
import com.luamuniz.dinlux.simulation.SimulationEntryType
import com.luamuniz.dinlux.simulation.SimulationRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Efeitos das confirmações do módulo de Avisos
 */
class AvisosRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val bankRepository: BankRepository = BankRepository(),
    private val simulationRepository: SimulationRepository = SimulationRepository(),
    private val entryRepository: SimulationEntryRepository = SimulationEntryRepository()
) {

    private fun banksRef(uid: String) = db.collection(FirestoreCollections.USERS)
        .document(uid)
        .collection(FirestoreCollections.BANKS)

    private fun entriesRef(uid: String) = db.collection(FirestoreCollections.USERS)
        .document(uid)
        .collection(FirestoreCollections.SIMULATION_ENTRIES)

    /**
     * Monta o histórico de sessões
     */
    fun carregarSecoes(onSuccess: (List<AvisoSecao>) -> Unit, onError: (String) -> Unit) {
        bankRepository.loadBanks(
            onSuccess = { banks ->
                simulationRepository.loadSimulations(
                    onSuccess = { simulations ->
                        val ativas = simulations.filter { it.active }
                        if (ativas.isEmpty()) {
                            onSuccess(emptyList())
                            return@loadSimulations
                        }
                        carregarEntriesDeTodasAsAtivas(ativas.map { it.id }, banks, onSuccess, onError)
                    },
                    onError = onError
                )
            },
            onError = onError
        )
    }

    private fun carregarEntriesDeTodasAsAtivas(
        simulationIds: List<String>,
        banks: List<Bank>,
        onSuccess: (List<AvisoSecao>) -> Unit,
        onError: (String) -> Unit
    ) {
        val todasEntries = mutableListOf<SimulationEntry>()
        var restantes = simulationIds.size
        var falhou = false

        simulationIds.forEach { simulationId ->
            entryRepository.loadEntries(
                simulationId = simulationId,
                onSuccess = { entries ->
                    if (falhou) return@loadEntries
                    todasEntries.addAll(entries)
                    restantes--
                    if (restantes == 0) onSuccess(montarSecoes(todasEntries, banks))
                },
                onError = { message ->
                    if (!falhou) {
                        falhou = true
                        onError(message)
                    }
                }
            )
        }
    }

    private fun montarSecoes(entries: List<SimulationEntry>, banks: List<Bank>): List<AvisoSecao> {
        val hoje = DebugClock.hoje()
        val secoes = mutableListOf<AvisoSecao>()

        entries
            .filter { !it.avisosArquivado }
            .forEach { entry ->
                when (entry.type) {
                    SimulationEntryType.SAVINGS -> {
                        val periodos = SimulationCalculator.listarPeriodosEconomia(entry, hoje)
                        if (periodos.isEmpty()) return@forEach
                        val bank = banks.firstOrNull { it.id == entry.bankId }
                        secoes.add(construirSecaoEconomia(entry, periodos, bank, hoje))
                    }
                    SimulationEntryType.PURCHASE -> {
                        val bank = banks.firstOrNull { it.id == entry.bankId }
                        val card = bank?.cards?.firstOrNull { it.id == entry.cardId }
                        val diaVencimento = if (entry.paymentMethod == PaymentMethod.CREDIT) {
                            card?.dueDay ?: 0
                        } else {
                            diaDoMesDaCompra(entry.createdAt)
                        }
                        if (diaVencimento <= 0) return@forEach
                        val ciclos = SimulationCalculator.listarCiclosCompra(entry, diaVencimento, hoje)
                        if (ciclos.isEmpty()) return@forEach
                        secoes.add(construirSecaoCompra(entry, ciclos, bank, card))
                    }
                }
            }

        return secoes.sortedBy { it.ordenacao }
    }

    private fun diaDoMesDaCompra(dataCriacaoMillis: Long): Int {
        return Instant.ofEpochMilli(dataCriacaoMillis).atZone(ZoneId.systemDefault()).toLocalDate().dayOfMonth
    }

    private fun construirSecaoEconomia(
        entry: SimulationEntry,
        periodos: List<Pair<YearMonth, Boolean>>,
        bank: Bank?,
        hoje: LocalDate
    ): AvisoSecao {
        val mensagens = periodos.map { (periodo, paga) -> AvisoMensagem(periodo = periodo, paga = paga) }
        val finalizada = !hoje.isBefore(
            Instant.ofEpochMilli(entry.endDate).atZone(ZoneId.systemDefault()).toLocalDate()
        )
        return AvisoSecao(
            entry = entry,
            tipo = SimulationEntryType.SAVINGS,
            titulo = entry.title,
            subtitulo = "Economia",
            bankId = entry.bankId,
            bankName = bank?.name ?: NOME_BANCO_EXCLUIDO,
            mensagens = mensagens,
            finalizada = finalizada,
            naoVistasCount = contarNaoVistas(entry, mensagens)
        )
    }

    // [card] só é usado aqui pra detectar a divergência de fechamento abaixo
    private fun construirSecaoCompra(
        entry: SimulationEntry,
        ciclos: List<Pair<YearMonth, Boolean>>,
        bank: Bank?,
        card: Card?
    ): AvisoSecao {
        val mensagens = ciclos.map { (periodo, paga) -> AvisoMensagem(periodo = periodo, paga = paga) }
        val finalizada = entry.paidInstallments >= entry.installments
        val subtitulo = if (entry.paymentMethod == PaymentMethod.CREDIT) "Cartão ${entry.cardLabel}" else "Débito"

        val fechamentoAlterado = entry.paymentMethod == PaymentMethod.CREDIT &&
            card != null && entry.cardClosingDay > 0 && card.closingDay > 0 &&
            card.closingDay != entry.cardClosingDay

        return AvisoSecao(
            entry = entry,
            tipo = SimulationEntryType.PURCHASE,
            titulo = entry.title,
            subtitulo = subtitulo,
            bankId = entry.bankId,
            bankName = bank?.name ?: NOME_BANCO_EXCLUIDO,
            mensagens = mensagens,
            finalizada = finalizada,
            naoVistasCount = contarNaoVistas(entry, mensagens),
            cardFechamentoAlterado = fechamentoAlterado,
            cardFechamentoAtual = if (fechamentoAlterado) card!!.closingDay else 0
        )
    }

    private fun contarNaoVistas(entry: SimulationEntry, mensagens: List<AvisoMensagem>): Int {
        val ultimoVisto = if (entry.ultimoPeriodoVisto > 0L) {
            YearMonth.from(Instant.ofEpochMilli(entry.ultimoPeriodoVisto).atZone(ZoneId.systemDefault()).toLocalDate())
        } else {
            null
        }
        return mensagens.count { !it.paga && (ultimoVisto == null || it.periodo.isAfter(ultimoVisto)) }
    }

    private fun periodoParaMillis(periodo: YearMonth): Long {
        return periodo.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * Marca ou desmarca UM período de uma economia como guardado de verdade:
     * - [confirmar] = true: soma o timestamp do período em `confirmedPeriods`
     *   (`FieldValue.arrayUnion`), soma `entry.monthlyAmount` em `savedAmount` e desconta
     *   esse valor do saldo real do banco (`Bank.debit`)
     * - [confirmar] = false: remove o timestamp de `confirmedPeriods`
     *   (`FieldValue.arrayRemove`), desconta o valor de `savedAmount` e DEVOLVE esse
     *   valor pro saldo do banco reverte exatamente o efeito de uma confirmação
     *   anterior. Quem chama é responsável por confirmar com o usuário antes
     */
    fun alternarPeriodoEconomia(
        entry: SimulationEntry,
        periodo: YearMonth,
        confirmar: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }

        val periodoMillis = periodoParaMillis(periodo)
        val valor = entry.monthlyAmount
        val sinal = if (confirmar) 1.0 else -1.0
        val operacaoLista = if (confirmar) {
            FieldValue.arrayUnion(periodoMillis)
        } else {
            FieldValue.arrayRemove(periodoMillis)
        }

        val batch = db.batch()
        batch.update(
            entriesRef(uid).document(entry.id),
            mapOf(
                "confirmedPeriods" to operacaoLista,
                "savedAmount" to FieldValue.increment(valor * sinal)
            )
        )
        batch.update(
            banksRef(uid).document(entry.bankId),
            mapOf("debit" to FieldValue.increment(-valor * sinal))
        )

        batch.commit()
            .addOnFailureListener { onError(it.message ?: "Erro ao atualizar a economia") }
        onSuccess()
    }

    /**
     * Marca ou desmarca UMA parcela (ciclo de fatura, ou de um parcelamento em débito)
     */
    fun alternarCicloCompra(
        entry: SimulationEntry,
        periodo: YearMonth,
        confirmar: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }

        val periodoMillis = periodoParaMillis(periodo)
        val valor = entry.installmentValue
        val sinal = if (confirmar) 1L else -1L
        val operacaoLista = if (confirmar) {
            FieldValue.arrayUnion(periodoMillis)
        } else {
            FieldValue.arrayRemove(periodoMillis)
        }

        val batch = db.batch()
        batch.update(
            entriesRef(uid).document(entry.id),
            mapOf(
                "confirmedPeriods" to operacaoLista,
                "paidInstallments" to FieldValue.increment(sinal)
            )
        )
        batch.update(
            banksRef(uid).document(entry.bankId),
            mapOf("debit" to FieldValue.increment(-valor * sinal))
        )

        batch.commit()
            .addOnFailureListener { onError(it.message ?: "Erro ao atualizar a parcela") }
        onSuccess()
    }

    /**
     * Marca como vistas todas as mensagens pendentes atuais das sessões de um banco (ao
     * abrir o chat dele) grava `ultimoPeriodoVisto` = período da mensagem mais recente
     * de cada sessão. Isso limpa o badge de não-vistas sem confirmar nada
     */
    fun marcarBancoComoVisto(secoes: List<AvisoSecao>, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("Usuário não autenticado")
            return
        }

        val comPendenciaNaoVista = secoes.filter { it.naoVistasCount > 0 }
        if (comPendenciaNaoVista.isEmpty()) {
            onSuccess()
            return
        }

        val zoneId = ZoneId.systemDefault()
        val batch = db.batch()
        comPendenciaNaoVista.forEach { secao ->
            val ultimaMensagem = secao.mensagens.lastOrNull() ?: return@forEach
            val millis = ultimaMensagem.periodo.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            batch.update(entriesRef(uid).document(secao.entry.id), mapOf("ultimoPeriodoVisto" to millis))
        }

        batch.commit()
            .addOnFailureListener { onError(it.message ?: "Erro ao marcar como visto") }
        onSuccess()
    }

    /**
     * Exclui o nó de verdade, some tanto do módulo de Avisos quanto da simulação/canvas
     */
    fun excluirNo(entry: SimulationEntry, onSuccess: () -> Unit, onError: (String) -> Unit) {
        entryRepository.deleteEntry(
            entry = entry,
            onSuccess = onSuccess,
            onError = { message -> onError(message) }
        )
    }

    private companion object {
        const val NOME_BANCO_EXCLUIDO = "Banco excluído"
    }
}
