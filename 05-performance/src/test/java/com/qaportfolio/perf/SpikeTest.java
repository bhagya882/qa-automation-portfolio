package com.qaportfolio.perf;

import org.testng.annotations.Test;
import us.abstracta.jmeter.javadsl.core.TestPlanStats;

import java.time.Duration;
import java.util.Set;

import static com.qaportfolio.perf.PerfSettings.*;
import static org.assertj.core.api.Assertions.assertThat;
import static us.abstracta.jmeter.javadsl.JmeterDsl.threadGroup;

/**
 * PERF-TC-03 Spike: baseline users, sudden jump to spike users within ~1s, back to baseline. The system must not
 * error during the spike (looser latency SLO {@code perf.spike.*}) and must recover to normal SLOs afterwards.
 */
public class SpikeTest extends PerfBase {

    @Test(groups = "spike", description = "Sudden traffic spike causes no errors; latency recovers afterwards")
    public void survivesSpikeAndRecovers() throws Exception {
        int baseUsers = i("perf.spike.baseUsers", 3);
        int spikeUsers = i("perf.spike.users", 30);
        Duration baseHold = seconds("perf.spike.baseHoldSeconds", 5);
        Duration spikeHold = seconds("perf.spike.holdSeconds", 8);
        Duration thinkMin = millis("perf.think.minMs", 300);
        Duration thinkMax = millis("perf.think.maxMs", 800);
        int reportPct = i("perf.reportPct", 10);

        TestPlanStats spike = run("spike", base -> threadGroup("spike")
                .rampTo(baseUsers, Duration.ofSeconds(1)).holdFor(baseHold)
                .rampTo(spikeUsers, Duration.ofSeconds(1)).holdFor(spikeHold)
                .rampTo(baseUsers, Duration.ofSeconds(1)).holdFor(baseHold)
                .children(Journey.shopper(base, thinkMin, thinkMax, reportPct)));
        assertThat(Thresholds.breaches(spike, Set.of(), false, "perf.spike"))
                .as("SLO breaches during spike %d -> %d users", baseUsers, spikeUsers).isEmpty();

        TestPlanStats recovery = run("spike-recovery", base -> threadGroup("recovery")
                .rampToAndHold(baseUsers, Duration.ofSeconds(1), seconds("perf.spike.recoverySeconds", 5))
                .children(Journey.shopper(base, thinkMin, thinkMax, reportPct)));
        assertThat(Thresholds.breaches(recovery, Set.of(), false))
                .as("System did not recover to normal SLOs after the spike").isEmpty();
    }
}
