package com.orchestrator.app.data.device

import com.orchestrator.app.data.model.StepsData
import com.orchestrator.app.data.repository.StepUploader
import com.orchestrator.app.data.store.StepPrefs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeStepPrefs : StepPrefs {
    override var stepDay: String = ""
    override var stepAccumulated: Int = 0
    override var stepLastSensor: Int = -1
    override var stepLastUploadHour: String = ""
}

private class FakeStepReader(private val readings: MutableList<Int?>) : StepReader {
    override suspend fun readCumulative(): Int? =
        if (readings.isEmpty()) null else readings.removeAt(0)
}

private class FakeUploader(var throwOnSend: Boolean = false) : StepUploader {
    val sent = mutableListOf<StepsData>()
    override suspend fun sendSteps(body: StepsData) {
        sent += body
        if (throwOnSend) throw RuntimeException("boom")
    }
}

class DailyStepTrackerTest {

    @Test
    fun `normal increments accumulate`() = runTest {
        val prefs = FakeStepPrefs()
        val reader = FakeStepReader(mutableListOf(100, 150, 170))
        val t = DailyStepTracker(reader, prefs, FakeUploader())

        // First read establishes baseline (adds 0), subsequent reads add deltas.
        assertEquals(0, t.update(TODAY))
        assertEquals(50, t.update(TODAY))
        assertEquals(70, t.update(TODAY))
        assertEquals(70, prefs.stepAccumulated)
        assertEquals(170, prefs.stepLastSensor)
    }

    @Test
    fun `reboot mid-day adds new reading not negative`() = runTest {
        val prefs = FakeStepPrefs()
        val reader = FakeStepReader(mutableListOf(100, 500, 30))
        val t = DailyStepTracker(reader, prefs, FakeUploader())

        assertEquals(0, t.update(TODAY))    // baseline 100
        assertEquals(400, t.update(TODAY))  // +400
        assertEquals(430, t.update(TODAY))  // reboot: sensor 30 < last 500 -> +30
        assertEquals(30, prefs.stepLastSensor)
    }

    @Test
    fun `first read adds zero`() = runTest {
        val prefs = FakeStepPrefs()
        val reader = FakeStepReader(mutableListOf(4200))
        val t = DailyStepTracker(reader, prefs, FakeUploader())

        assertEquals(0, t.update(TODAY))
        assertEquals(0, prefs.stepAccumulated)
        assertEquals(4200, prefs.stepLastSensor)
        assertEquals(TODAY, prefs.stepDay)
    }

    @Test
    fun `null read is a no-op`() = runTest {
        val prefs = FakeStepPrefs().apply {
            stepDay = TODAY
            stepAccumulated = 55
            stepLastSensor = 900
        }
        val reader = FakeStepReader(mutableListOf(null))
        val t = DailyStepTracker(reader, prefs, FakeUploader())

        assertEquals(55, t.update(TODAY))
        assertEquals(55, prefs.stepAccumulated)
        assertEquals(900, prefs.stepLastSensor)
        assertEquals(TODAY, prefs.stepDay)
    }

    @Test
    fun `day rollover resets and flushes old day`() = runTest {
        val prefs = FakeStepPrefs().apply {
            stepDay = YESTERDAY
            stepAccumulated = 8000
            stepLastSensor = 12000
        }
        val reader = FakeStepReader(mutableListOf(12500))
        val uploader = FakeUploader()
        val t = DailyStepTracker(reader, prefs, uploader)

        assertEquals(0, t.update(TODAY))
        assertEquals(0, prefs.stepAccumulated)
        assertEquals(TODAY, prefs.stepDay)
        assertEquals(12500, prefs.stepLastSensor)

        assertEquals(1, uploader.sent.size)
        assertEquals(YESTERDAY, uploader.sent[0].localDate)
        assertEquals(8000, uploader.sent[0].stepsToday)
    }

    @Test
    fun `rollover still resets when flush throws`() = runTest {
        val prefs = FakeStepPrefs().apply {
            stepDay = YESTERDAY
            stepAccumulated = 3000
            stepLastSensor = 9000
        }
        val reader = FakeStepReader(mutableListOf(9100))
        val uploader = FakeUploader(throwOnSend = true)
        val t = DailyStepTracker(reader, prefs, uploader)

        assertEquals(0, t.update(TODAY))
        assertEquals(0, prefs.stepAccumulated)
        assertEquals(TODAY, prefs.stepDay)
        assertEquals(9100, prefs.stepLastSensor)
        assertTrue(uploader.sent.isNotEmpty())
    }

    @Test
    fun `null read on fresh state is null-safe`() = runTest {
        val prefs = FakeStepPrefs()
        val reader = FakeStepReader(mutableListOf(null))
        val t = DailyStepTracker(reader, prefs, FakeUploader())

        // Fresh state: accumulated defaults to 0, and a null read must not throw or mutate.
        assertEquals(0, t.update(TODAY))
        assertEquals("", prefs.stepDay)
        assertEquals(-1, prefs.stepLastSensor)
    }

    companion object {
        private const val TODAY = "2026-07-13"
        private const val YESTERDAY = "2026-07-12"
    }
}
