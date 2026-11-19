# Markov-Based-Chat-Bot
A text generating bot that utilizes Markov adjacent algorithms rather than neural networks.

## Building the conversation model

Run `python DataCollector.py --trigrams-only` from the project folder to collect
transcript text and generate `Markov_ThirdGenData.json` without replacing
`Markov_FirstGenData.json`. The CLI prefers this two-word-context model when it
is present; otherwise, it falls back to `Markov_SecondGenData.json`.
