package com.luamuniz.dinlux.core

import android.content.Context
import androidx.core.content.ContextCompat
import com.luamuniz.dinlux.R
import kotlin.math.absoluteValue

/**
 * Paleta de cores usada pra identificar visualmente bancos/cartões no app, no menu de
 * cartões da simulação, nos Grupos de nós do canvas, e nos gráficos do dashboard.
 *
 * A cor de um banco (débito) ou cartão (crédito) começa sempre pela mesma cor
 * "preferida", derivada por hash do próprio id do documento no Firestore. Isso dá uma
 * base estável (o mesmo banco/cartão tende a aparecer com a mesma cor em qualquer tela,
 * sem precisar guardar isso no banco de dados).
 */
object EntityColorPalette {

    private val paletteRes = intArrayOf(
        R.color.node_color_01,
        R.color.node_color_02,
        R.color.node_color_03,
        R.color.node_color_04,
        R.color.node_color_05,
        R.color.node_color_06,
        R.color.node_color_07,
        R.color.node_color_08,
        R.color.node_color_09,
        R.color.node_color_10,
        R.color.node_color_11,
        R.color.node_color_12,
        R.color.node_color_13,
        R.color.node_color_14,
        R.color.node_color_15,
        R.color.node_color_16,
        R.color.node_color_17,
        R.color.node_color_18
    )

    // Matiz (0-360°) de cada cor de paletteRes, NA MESMA ORDEM. Usado só pra medir se
    // duas cores ficariam parecidas demais (ver distanciaCircular). Precisa ser mantido
    // manualmente em sincronia com colors.xml se as cores da paleta mudarem de novo (são
    // as mesmas 18 cores geradas com matiz espaçado a cada 20°, só reordenadas em
    // vermelho/verde/azul/amarelo virem primeiro
    private val paletteHue = intArrayOf(
        0, 120, 240, 60, 20, 40, 80, 100, 140, 160, 180, 200, 220, 260, 280, 300, 320, 340
    )

    // Abaixo dessa distância (em graus, no círculo de cores) duas cores contam como
    // "parecidas demais" pra conviver na mesma tela. Com 18 cores espaçadas a cada 20°,
    // 40° deixa uns 8-9 tons bem separados
    private const val MIN_HUE_DISTANCE = 40

    private fun misturarHash(valor: Int): Int {
        var h = valor
        h = h xor (h ushr 16)
        h *= -0x7ee3623b
        h = h xor (h ushr 13)
        h *= -0x3b314601
        h = h xor (h ushr 16)
        return h
    }

    private fun indicePreferido(id: String): Int =
        misturarHash(id.hashCode()).absoluteValue % paletteRes.size

    // Distância entre dois ângulos no círculo de cores (0-360°), sempre pelo caminho mais curto
    private fun distanciaCircular(hueA: Int, hueB: Int): Int {
        val diferenca = kotlin.math.abs(hueA - hueB) % 360
        return kotlin.math.min(diferenca, 360 - diferenca)
    }

    /**
     * Resolve o ÍNDICE (posição em paletteRes/paletteHue) de cada id em ids, evitando
     * repetir ou deixar duas cores parecidas demais
     *
     * ids são processados em ordem alfabética (não na ordem em que a lista chega) só pra
     * o resultado não depender da ordem de carregamento, o mesmo conjunto de ids sempre
     * resolve pro mesmo mapa de cores
     */
    fun resolveIndices(ids: Collection<String>): Map<String, Int> {
        val idsValidos = ids.filter { it.isNotEmpty() }.distinct().sorted()
        val indicesUsados = mutableListOf<Int>()
        val resultado = LinkedHashMap<String, Int>()

        for (id in idsValidos) {
            var indice = indicePreferido(id)
            var tentativas = 0
            while (
                indicesUsados.any { distanciaCircular(paletteHue[it], paletteHue[indice]) < MIN_HUE_DISTANCE } &&
                tentativas < paletteRes.size
            ) {
                indice = (indice + 1) % paletteRes.size
                tentativas++
            }
            indicesUsados.add(indice)
            resultado[id] = indice
        }
        return resultado
    }

    fun colorAt(context: Context, indice: Int): Int = ContextCompat.getColor(context, paletteRes[indice])

    // Atalho pra um id isolado, sem o conjunto inteiro por perto
    fun colorFor(context: Context, id: String): Int = colorAt(context, indicePreferido(id))
}
