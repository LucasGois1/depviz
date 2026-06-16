package io.github.lucasgois1.depviz.version;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VersionSummaryTest {
    @Test
    void disabledReturnsZeroedDisabledSummary() {
        VersionSummary summary = VersionSummary.disabled();

        assertThat(summary.enabled()).isFalse();
        assertThat(summary.checked()).isZero();
        assertThat(summary.current()).isZero();
        assertThat(summary.outdated()).isZero();
        assertThat(summary.patch()).isZero();
        assertThat(summary.minor()).isZero();
        assertThat(summary.major()).isZero();
        assertThat(summary.unknown()).isZero();
        assertThat(summary.unavailable()).isZero();
    }
}
