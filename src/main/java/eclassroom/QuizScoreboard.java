package eclassroom;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** In-memory scoring on the teacher client; each student's first answer counts. */
final class QuizScoreboard {
    record Answer(boolean accepted, boolean correct, int points) {}

    private final int[] correctOptions;
    private final int[] points;
    private final int[] optionCounts;
    private final Map<String, Integer> scores = new HashMap<>();
    private final Map<String, Set<Integer>> answered = new HashMap<>();
    private boolean active;

    QuizScoreboard(int[] correctOptions, int[] points, int[] optionCounts) {
        if (correctOptions.length != points.length || points.length != optionCounts.length) {
            throw new IllegalArgumentException("Question arrays must have equal lengths.");
        }
        this.correctOptions = correctOptions.clone();
        this.points = points.clone();
        this.optionCounts = optionCounts.clone();
    }

    void start() {
        scores.clear();
        answered.clear();
        active = true;
    }

    void finish() {
        active = false;
    }

    Answer submit(String name, int page, int option) {
        int index = page - 1;
        if (!active || index < 0 || index >= points.length
                || option < 0 || option >= optionCounts[index]) {
            return new Answer(false, false, 0);
        }
        if (!answered.computeIfAbsent(name, ignored -> new HashSet<>()).add(page)) {
            return new Answer(false, false, 0);
        }
        boolean correct = option == correctOptions[index];
        int earned = correct ? points[index] : 0;
        scores.merge(name, earned, Integer::sum);
        return new Answer(true, correct, earned);
    }

    Map<String, Integer> scores() {
        return Map.copyOf(scores);
    }
}
