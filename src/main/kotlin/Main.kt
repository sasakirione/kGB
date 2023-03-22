import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

fun main() = application {
    val chipset = Chipset("pokemon_red")
    val cpu = Cpu(chipset)

    val oneFlame = 456 * (144 + 10)

    Window(title = cpu.getGameTitle(), onCloseRequest = ::exitApplication) {
        var frame by mutableStateOf(0)

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            while (true) {
                var step = 0
                val startTime = System.currentTimeMillis()
                while (step < oneFlame) {
                    step += cpu.step()
                }
                val elapsedTime = System.currentTimeMillis() - startTime
                delay(1000L / 60 - elapsedTime) // Update at 60 FPS
                frame++
            }
        }

        Box {
            Canvas(Modifier.size(160.dp, 144.dp)) {
                renderScreen(cpu)
            }
        }
    }
}

fun DrawScope.renderScreen(cpu: Cpu) {
    for (y in 0 until 144) {
        for (x in 0 until 160) {
            val color = cpu.getPixel(x, y)
            drawRect(color = color, topLeft = Offset(x.toFloat(), y.toFloat()), size = Size(1f, 1f))
        }
    }
}