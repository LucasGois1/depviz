package io.github.lucasgois1.depviz.version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class VersionClassifierTest {
    @Test
    void classifiesPatchUpdates() {
        VersionInsight insight = VersionClassifier.classify("6.1.14", "6.1.16");

        assertThat(insight.status()).isEqualTo("outdated");
        assertThat(insight.updateType()).isEqualTo("patch");
        assertThat(insight.message()).isNull();
    }

    @Test
    void classifiesMinorUpdates() {
        VersionInsight insight = VersionClassifier.classify("6.1.14", "6.2.0");

        assertThat(insight.status()).isEqualTo("outdated");
        assertThat(insight.updateType()).isEqualTo("minor");
        assertThat(insight.message()).isNull();
    }

    @Test
    void classifiesMajorUpdates() {
        VersionInsight insight = VersionClassifier.classify("6.1.14", "7.0.0");

        assertThat(insight.status()).isEqualTo("outdated");
        assertThat(insight.updateType()).isEqualTo("major");
        assertThat(insight.message()).isNull();
    }

    @Test
    void treatsSameAndOlderLatestVersionsAsCurrent() {
        VersionInsight same = VersionClassifier.classify("6.1.14", "6.1.14");
        VersionInsight older = VersionClassifier.classify("6.1.14", "6.1.13");

        assertThat(same.status()).isEqualTo("current");
        assertThat(same.updateType()).isEqualTo("none");
        assertThat(same.message()).isNull();
        assertThat(older.status()).isEqualTo("current");
        assertThat(older.updateType()).isEqualTo("none");
        assertThat(older.message()).isNull();
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
        assertThat(VersionClassifier.isStable("1.0.0rc1")).isFalse();
        assertThat(VersionClassifier.isStable("1.0.0RC1")).isFalse();
        assertThat(VersionClassifier.isStable("1.0M1")).isFalse();
        assertThat(VersionClassifier.isStable("1.0.0M1")).isFalse();
        assertThat(VersionClassifier.isStable("2.0.0preview")).isFalse();
        assertThat(VersionClassifier.isStable("1.0.0-beta")).isFalse();
        assertThat(VersionClassifier.isStable("1.0.0-SNAPSHOT")).isFalse();
        assertThat(VersionClassifier.isStable("1.5.22.RELEASE")).isTrue();
        assertThat(VersionClassifier.isStable("1.5.22.Final")).isTrue();
        assertThat(VersionClassifier.isStable("7.0.0")).isTrue();
        assertThat(VersionClassifier.latestStable(List.of("7.0.0-RC1", "6.2.0", "6.2.1-SNAPSHOT", "6.3.0")))
            .contains("6.3.0");
        assertThat(VersionClassifier.latestStable(List.of("1.5.22.RELEASE"))).contains("1.5.22.RELEASE");
        assertThat(VersionClassifier.latestStable(List.of("1.0.0-beta", "1.0.0-SNAPSHOT"))).isEmpty();
    }

    @Test
    void rejectsBlankOrMissingVersions() {
        assertThatThrownBy(() -> VersionClassifier.classify(null, "1.0.0"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Current version must not be blank");
        assertThatThrownBy(() -> VersionClassifier.classify("1.0.0", " "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Latest version must not be blank");
    }
}
