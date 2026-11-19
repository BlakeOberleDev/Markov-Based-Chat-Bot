import argparse
import json
import re
import time
import trafilatura

parser = argparse.ArgumentParser()
parser.add_argument(
    "--trigrams-only",
    action="store_true",
    help="Build the trigram model without replacing Markov_FirstGenData.json.",
)
args = parser.parse_args()

links = [
  "https://www.thisamericanlife.org/800/transcript",                                      # 1. This American Life (Storytelling & interviews)
  "https://imsdb.com/scripts/Pulp-Fiction.html",                                          # 2. IMSDB (Sharp conversational dialogue)
  "https://www.npr.org/transcripts/1197954421",                                           # 3. NPR Fresh Air / Podcast transcript
  "https://waitbutwhy.com/2014/05/life-weeks.html",                                       # 4. Wait But Why (Casual conversational blog)
  "https://www.theverge.com/2023/10/24/23929732/decoder-nilay-patel-interview-transcript", # 5. Decoder Podcast transcript
  "https://www.thisamericanlife.org/795/transcript",                                      # 6. This American Life
  "https://imsdb.com/scripts/Good-Will-Hunting.html",                                     # 7. IMSDB (Natural back-and-forth banter)
  "https://www.npr.org/transcripts/1197958234",                                           # 8. NPR Transcript
  "https://www.askamanager.org/2023/11/update-the-new-hire-who-refused-to-do-any-work.html", # 9. Ask A Manager (Everyday workplace stories/comments)
  "https://tim.blog/2018/09/20/the-tim-ferriss-show-transcripts-naval-ravikant/",         # 10. Tim Ferriss Show (Long-form conversation)
  "https://www.thisamericanlife.org/780/transcript",                                      # 11. This American Life
  "https://imsdb.com/scripts/The-Social-Network.html",                                    # 12. IMSDB (Fast-paced modern dialogue)
  "https://www.npr.org/transcripts/1197956389",                                           # 13. NPR Transcript
  "https://waitbutwhy.com/2013/12/how-to-beat-procrastination.html",                      # 14. Wait But Why
  "https://lexfridman.com/john-carmack-transcript/",                                      # 15. Lex Fridman Podcast (Casual deep-dive chat)
  "https://www.thisamericanlife.org/750/transcript",                                      # 16. This American Life
  "https://imsdb.com/scripts/Superbad.html",                                              # 17. IMSDB (Casual teen/friend banter)
  "https://www.npr.org/transcripts/1197959012",                                           # 18. NPR Transcript
  "https://www.askamanager.org/2022/07/my-coworker-keeps-stealing-my-lunch-and-blaming-the-ghost.html", # 19. Ask A Manager
  "https://tim.blog/2020/05/07/hugh-jackman-transcript/",                                 # 20. Tim Ferriss Show
  "https://www.thisamericanlife.org/700/transcript",                                      # 21. This American Life
  "https://imsdb.com/scripts/When-Harry-Met-Sally.html",                                  # 22. IMSDB (Pure conversational dialogue)
  "https://www.npr.org/transcripts/1197960145",                                           # 23. NPR Transcript
  "https://waitbutwhy.com/2015/01/artificial-intelligence-revolution-1.html",             # 24. Wait But Why
  "https://lexfridman.com/george-hotz-3-transcript/",                                     # 25. Lex Fridman Podcast
  "https://www.thisamericanlife.org/650/transcript",                                      # 26. This American Life
  "https://imsdb.com/scripts/The-Big-Lebowski.html",                                      # 27. IMSDB (Laid-back colloquial speech)
  "https://www.npr.org/transcripts/1197961278",                                           # 28. NPR Transcript
  "https://www.askamanager.org/2021/10/my-employee-wasnt-actually-doing-her-job-for-two-years.html", # 29. Ask A Manager
  "https://tim.blog/2019/10/03/neil-gaiman-transcript/",                                  # 30. Tim Ferriss Show
  "https://www.thisamericanlife.org/600/transcript",                                      # 31. This American Life
  "https://imsdb.com/scripts/Juno.html",                                                  # 32. IMSDB (Quirky casual dialogue)
  "https://www.npr.org/transcripts/1197962411",                                           # 33. NPR Transcript
  "https://waitbutwhy.com/2014/10/religion-for-the-nonreligious.html",                    # 34. Wait But Why
  "https://lexfridman.com/magnus-carlsen-transcript/",                                    # 35. Lex Fridman Podcast
  "https://www.thisamericanlife.org/550/transcript",                                      # 36. This American Life
  "https://imsdb.com/scripts/Groundhog-Day.html",                                         # 37. IMSDB (Everyday conversational reactions)
  "https://www.npr.org/transcripts/1197963544",                                           # 38. NPR Transcript
  "https://www.askamanager.org/2019/09/my-boss-wants-me-to-lie-about-why-im-leaving.html", # 39. Ask A Manager
  "https://tim.blog/2017/05/07/terry-crews-transcript/",                                  # 40. Tim Ferriss Show
  "https://www.thisamericanlife.org/500/transcript",                                      # 41. This American Life
  "https://imsdb.com/scripts/Toy-Story.html",                                             # 42. IMSDB (Clean, punchy dialogue)
  "https://www.npr.org/transcripts/1197964677",                                           # 43. NPR Transcript
  "https://waitbutwhy.com/2016/03/cryonics.html",                                         # 44. Wait But Why
  "https://lexfridman.com/huberman-lab-transcript/",                                      # 45. Lex Fridman Podcast
  "https://www.thisamericanlife.org/450/transcript",                                      # 46. This American Life
  "https://imsdb.com/scripts/Knives-Out.html",                                            # 47. IMSDB (Modern ensemble banter)
  "https://www.npr.org/transcripts/1197965810",                                           # 48. NPR Transcript
  "https://www.askamanager.org/2020/04/my-coworker-is-faking-working-from-home.html",     # 49. Ask A Manager
  "https://tim.blog/2016/12/20/jocko-willink-transcript/",                                # 50. Tim Ferriss Show
]

# Common boilerplate phrases found at the start/end of transcripts & blogs
BOILERPLATE_PATTERNS = re.compile(
    r"(subscribe|newsletter|copyright|all rights reserved|transcript provided|"
    r"click here|read more|follow us|advertisement|sponsored by|cookie policy)",
    re.IGNORECASE,
)

# Common abbreviations whose periods shouldn't end a sentence
ABBREVIATIONS = re.compile(
    r"\b(mr|mrs|ms|dr|prof|sr|jr|st|vs|etc|eg|ie|us|uk)\.",
    re.IGNORECASE,
)


def clean_transcript_text(raw_text):
    # 1. Normalize curly apostrophes/quotes so contractions stay together
    text = raw_text.replace("’", "'").replace("‘", "'")

    # 2. Strip bracketed stage directions / audio cues like [laughter] or (applause)
    text = re.sub(r"\[[^\]]*\]|\([^)]*\)", " ", text)

    # 3. Strip URLs and email addresses
    text = re.sub(r"https?://\S+|www\.\S+|\S+@\S+", " ", text)

    # 4. Remove periods from common abbreviations (Mr. -> Mr) so they don't fake sentence ends
    text = ABBREVIATIONS.sub(r"\1", text)

    cleaned_lines = []
    for line in text.splitlines():
        line = line.strip()
        if not line:
            continue

        # 5. Skip boilerplate/footer lines
        if BOILERPLATE_PATTERNS.search(line):
            continue

        # 6. Strip speaker prefixes at the start of lines (e.g., "NILAY PATEL:" or "Host:")
        line = re.sub(r"^[A-Za-z][A-Za-z\s\.\-]{1,25}:\s*", "", line)

        # 7. Skip standalone ALL-CAPS headers or screenplay character cues (e.g., "ACT I", "JULES")
        if line.isupper() and len(line.split()) <= 5:
            continue

        # 8. Skip very short non-sentence lines (titles, dates, bylines without punctuation)
        if len(line.split()) < 3 and not line.endswith((".", "!", "?")):
            continue

        cleaned_lines.append(line)

    joined = " ".join(cleaned_lines)

    # 9. Collapse repeated punctuation ("..." or "?!") into a single terminator
    joined = re.sub(r"([.!?])+", r"\1", joined)

    return joined


markov_data = {}
trigram_counts = {}


def add_trigram_transition(context, next_word):
    if context not in trigram_counts:
        trigram_counts[context] = {}
    trigram_counts[context][next_word] = (
        trigram_counts[context].get(next_word, 0) + 1
    )


for link in links:
    downloaded = trafilatura.fetch_url(link)
    if not downloaded:
        continue

    # favor_precision=True cuts out extra navbars/sidebars; no_fallback=True avoids scraping messy raw HTML
    text = trafilatura.extract(
        downloaded,
        include_comments=False,
        include_tables=False,
        favor_precision=True,
    )
    if not text:
        continue

    cleaned = clean_transcript_text(text)

    # Tokenize: letters-only words (keeping internal apostrophes like don't) + sentence enders
    tokens = re.findall(r"[a-zA-Z]+(?:'[a-zA-Z]+)?|[.!?]", cleaned)

    for i in range(len(tokens) - 1):
        current_word = tokens[i].lower()
        next_word = tokens[i + 1].lower()

        if current_word not in markov_data:
            markov_data[current_word] = {"transitions": [], "form": ""}

        markov_data[current_word]["transitions"].append(next_word)

    # Store the next token for each two-word context; this preserves enough
    # local word order for the bot to generate more coherent phrases.
    for i in range(len(tokens) - 2):
        first_word = tokens[i].lower()
        second_word = tokens[i + 1].lower()
        next_word = tokens[i + 2].lower()
        add_trigram_transition(f"{first_word} {second_word}", next_word)

    # Give the generator a learned way to begin at actual sentence starts.
    for i, token in enumerate(tokens):
        if i == 0 or tokens[i - 1] in {".", "!", "?"}:
            first_word = token.lower()
            add_trigram_transition("<START> <START>", first_word)
            if i + 1 < len(tokens):
                add_trigram_transition(
                    f"<START> {first_word}", tokens[i + 1].lower()
                )

    if tokens:
        last_word = tokens[-1].lower()
        if last_word not in markov_data:
            markov_data[last_word] = {"transitions": [], "form": ""}
        markov_data[last_word]["transitions"].append(" ")

    time.sleep(5)

if not args.trigrams_only:
    with open("Markov_FirstGenData.json", "w", encoding="utf-8") as f:
        json.dump(markov_data, f, indent=4)

trigram_data = {}
for context, transitions in trigram_counts.items():
    total = sum(transitions.values())
    trigram_data[context] = {
        "transitions": {
            next_word: count / total
            for next_word, count in transitions.items()
        },
        "form": "",
    }

with open("Markov_ThirdGenData.json", "w", encoding="utf-8") as f:
    json.dump(trigram_data, f, indent=2)
