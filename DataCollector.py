import json
import re
import time
import trafilatura

links = [
    "https://www.gutenberg.org/cache/epub/1524/pg1524.txt",          # 1. Gutenberg (Hamlet)
    "https://en.wikipedia.org/wiki/Markov_chain",                    # 2. Wikipedia
    "https://avalon.law.yale.edu/19th_century/gettyb.asp",           # 3. Yale Avalon
    "https://classics.mit.edu/Sophocles/antigone.pl.txt",            # 4. MIT Classics
    "https://paulgraham.com/makersschedule.html",                    # 5. Paul Graham
    "https://www.archives.gov/founding-docs/declaration-transcript", # 6. National Archives
    "https://www.gutenberg.org/cache/epub/1533/pg1533.txt",          # 7. Gutenberg (Macbeth)
    "https://en.wikipedia.org/wiki/Artificial_intelligence",         # 8. Wikipedia
    "https://avalon.law.yale.edu/20th_century/kennedy.asp",          # 9. Yale Avalon
    "https://classics.mit.edu/Plato/republic.2.i.html",              # 10. MIT Classics
    "https://paulgraham.com/avg.html",                               # 11. Paul Graham
    "https://www.archives.gov/founding-docs/constitution-transcript",# 12. National Archives
    "https://www.gutenberg.org/cache/epub/844/pg844.txt",            # 13. Gutenberg (Earnest)
    "https://en.wikipedia.org/wiki/Stoicism",                        # 14. Wikipedia
    "https://avalon.law.yale.edu/20th_century/froos1.asp",           # 15. Yale Avalon
    "https://www.gutenberg.org/cache/epub/2542/pg2542.txt",          # 16. Gutenberg (A Doll's House)
    "https://en.wikipedia.org/wiki/History_of_the_Internet",         # 17. Wikipedia
    "https://avalon.law.yale.edu/18th_century/washing.asp",          # 18. Yale Avalon
    "https://www.gutenberg.org/cache/epub/3748/pg3748.txt",          # 19. Gutenberg (Pygmalion)
    "https://en.wikipedia.org/wiki/Game_theory",                     # 20. Wikipedia
    "https://avalon.law.yale.edu/19th_century/lincoln2.asp",         # 21. Yale Avalon
    "https://www.gutenberg.org/cache/epub/16328/pg16328.txt",        # 22. Gutenberg (Cyrano)
    "https://en.wikipedia.org/wiki/Cognitive_bias",                  # 23. Wikipedia
    "https://www.gutenberg.org/cache/epub/11/pg11.txt",              # 24. Gutenberg (Alice)
    "https://en.wikipedia.org/wiki/Distributed_computing",           # 25. Wikipedia
    "https://www.gutenberg.org/cache/epub/12/pg12.txt",              # 26. Gutenberg (Looking-Glass)
    "https://en.wikipedia.org/wiki/Cryptography",                    # 27. Wikipedia
    "https://www.gutenberg.org/cache/epub/16643/pg16643.txt",        # 28. Gutenberg (Emerson)
    "https://en.wikipedia.org/wiki/Evolutionary_biology",            # 29. Wikipedia
    "https://www.gutenberg.org/cache/epub/3600/pg3600.txt",          # 30. Gutenberg (Montaigne)
    "https://en.wikipedia.org/wiki/Thermodynamics",                  # 31. Wikipedia
    "https://www.gutenberg.org/cache/epub/2680/pg2680.txt",          # 32. Gutenberg (Meditations)
    "https://www.gutenberg.org/cache/epub/10615/pg10615.txt",        # 33. Gutenberg (Hume)
    "https://www.gutenberg.org/cache/epub/5827/pg5827.txt",          # 34. Gutenberg (Russell)
    "https://www.gutenberg.org/cache/epub/205/pg205.txt",            # 35. Gutenberg (Walden)
    "https://www.gutenberg.org/cache/epub/1232/pg1232.txt",          # 36. Gutenberg (The Prince)
    "https://www.gutenberg.org/cache/epub/132/pg132.txt",            # 37. Gutenberg (Art of War)
    "https://www.gutenberg.org/cache/epub/2376/pg2376.txt",          # 38. Gutenberg (Up from Slavery)
    "https://www.gutenberg.org/cache/epub/23/pg23.txt",              # 39. Gutenberg (Douglass)
    "https://www.gutenberg.org/cache/epub/147/pg147.txt",            # 40. Gutenberg (Common Sense)
    "https://www.gutenberg.org/cache/epub/1661/pg1661.txt",          # 41. Gutenberg (Sherlock Holmes)
    "https://www.gutenberg.org/cache/epub/35/pg35.txt",              # 42. Gutenberg (Time Machine)
    "https://www.gutenberg.org/cache/epub/36/pg36.txt",              # 43. Gutenberg (War of the Worlds)
    "https://www.gutenberg.org/cache/epub/84/pg84.txt",              # 44. Gutenberg (Frankenstein)
    "https://www.gutenberg.org/cache/epub/345/pg345.txt",            # 45. Gutenberg (Dracula)
    "https://www.gutenberg.org/cache/epub/43/pg43.txt",              # 46. Gutenberg (Jekyll & Hyde)
    "https://www.gutenberg.org/cache/epub/174/pg174.txt",            # 47. Gutenberg (Dorian Gray)
    "https://www.gutenberg.org/cache/epub/215/pg215.txt",            # 48. Gutenberg (Call of the Wild)
    "https://www.gutenberg.org/cache/epub/120/pg120.txt",            # 49. Gutenberg (Treasure Island)
    "https://www.gutenberg.org/cache/epub/2701/pg2701.txt",          # 50. Gutenberg (Moby-Dick)
]

# Initialize the master dictionary to hold all word transitions
markov_data = {}

for link in links:
  downloaded = trafilatura.fetch_url(link)
  if not downloaded:
    continue

  text = trafilatura.extract(downloaded)
  if not text:
    continue

  # Tokenize the text
  # Extracts the words (now tokens) using \w+ and counts ., ?, and ! as words too
  tokens = re.findall(r"\w+|[.!?]", text)

  # Build the transitions from the stream of tokens
  for i in range(len(tokens) - 1):
    current_word = tokens[i].lower()
    next_word = tokens[i + 1].lower()

    # make sure the current word exists in the dictionary
    if current_word not in markov_data:
      markov_data[current_word] = {"transitions": [], "form": ""}

    # append the next word to the transitions list
    markov_data[current_word]["transitions"].append(next_word)

  # Handle the end of the webpage, treat the final word as transitioning to a space
  if tokens:
    last_word = tokens[-1].lower()
    if last_word not in markov_data:
      markov_data[last_word] = {"transitions": [], "form": ""}
    markov_data[last_word]["transitions"].append(" ")

  # time delay to avoid website bot detection
  time.sleep(5)

# export as a JSON file
# Write the fully populated dictionary to a JSON file formatted with indentations for readability
with open("Markov_FirstGenData.json", "w", encoding="utf-8") as f:
  json.dump(markov_data, f, indent=4)