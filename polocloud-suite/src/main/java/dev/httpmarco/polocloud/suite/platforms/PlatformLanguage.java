package dev.httpmarco.polocloud.suite.platforms;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public enum PlatformLanguage {

    JAVA(".jar"),
    GO("");

    private final String serviceFileExtension;

}
