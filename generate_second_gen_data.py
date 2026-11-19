import json
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parent
INPUT_PATH = ROOT / "Markov_FirstGenData.json"
OUTPUT_PATH = ROOT / "Markov_SecondGenData.json"


def convert_transitions(data):
    if not isinstance(data, dict):
        raise ValueError("The input JSON must contain an object of word entries.")

    converted = {}
    for word, entry in data.items():
        if not isinstance(entry, dict):
            raise ValueError(f"Entry for {word!r} must be an object.")

        transitions = entry.get("transitions")
        if not isinstance(transitions, list) or not all(
            isinstance(next_word, str) for next_word in transitions
        ):
            raise ValueError(
                f"Transitions for {word!r} must be a list of words."
            )

        counts = Counter(transitions)
        total = len(transitions)
        converted[word] = {
            "transitions": {
                next_word: count / total
                for next_word, count in counts.items()
            } if total else {},
            "form": entry.get("form", ""),
        }

    return converted


def main():
    with INPUT_PATH.open(encoding="utf-8") as source_file:
        source_data = json.load(source_file)

    probability_data = convert_transitions(source_data)

    with OUTPUT_PATH.open("w", encoding="utf-8") as output_file:
        json.dump(probability_data, output_file, ensure_ascii=False, indent=2)
        output_file.write("\n")

    print(f"Saved probabilities for {len(probability_data)} words to {OUTPUT_PATH.name}.")


if __name__ == "__main__":
    main()
