import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs a console chat loop and creates replies from Markov transition data.
 *
 * <p>The preferred data file is a trigram model: it uses the previous two
 * words to choose the next word. If that model is missing, the program falls
 * back to the older one-word transition model.</p>
 */
public class CLI {
    // The older model maps one word to the probabilities of the next word.
    private static final Path DATA_FILE = Path.of("Markov_SecondGenData.json");

    // The newer model maps a two-word context to possible next words.
    private static final Path TRIGRAM_DATA_FILE = Path.of("Markov_ThirdGenData.json");

    // This pattern extracts words (including contractions) and sentence endings.
    private static final Pattern WORD_PATTERN =
            Pattern.compile("[a-zA-Z]+(?:'[a-zA-Z]+)?|[.!?]");

    // Prevent any generated response from growing without bound.
    private static final int MAX_SENTENCE_WORDS = 9;

    // The old model makes several attempts so the best-scoring reply can be chosen.
    private static final int RESPONSE_CANDIDATES = 32;

    // Raising probabilities to this power favors more common training transitions.
    private static final double TRANSITION_POWER = 2.0;

    // These markers let the trigram model learn what words start a sentence.
    private static final String START_MARKER = "<START>";
    private static final String START_CONTEXT = START_MARKER + " " + START_MARKER;

    // Punctuation is stored as a token in the model, just like words are.
    private static final String PERIOD = ".";
    private static final String EXCLAMATION = "!";
    private static final String QUESTION = "?";

    // Common words are ignored when measuring how closely a reply matches a topic.
    private static final Set<String> COMMON_WORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "if", "then", "so",
            "to", "of", "in", "on", "at", "for", "from", "by", "with",
            "about", "as", "is", "am", "are", "was", "were", "be", "been",
            "being", "have", "has", "had", "do", "does", "did", "can",
            "could", "would", "should", "will", "might", "must", "i", "me",
            "my", "mine", "you", "your", "yours", "he", "she", "it", "we",
            "they", "them", "their", "this", "that", "these", "those",
            "what", "when", "where", "why", "how", "think", "like", "tell");

    /**
     * One possible next token and its probability within a transition choice.
     *
     * @param word the word or punctuation token that may follow the context
     * @param probability the relative chance assigned to that next token
     */
    private record Transition(String word, double probability) {
    }

    /**
     * All next-token choices for one word or two-word context.
     *
     * @param transitions maps each possible next token to its probability
     */
    private record WordData(Map<String, Double> transitions) {
    }

    /**
     * Loads a model once, then repeatedly reads user messages and prints replies.
     *
     * @param args command-line arguments (not currently used)
     * @throws IOException if a model file is missing or malformed
     */
    public static void main(String[] args) throws IOException {
        // Start with empty maps; only the selected model needs to be populated.
        Map<String, WordData> wordData = Map.of();
        Map<String, WordData> trigramData = Map.of();
        Path trigramFile = findOptionalDataFile(TRIGRAM_DATA_FILE);
        if (trigramFile != null) {
            // Prefer the model that preserves two-word context.
            trigramData = loadWordData(trigramFile);
            if (!trigramData.containsKey(START_CONTEXT)) {
                throw new IOException(
                        TRIGRAM_DATA_FILE + " is missing its sentence-start transitions.");
            }

            // Load the older model too when available; it can provide fallback candidates.
            Path wordDataFile = findOptionalDataFile(DATA_FILE);
            if (wordDataFile != null) {
                wordData = loadWordData(wordDataFile);
            }
            System.out.println("Using the trigram model.");
        } else {
            wordData = loadWordData(findDataFile(DATA_FILE));
            System.err.println(
                    "Trigram model not found; using the older one-word model. "
                            + "Run DataCollector.py to build it.");
        }

        // The vocabulary tells us whether the user's message contains model words.
        Set<String> vocabulary = trigramData.isEmpty()
                ? wordData.keySet()
                : buildVocabulary(trigramData);

        // try-with-resources closes the keyboard scanner when the chat ends.
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("You: ");
                // End cleanly if input is closed, such as when input is piped in.
                if (!scanner.hasNextLine()) {
                    break;
                }

                String userInput = scanner.nextLine();
                // Typing "exit" is the normal way to leave the conversation.
                if (userInput.equalsIgnoreCase("exit")) {
                    System.out.println("Goodbye!");
                    break;
                }

                // Generate a response from the loaded model and show it to the user.
                String response = createResponse(
                        userInput, wordData, trigramData, vocabulary);
                System.out.println("Bot: " + response);
            }
        }
    }

    /*
     * Find a required model file in the current folder or its parent.
     *
     * <p>Checking both locations supports starting the program from either the
     * project folder or its {@code src} folder.</p>
     *
     * @param fileName the model file to locate
     * @return the path to the model
     * @throws IOException if neither location contains the file
     */
    private static Path findDataFile(Path fileName) throws IOException {
        // Reuse the optional lookup so both methods follow the same path rules.
        Path file = findOptionalDataFile(fileName);
        if (file != null) {
            return file;
        }

        throw new IOException(
                "Could not find " + fileName
                        + ". Run the program from the project folder.");
    }

    /**
     * Look for a model in the current directory and then one directory above.
     *
     * @param fileName the model file to locate
     * @return its path, or {@code null} if it is not present
     */
    private static Path findOptionalDataFile(Path fileName) {
        // First check the working directory used to launch the Java program.
        if (Files.isRegularFile(fileName)) {
            return fileName;
        }

        // The parent lookup supports launching with "src" as the working directory.
        Path parentFile = Path.of("..").resolve(fileName).normalize();
        if (Files.isRegularFile(parentFile)) {
            return parentFile;
        }
        return null;
    }

    /*
     * Parse and validate the JSON structure used by both Markov model files.
     *
     * <p>Each top-level key is either a word or a two-word context. Its value
     * must contain a {@code transitions} object mapping next tokens to numeric
     * probabilities.</p>
     *
     * @param file JSON model file to read
     * @return the parsed model, indexed by word or context
     * @throws IOException if the file cannot be read or has the wrong structure
     */
    private static Map<String, WordData> loadWordData(Path file)
            throws IOException {
        // The parser converts JSON objects into maps and numbers into Doubles.
        Object parsedJson = new JsonParser(Files.readString(file)).parse();
        if (!(parsedJson instanceof Map<?, ?> entries)) {
            throw new IOException("The Markov data JSON must have an object at its root.");
        }

        // Keep insertion order so weighted choices are stable for a given model.
        Map<String, WordData> wordData = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : entries.entrySet()) {
            // Each model entry must have a string key and an object as its value.
            if (!(entry.getKey() instanceof String context)
                    || !(entry.getValue() instanceof Map<?, ?> wordObject)) {
                throw new IOException("The Markov data contains an invalid context entry.");
            }

            // Transitions are the only field the generator needs from each entry.
            Object rawTransitions = wordObject.get("transitions");
            if (!(rawTransitions instanceof Map<?, ?> transitionObject)) {
                throw new IOException(
                        "The Markov data for \"" + context
                                + "\" is missing its transitions object.");
            }

            Map<String, Double> transitions = new LinkedHashMap<>();
            for (Map.Entry<?, ?> transition : transitionObject.entrySet()) {
                // Reject bad weights early rather than silently making a bad model.
                if (!(transition.getKey() instanceof String nextWord)
                        || !(transition.getValue() instanceof Double probability)
                        || !Double.isFinite(probability)
                        || probability < 0.0) {
                    throw new IOException(
                            "The Markov data for \"" + context
                                    + "\" contains an invalid transition.");
                }
                transitions.put(nextWord, probability);
            }

            // Store the validated transitions under their word/context key.
            wordData.put(context, new WordData(transitions));
        }
        return wordData;
    }

    /**
     * Collect every ordinary word appearing in the trigram model.
     *
     * <p>Words can occur in either a context key or as a possible next token.
     * The special sentence-start marker is not a real vocabulary word.</p>
     *
     * @param trigramData loaded trigram model
     * @return the set of words the model knows
     */
    private static Set<String> buildVocabulary(Map<String, WordData> trigramData) {
        Set<String> vocabulary = new HashSet<>();
        for (Map.Entry<String, WordData> context : trigramData.entrySet()) {
            // A context key contains the two words used to predict its next word.
            for (String word : context.getKey().split(" ")) {
                if (!START_MARKER.equals(word)) {
                    vocabulary.add(word);
                }
            }
            // Also include possible next words, which might not yet be a context.
            for (String word : context.getValue().transitions().keySet()) {
                if (!START_MARKER.equals(word)) {
                    vocabulary.add(word);
                }
            }
        }
        return vocabulary;
    }

    /*
     * Generate several sentence-shaped candidates and prefer ones that reuse
     * meaningful words from the user's message. Use two-word context whenever
     * the trigram model is available.
     */
    private static String createResponse(
            String userInput,
            Map<String, WordData> wordData,
            Map<String, WordData> trigramData,
            Set<String> vocabulary) {
        // An empty trigram map means the program loaded only the older model.
        boolean useTrigramModel = !trigramData.isEmpty();
        Set<String> inputWords = new HashSet<>();
        Matcher matcher = WORD_PATTERN.matcher(userInput);
        while (matcher.find()) {
            // Lowercase tokens so user input matches the lowercase training data.
            String word = matcher.group().toLowerCase(Locale.ROOT);
            if (!isPunctuation(word) && vocabulary.contains(word)) {
                inputWords.add(word);
            }
        }

        // Without any known words there is no useful way to connect the reply to the input.
        if (inputWords.isEmpty()) {
            return "I don't know any of those words yet.";
        }

        // Common words ("the", "is", "you", etc.) are weak topic signals.
        Set<String> meaningfulInputWords = new HashSet<>(inputWords);
        meaningfulInputWords.removeAll(COMMON_WORDS);

        List<List<String>> candidates = new ArrayList<>();
        if (!trigramData.isEmpty()) {
            // Prefer continuations that use phrases from the user's message.
            addContextCandidates(userInput, trigramData, candidates);
            // Supplement those with older-model choices and complete sentences.
            addWordCandidates(inputWords, wordData, candidates);
            for (int i = 0; i < 8; i++) {
                candidates.add(generateTrigramSentence(trigramData));
            }
        } else {
            // The one-word model has no sentence-start context, so try many paths.
            for (int i = 0; i < RESPONSE_CANDIDATES; i++) {
                candidates.add(generateForward(PERIOD, wordData));
            }
        }

        String bestResponse = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        int tiedBestResponses = 0;
        for (List<String> candidate : candidates) {
            // A missing transition can make a candidate empty; ignore it.
            if (candidate.isEmpty()) {
                continue;
            }

            double score = scoreResponse(candidate, meaningfulInputWords);
            if (score > bestScore) {
                // A higher score means a stronger topic match and/or better length.
                bestResponse = joinWords(candidate);
                bestScore = score;
                tiedBestResponses = 1;
            } else if (score == bestScore
                    && ThreadLocalRandom.current().nextInt(++tiedBestResponses) == 0) {
                // Randomly select among equal-scoring candidates instead of always
                // favoring whichever happened to be generated first.
                bestResponse = joinWords(candidate);
            }
        }

        if (bestResponse == null) {
            return "I couldn't come up with a response.";
        }
        return capitalizeFirstLetter(bestResponse);
    }

    /*
     * Continue from recent two-word phrases in the user's message. This
     * grounds replies in the prompt instead of always choosing a random
     * sentence opening from the training set.
     */
    private static void addContextCandidates(
            String userInput,
            Map<String, WordData> trigramData,
            List<List<String>> candidates) {
        List<String> words = new ArrayList<>();
        Matcher matcher = WORD_PATTERN.matcher(userInput);
        while (matcher.find()) {
            // Keep punctuation here so we can avoid building contexts across sentence ends.
            words.add(matcher.group().toLowerCase(Locale.ROOT));
        }

        List<String> contexts = new ArrayList<>();
        for (int i = 0; i + 1 < words.size(); i++) {
            // Only adjacent words make a valid trigram context.
            if (isPunctuation(words.get(i)) || isPunctuation(words.get(i + 1))) {
                continue;
            }
            String context = words.get(i) + " " + words.get(i + 1);
            // A context is usable only if the collector observed it in training.
            if (trigramData.containsKey(context)) {
                contexts.add(context);
            }
        }

        // Limit work on long messages by using only the final eight known contexts.
        int firstContext = Math.max(0, contexts.size() - 8);
        for (int i = firstContext; i < contexts.size(); i++) {
            // Make several random continuations for each context for later scoring.
            for (int sample = 0; sample < 3; sample++) {
                candidates.add(generateTrigramContinuation(
                        contexts.get(i), trigramData));
            }
        }
    }

    /* Also consider continuations from known input words in the older model. */
    private static void addWordCandidates(
            Set<String> inputWords,
            Map<String, WordData> wordData,
            List<List<String>> candidates) {
        // The newer model can be used without the optional older data file.
        if (wordData.isEmpty()) {
            return;
        }
        for (String word : inputWords) {
            if (wordData.containsKey(word)) {
                // Try several weighted paths from each known input word.
                for (int sample = 0; sample < 5; sample++) {
                    List<String> candidate = new ArrayList<>();
                    // Include the seed itself because generateForward returns only what follows it.
                    candidate.add(word);
                    candidate.addAll(generateForward(word, wordData));
                    // Add a period if the sampled path did not reach punctuation.
                    if (!isPunctuation(candidate.get(candidate.size() - 1))) {
                        candidate.add(PERIOD);
                    }
                    candidates.add(candidate);
                }
            }
        }
    }

    /*
     * A trigram model predicts the next token from the previous two. The
     * special start context picks a real sentence opening from the training
     * text; following contexts then preserve more local word order.
     */
    private static List<String> generateTrigramSentence(
            Map<String, WordData> trigramData) {
        // The start context is trained on the first word of each source sentence.
        WordData startData = trigramData.get(START_CONTEXT);
        String firstWord = chooseWeightedWord(startData.transitions());
        if (firstWord == null || firstWord.isBlank()) {
            return List.of();
        }
        // Treat the beginning marker as the previous word for the first transition.
        return generateTrigramContinuation(
                START_MARKER + " " + firstWord, trigramData);
    }

    /**
     * Continue generation from an existing two-word context.
     *
     * <p>Words already present in the starting context are marked as used.
     * Later choices that repeat a used word are filtered out, while punctuation
     * remains available so the sentence can still terminate.</p>
     *
     * @param startingContext two words that determine the next-word choices
     * @param trigramData model containing context-to-transition mappings
     * @return the generated sentence tokens, including ending punctuation
     */
    private static List<String> generateTrigramContinuation(
            String startingContext, Map<String, WordData> trigramData) {
        // Split once: the special marker itself contains angle brackets, not spaces.
        String[] contextWords = startingContext.split(" ", 2);
        if (contextWords.length != 2) {
            return List.of();
        }

        String previousWord = contextWords[0];
        String currentWord = contextWords[1];
        List<String> sentence = new ArrayList<>();
        Set<String> usedWords = new HashSet<>();

        // Context-based replies start with both real words; sentence-start replies
        // use the special marker only internally, so it is not printed.
        if (!START_MARKER.equals(previousWord)) {
            sentence.add(previousWord);
            if (!isPunctuation(previousWord)) {
                usedWords.add(previousWord);
            }
        }
        sentence.add(currentWord);
        if (!isPunctuation(currentWord)) {
            usedWords.add(currentWord);
        }

        // Each iteration uses the previous two tokens to choose exactly one next token.
        for (int i = sentence.size(); i < MAX_SENTENCE_WORDS; i++) {
            String context = previousWord + " " + currentWord;
            WordData contextData = trigramData.get(context);
            if (contextData == null) {
                // The training data has no continuation for this pair.
                break;
            }

            // The sampler ignores words already used, but still allows punctuation.
            String nextWord = chooseWeightedWord(
                    contextData.transitions(), usedWords);
            if (nextWord == null || nextWord.isBlank()) {
                // No eligible transition remains (for example, all choices repeat).
                break;
            }

            sentence.add(nextWord);
            if (isPunctuation(nextWord)) {
                // Preserve punctuation and stop immediately at the sentence boundary.
                break;
            }
            // Update the repeat-check set and shift the two-word context forward.
            usedWords.add(nextWord);
            previousWord = currentWord;
            currentWord = nextWord;
        }

        // If the model ran out of context or hit the length limit, close the sentence.
        if (!isPunctuation(sentence.get(sentence.size() - 1))) {
            sentence.add(PERIOD);
        }
        return sentence;
    }

    /*
     * Give extra weight to matching topic words. Prefer a finished, moderate
     * length sentence, while penalizing very short or overly long fragments.
     */
    private static double scoreResponse(
            List<String> words, Set<String> meaningfulInputWords) {
        int wordCount = 0;
        Set<String> matchingWords = new HashSet<>();
        Set<String> uniqueWords = new HashSet<>();
        for (String word : words) {
            // Punctuation ends a sentence but should not count as a vocabulary word.
            if (isPunctuation(word)) {
                continue;
            }
            wordCount++;
            if (meaningfulInputWords.contains(word)) {
                // A set counts each topic word once, avoiding a high score
                // just because one word appears repeatedly.
                matchingWords.add(word);
            }
            uniqueWords.add(word);
        }

        boolean endsWithPunctuation = isPunctuation(words.get(words.size() - 1));
        // More topic matches and a finished sentence improve the score.
        double score = matchingWords.size() * 5.0;
        score += endsWithPunctuation ? 2.0 : -2.0;
        // Penalize very short replies, long replies, and repeated words.
        score -= Math.max(0, 4 - wordCount) * 1.5;
        score -= Math.max(0, wordCount - 8) * 0.5;
        score -= Math.max(0, wordCount - uniqueWords.size()) * 0.75;
        return score;
    }

    /**
     * Capitalize the first letter without changing the rest of the generated text.
     *
     * @param sentence generated response
     * @return the sentence with its first letter capitalized
     */
    private static String capitalizeFirstLetter(String sentence) {
        for (int i = 0; i < sentence.length(); i++) {
            if (Character.isLetter(sentence.charAt(i))) {
                // Leave leading quotes or other characters in their original place.
                return sentence.substring(0, i)
                        + Character.toUpperCase(sentence.charAt(i))
                        + sentence.substring(i + 1);
            }
        }
        return sentence;
    }

    /*
     * Start at a sentence-ending token and follow its outgoing probabilities.
     * Stop at the next punctuation, a missing transition, or the word limit.
     */
    private static List<String> generateForward(
            String startingWord, Map<String, WordData> wordData) {
        List<String> followingWords = new ArrayList<>();
        Set<String> usedWords = new HashSet<>();
        // Include the seed so the chain cannot immediately choose it again.
        if (!isPunctuation(startingWord)) {
            usedWords.add(startingWord);
        }
        String currentWord = startingWord;

        // Follow at most the configured number of transitions.
        for (int i = 0; i < MAX_SENTENCE_WORDS; i++) {
            WordData currentData = wordData.get(currentWord);
            if (currentData == null) {
                // No data exists for the current word, so the chain cannot continue.
                break;
            }

            String nextWord = chooseWeightedWord(
                    currentData.transitions(), usedWords);
            if (nextWord == null || nextWord.isBlank()) {
                break;
            }

            followingWords.add(nextWord);
            if (isPunctuation(nextWord)) {
                // Sentence-ending punctuation is included in the output.
                break;
            }
            // Record the word and use it as the source for the next transition.
            usedWords.add(nextWord);
            currentWord = nextWord;
        }
        return followingWords;
    }

    /*
     * Pick a next word proportionally to its probability. A null result means
     * there are no positive-probability choices.
     */
    private static String chooseWeightedWord(Map<String, Double> choices) {
        // An empty used-word set keeps this overload useful for sentence starts.
        return chooseWeightedWord(choices, Set.of());
    }

    /**
     * Choose a next token proportionally to its probability, excluding used words.
     *
     * <p>Sentence punctuation is exempt from the no-repeat rule. Raising each
     * probability to {@link #TRANSITION_POWER} makes frequent transitions more
     * likely while preserving some randomness.</p>
     *
     * @param choices next-token probabilities for the current context
     * @param usedWords ordinary words already in this sentence
     * @return the sampled word or punctuation, or {@code null} if nothing remains
     */
    private static String chooseWeightedWord(
            Map<String, Double> choices, Set<String> usedWords) {
        List<Transition> weightedChoices = new ArrayList<>();
        for (Map.Entry<String, Double> choice : choices.entrySet()) {
            // Skip a repeated word; punctuation is allowed to end the sentence
            // even if the same punctuation mark appeared earlier.
            if (!isPunctuation(choice.getKey())
                    && usedWords.contains(choice.getKey())) {
                continue;
            }
            // Sharpen the distribution so common, well-supported phrases win more often.
            weightedChoices.add(new Transition(
                    choice.getKey(), Math.pow(choice.getValue(), TRANSITION_POWER)));
        }
        // The helper handles the weighted random draw and empty-choice case.
        return chooseWeightedTransition(weightedChoices);
    }

    /**
     * Draw one item from a list of non-negative weights.
     *
     * <p>Each item's share of the total weight is its chance of being selected.
     * The method returns {@code null} if all choices have zero weight.</p>
     *
     * @param choices words paired with their weights
     * @return the selected word or {@code null} when no positive weight exists
     */
    private static String chooseWeightedTransition(List<Transition> choices) {
        double totalWeight = 0.0;
        for (Transition choice : choices) {
            // The total defines the range from which the random value is drawn.
            totalWeight += choice.probability();
        }
        if (totalWeight <= 0.0) {
            return null;
        }

        double selectedWeight = ThreadLocalRandom.current().nextDouble(totalWeight);
        for (Transition choice : choices) {
            // Subtract each weight until the random value lands inside its interval.
            selectedWeight -= choice.probability();
            if (selectedWeight < 0.0) {
                return choice.word();
            }
        }

        // Floating-point rounding can leave the final choice just below zero.
        return choices.get(choices.size() - 1).word();
    }

    /**
     * Check whether a model token ends a sentence.
     *
     * @param word token to check
     * @return {@code true} for a period, exclamation mark, or question mark
     */
    private static boolean isPunctuation(String word) {
        return PERIOD.equals(word)
                || EXCLAMATION.equals(word)
                || QUESTION.equals(word);
    }

    /*
     * Punctuation tokens in the data stand alone, so attach them to the
     * preceding word instead of adding a space before them.
     *
     * @param words generated word and punctuation tokens
     * @return display-ready sentence text
     */
    private static String joinWords(List<String> words) {
        StringBuilder sentence = new StringBuilder();
        for (String word : words) {
            if (isPunctuation(word)) {
                // Attach punctuation directly to the previous token.
                sentence.append(word);
            } else {
                // Add spaces between words, but not before the first one.
                if (!sentence.isEmpty()) {
                    sentence.append(' ');
                }
                sentence.append(word);
            }
        }
        return sentence.toString();
    }

    /*
     * This small JSON parser handles the JSON types used by the generated data.
     * Keeping it here avoids adding a library dependency just to read the model.
     */
    private static final class JsonParser {
        // The entire JSON file is held in memory while it is parsed.
        private final String json;

        // Index of the next character to read; parser methods advance this value.
        private int position;

        /**
         * Create a parser for one JSON document.
         *
         * @param json full JSON text read from a model file
         */
        private JsonParser(String json) {
            this.json = json;
        }

        /**
         * Parse exactly one JSON value and reject any non-whitespace trailing text.
         *
         * @return the parsed root object
         * @throws IOException if the text is malformed
         */
        private Object parse() throws IOException {
            Object value = parseValue();
            skipWhitespace();
            if (position != json.length()) {
                throw error("Unexpected content after the JSON value");
            }
            return value;
        }

        /**
         * Read one JSON value by looking at its first non-whitespace character.
         *
         * <p>Objects, arrays, and strings have distinctive opening characters.
         * The remaining legal starts represent literals or numbers.</p>
         */
        private Object parseValue() throws IOException {
            skipWhitespace();
            if (position >= json.length()) {
                throw error("Unexpected end of JSON");
            }

            // Delegate each JSON type to a parser that understands its syntax.
            return switch (json.charAt(position)) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> parseNumber();
            };
        }

        /**
         * Parse a JSON object into a map of string keys and parsed values.
         *
         * <p>Object grammar is: opening brace, zero or more key/value pairs
         * separated by commas, then a closing brace.</p>
         */
        private Map<String, Object> parseObject() throws IOException {
            expect('{');
            Map<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (consume('}')) {
                // An immediate closing brace represents an empty object.
                return object;
            }

            while (true) {
                skipWhitespace();
                // JSON requires object keys to be quoted strings.
                if (position >= json.length() || json.charAt(position) != '"') {
                    throw error("Expected a quoted object key");
                }
                String key = parseString();
                skipWhitespace();
                expect(':');
                // parseValue handles nested objects and all other JSON value types.
                object.put(key, parseValue());
                skipWhitespace();
                if (consume('}')) {
                    return object;
                }
                // If the object did not end, another key/value pair must follow.
                expect(',');
            }
        }

        /**
         * Parse a JSON array into a list of parsed values.
         */
        private List<Object> parseArray() throws IOException {
            expect('[');
            List<Object> array = new ArrayList<>();
            skipWhitespace();
            if (consume(']')) {
                // An immediate closing bracket represents an empty array.
                return array;
            }

            while (true) {
                // Values may be any JSON type, including nested arrays or objects.
                array.add(parseValue());
                skipWhitespace();
                if (consume(']')) {
                    return array;
                }
                // More values must be comma-separated.
                expect(',');
            }
        }

        /**
         * Parse a quoted JSON string, decoding its backslash escapes.
         */
        private String parseString() throws IOException {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (position < json.length()) {
                char character = json.charAt(position++);
                if (character == '"') {
                    return value.toString();
                }
                if (character == '\\') {
                    if (position >= json.length()) {
                        throw error("Unfinished escape sequence");
                    }
                    char escaped = json.charAt(position++);
                    // JSON defines these short escapes plus four-digit Unicode escapes.
                    switch (escaped) {
                        case '"', '\\', '/' -> value.append(escaped);
                        case 'b' -> value.append('\b');
                        case 'f' -> value.append('\f');
                        case 'n' -> value.append('\n');
                        case 'r' -> value.append('\r');
                        case 't' -> value.append('\t');
                        case 'u' -> value.append(parseUnicodeEscape());
                        default -> throw error("Invalid escape sequence");
                    }
                } else if (character < 0x20) {
                    throw error("Unescaped control character in string");
                } else {
                    value.append(character);
                }
            }
            throw error("Unterminated string");
        }

        /**
         * Decode the four hexadecimal digits after a JSON {@code \\u} escape.
         */
        private char parseUnicodeEscape() throws IOException {
            if (position + 4 > json.length()) {
                throw error("Incomplete Unicode escape");
            }
            String hexDigits = json.substring(position, position + 4);
            try {
                char decoded = (char) Integer.parseInt(hexDigits, 16);
                // Advance only after confirming the digits are valid hexadecimal.
                position += 4;
                return decoded;
            } catch (NumberFormatException exception) {
                throw error("Invalid Unicode escape");
            }
        }

        /**
         * Parse a JSON number, including optional fraction and exponent parts.
         */
        private Double parseNumber() throws IOException {
            int start = position;
            // A number can start with a minus sign; the integer part follows it.
            consume('-');
            if (!consume('0')) {
                consumeDigits();
            }
            // The fraction is optional, but if present it must contain digits.
            if (consume('.')) {
                consumeDigits();
            }
            // An exponent may use either e or E and may have an optional sign.
            if (consume('e') || consume('E')) {
                if (!consume('+')) {
                    consume('-');
                }
                consumeDigits();
            }

            if (position == start) {
                throw error("Expected a JSON value");
            }
            try {
                return Double.parseDouble(json.substring(start, position));
            } catch (NumberFormatException exception) {
                throw error("Invalid number");
            }
        }

        /**
         * Advance over one or more decimal digits; fail if there are none.
         */
        private void consumeDigits() throws IOException {
            int start = position;
            while (position < json.length()
                    && Character.isDigit(json.charAt(position))) {
                position++;
            }
            if (position == start) {
                throw error("Expected a digit");
            }
        }

        /**
         * Read a fixed JSON literal such as {@code true}, {@code false}, or {@code null}.
         *
         * @param literal expected characters at the current position
         * @param value Java value to return when they match
         * @return the supplied Java value
         * @throws IOException if the document does not contain the expected literal
         */
        private Object parseLiteral(String literal, Object value)
                throws IOException {
            if (!json.startsWith(literal, position)) {
                throw error("Invalid JSON literal");
            }
            position += literal.length();
            return value;
        }

        /**
         * Require a specific punctuation character, allowing whitespace before it.
         */
        private void expect(char expected) throws IOException {
            skipWhitespace();
            if (!consume(expected)) {
                throw error("Expected '" + expected + "'");
            }
        }

        /**
         * Consume a character only if it is exactly at the current position.
         *
         * @param expected character to look for
         * @return {@code true} and advance on a match; otherwise {@code false}
         */
        private boolean consume(char expected) {
            if (position < json.length() && json.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        /** Advance past spaces, tabs, and line breaks between JSON tokens. */
        private void skipWhitespace() {
            while (position < json.length()
                    && Character.isWhitespace(json.charAt(position))) {
                position++;
            }
        }

        /**
         * Create a readable parse error that points to its character offset.
         *
         * @param message what went wrong
         * @return an exception ready to throw
         */
        private IOException error(String message) {
            return new IOException(
                    message + " at character " + position + " in Markov data JSON");
        }
    }
}
