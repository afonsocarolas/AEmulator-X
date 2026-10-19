package app.aemu.core

import org.junit.Assert.*
import org.junit.Test

class PerfStatsTest {
    @Test fun ticksFromStatLineWithSpacesInName() {
        // pid (comm) state ppid pgrp session tty tpgid flags minflt cminflt majflt cmajflt utime stime ...
        val stat = "1234 (app_process (x)) S 1 1234 0 0 -1 4194560 100 0 0 0 250 50 0 0 20 0 12 0 1000 0 0"
        assertEquals(300L, PerfStats.ticksOf(stat))
        assertEquals(0L, PerfStats.ticksOf("garbage"))
    }
    @Test fun rssFromStatus() {
        assertEquals(81234L, PerfStats.rssKbOf("Name:\tqemu\nVmPeak:\t 9 kB\nVmRSS:\t   81234 kB\nThreads: 4\n"))
        assertEquals(0L, PerfStats.rssKbOf("Name:\tkthread\n"))
    }
    @Test fun cpuShareOfThePhone() {
        // 100 ticks (1 s of one core) in 1 s on 4 cores = 25 %
        assertEquals(25, PerfStats.cpuPercent(100, 1000, 4))
        assertEquals(100, PerfStats.cpuPercent(10_000, 1000, 4))   // capped
        assertEquals(0, PerfStats.cpuPercent(50, 0, 4))
    }
    @Test fun framesPerSecond() {
        assertEquals(60, PerfStats.fps(60, 1000))
        assertEquals(31, PerfStats.fps(61, 2000))   // 30.5 rounds up
        assertEquals(0, PerfStats.fps(-5, 1000))   // counter reset by a restart
    }
}
