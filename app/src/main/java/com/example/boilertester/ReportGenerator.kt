package com.example.boilertester

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.example.boilertester.R
import java.io.File
import java.io.FileOutputStream

object ReportGenerator {

    fun generatePDFReport(context: Context,  data: BoilerData): File? {
        return try {
            val fileName = "Отчёт_${data.serialNumber}_${System.currentTimeMillis()}.pdf"
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            directory?.mkdirs()
            val file = File(directory, fileName)

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val styles = ReportStyles()

            var y = 60f

            // Заголовок
            canvas.drawText("ОТЧЁТ ПО ИСПЫТАНИЯМ КОТЛА", 50f, y, styles.titlePaint)
            y += 25f
            canvas.drawText("Заводской номер: ${data.serialNumber}", 50f, y, styles.headerValuePaint)
            y += 20f
            canvas.drawText("Топливо: природный газ с Qнр = ${data.gasLowerHeat} ккал/м3", 50f, y, styles.headerValuePaint)
            y += 30f

            // Основные данные
            y = drawSection(canvas, styles, "ОСНОВНЫЕ ДАННЫЕ", y, listOf(
                "Тип котла:" to if (data.boilerType == "water") "Водогрейный" else "Паровой",
                "Модель котла:" to data.boilerModel,
                "Мощность:" to "${data.power} кВт",
                "Горелка:" to data.burnerModel,
                "Объект:" to data.objectName,
                "Адрес:" to data.address
            ))
            y += 20f

            // Результаты замеров
            val measurements = mutableListOf<Pair<String, String>>()
            if (data.boilerType == "water") {
                measurements.add("tвх/tвых:" to "${data.tIn}/${data.tOut} °C")
                measurements.add("Gводы:" to "${data.waterFlow} м³/ч")
                measurements.add("Pвх/Pвых:" to "${data.pressureIn}/${data.pressureOut} бар")
            } else {
                measurements.add("Pпара:" to "${data.steamPressure} бар")
                measurements.add("Gпара:" to "${data.steamFlow} т/ч")
            }
            measurements.addAll(listOf(
                "Gгаза:" to "${data.gasFlow} м³/ч",
                "η:" to "${data.efficiency} %",
                "CO/CO₂:" to "${data.co} ppm / ${data.co2} %",
                "O₂/α:" to "${data.o2}% / ${"%.3f".format(data.alpha)}",
                "tвозд/tдым:" to "${data.tAir}/${data.tFlue} °C",
                "Разр. топка/за котлом:" to "${data.draftFurnace}/${data.draftAfterBoiler} мм вод.ст.",
                "Pвозд/Pгаз:" to "${data.pAir}/${data.pGas} мбар",
                "tгаз:" to "${data.tGas} °C"
            ))
            y = drawSection(canvas, styles, "РЕЗУЛЬТАТЫ ЗАМЕРОВ", y, measurements)
            y += 20f

            // Расчётные параметры
            val qk = calculateQkEffective(data)
            val qkGcal = qk * 0.000859845
            val q2 = calculateQ2(data)
            val q3 = calculateQ3(data)
            val q5 = calculateQ5(data)
            val eta = 100.0 - (q2 + q3 + q5)
            val bg = calculateBg(data, qk)

            y = drawSection(canvas, styles, "РАСЧЁТНЫЕ ПАРАМЕТРЫ", y, listOf(
                "Qк (кВт):" to "%.1f".format(qk),
                "Qк (Гкал/ч):" to "%.4f".format(qkGcal),
                "q2 (уход. газы):" to "%.2f %%".format(q2),
                "q3 (неполнота):" to "%.2f %%".format(q3),
                "q5 (окруж. среда):" to "%.1f %%".format(q5),
                "η брутто:" to "%.2f %%".format(eta),
                "Вг (норм. расход):" to "%.2f м³/ч".format(bg)
            ))

            // Футер
            val footerY = 810f
            val lineY = 795f
            canvas.drawLine(40f, lineY, 555f, lineY, styles.footerLinePaint)
            canvas.drawText("Дата: ${android.text.format.DateFormat.format("dd.MM.yyyy", System.currentTimeMillis())}", 50f, footerY, styles.footerPaint)
            canvas.drawText("Стр. 1", 520f, footerY, styles.footerPaint)

            document.finishPage(page)
            document.writeTo(FileOutputStream(file))
            document.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawSection(
        canvas: Canvas,
        styles: ReportStyles,
        title: String,
        startY: Float,
        items: List<Pair<String, String>>
    ): Float {
        var y = startY
        canvas.drawRect(40f, y - 15f, 555f, y + 8f, styles.sectionHeaderBgPaint)
        canvas.drawText(title, 50f, y, styles.sectionHeaderPaint)
        y += 25f
        for ((label, value) in items) {
            canvas.drawText(label, 50f, y, styles.labelPaint)
            canvas.drawText(value, 320f, y, styles.valuePaint)
            y += 20f
        }
        return y
    }

    // === Стили ===
    private class ReportStyles {
        val titlePaint = Paint().apply { textSize = 18f; color = Color.rgb(30, 50, 70); isFakeBoldText = true }
        val headerValuePaint = Paint().apply { textSize = 12f; color = Color.rgb(50, 50, 50) }
        val sectionHeaderPaint = Paint().apply { textSize = 14f; color = Color.WHITE; isFakeBoldText = true }
        val sectionHeaderBgPaint = Paint().apply { color = Color.rgb(41, 128, 185) }
        val labelPaint = Paint().apply { textSize = 11f; color = Color.BLACK }
        val valuePaint = Paint().apply { textSize = 11f; color = Color.rgb(41, 128, 185); isFakeBoldText = true }
        val footerPaint = Paint().apply { textSize = 10f; color = Color.GRAY }
        val footerLinePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }
    }

    // === Формулы ===
    private fun calculateQkEffective( data: BoilerData): Double {
        return when {
            data.boilerType == "water" && data.waterFlow > 0 -> {
                val G = data.waterFlow * 1000.0 / 3600.0
                G * 4.186 * (data.tOut - data.tIn)
            }
            data.boilerType == "steam" && data.steamFlow > 0 -> {
                val D = data.steamFlow * 1000.0 / 3600.0
                D * 2777.0
            }
            else -> calculateQkFromGas(data)
        }
    }

    private fun calculateQkFromGas( data: BoilerData): Double {
        val Bnorm = data.gasFlow * (data.gasPressure / 1013.0) * (273.15 / (273.15 + data.gasTemperature))
        val qnrKJ = data.gasLowerHeat * 4.186
        val q2 = calculateQ2(data)
        val q3 = calculateQ3(data)
        val q5 = calculateQ5(data)
        val eta = (100.0 - (q2 + q3 + q5)) / 100.0
        return Bnorm * qnrKJ * eta / 3600.0
    }

    private fun calculateQ2( data: BoilerData): Double = 0.025 * data.alpha * (data.tFlue - data.tAir)

    private fun calculateQ3( data: BoilerData): Double {
        val coPercent = data.co / 10000.0
        val co2Percent = data.co2
        return if (coPercent > 0 && co2Percent > 0) 126.0 * coPercent / (coPercent + co2Percent) else 0.0
    }

    private fun calculateQ5( data: BoilerData): Double = when {
        data.power <= 300 -> 3.0
        data.power <= 1000 -> 2.0
        data.power <= 4000 -> 1.5
        else -> 1.0
    }

    private fun calculateBg( data: BoilerData, qk: Double): Double {
        val qnrKJ = data.gasLowerHeat * 4.186
        val q2 = calculateQ2(data)
        val q3 = calculateQ3(data)
        val q5 = calculateQ5(data)
        val eta = (100.0 - (q2 + q3 + q5)) / 100.0
        return qk / (eta * qnrKJ / 3600.0)
    }
}