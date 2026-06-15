package dev.gois.tools.depviz.version;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class VersionClassifierTest {
    @Test
    void classifiesPatchUpdates() {
        VersionInsight insight = VersionClassifier.classify("6.1.14", "6.1.16");

        assertThat(insight.status()).isEqualTo("outdated");
        assertThat(insight.updateType()).isEqualTo("patch");
    }

    @Test
    void classifiesMinorUpdates() {
        VersionInsight insight = VersionClassifier.classify("6.1.14", "6.2.0");

        assertThat(insight.status()).isEqualTo("outdated");
        assertThat(insight.updateType()).isEqualTo("minor");
    }

    @Test
    void classifiesMajorUpdates() {
        VersionInsight insight = VersionClassifier.classify("6.1.14", "7.0.0");

        assertThat(insight.status()).isEqualTo("outdated");
        assertThat(insight.updateType()).isEqualTo("major");
    }

    @Test
    void treatsSameAndOlderLatestVersionsAsCurrent() {
        VersionInsight same = VersionClassifier.classify("6.1.14", "6.1.14");
        VersionInsight older = VersionClassifier.classify("6.1.14", "6.1.13");

        assertThat(same.status()).isEqualTo("current");
        assertThat(same.updateType()).isEqualTo("none");
        assertThat(older.status()).isEqualTo("current");
        assertThat(older.updateType()).isEqualTo("none");
    }

    @Test
    void classifiesNewerNonSemanticVersionsAsUnknown() {
        VersionInsight insight = VersionClassifier.classify("1.0.0", "1.0.1.Final");

        assertThat(insight.status()).isEqualTo("outdated");
        assertThat(insight.updateType()).isEqualTo("unknown");
    }

    @Test
    void filtersPrereleasesWhenSelectingLatestStableVersion() {
        assertThat(VersionClassifier.isStable("7.0.0-RC1")).isFalse();
        assertThat(VersionClassifier.isStable("7.0.0")).isTrue();
        assertThat(VersionClassifier.latestStable(List.of("7.0.0-RC1", "6.2.0", "6.2.1-SNAPSHOT", "6.3.0")))
            .contains("6.3.0");
        assertThat(VersionClassifier.latestStable(List.of("1.0.0-beta", "1.0.0-SNAPSHOT"))).isEmpty();
    }
}
