package dev.httpmarco.polocloud.node.terminal.setup;

import dev.httpmarco.polocloud.node.utils.Pair;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public final class SetupQuestion {

    private String answerKey;
    private String question;
    private final Function<Map<String, String>, List<String>> possibleAnswers;
    private @Nullable Function<Pair<String, Map<String, String>>, Boolean> predicate;

}
