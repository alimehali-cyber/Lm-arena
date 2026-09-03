#!/bin/bash
REPO=/home/user/Lm-arena
KT_HOME=/usr/local/lib/node_modules/kotlin-compiler
JAVA_HOME=$(python3 -c 'import jdk4py, os; print(os.path.dirname(jdk4py.__file__))')/java-runtime
export JAVA_HOME PATH=$KT_HOME/bin:$JAVA_HOME/bin:$PATH
TESTFILE="$1"; shift
OUT=$(mktemp -d); HARNESS=$REPO/tools/kotlin-harness/src
MAIN=$REPO/app/src/main/java
declare -A SRCS=(); SRCS[$TESTFILE]=1
for extra in "$@"; do SRCS[$extra]=1; done
BLOCKERS=""
kotlinc "$HARNESS"/org/junit/Test.kt "$HARNESS"/org/junit/Assert.kt "$HARNESS"/runner/Main.kt -d "$OUT/classes" 2>/dev/null
for round in 1 2 3 4 5 6 7 8 9 10; do
  FILES=$(printf '%s\n' "${!SRCS[@]}")
  kotlinc "$HARNESS"/org/junit/Test.kt "$HARNESS"/org/junit/Assert.kt "$HARNESS"/androidx/compose/ui/geometry/Offset.kt $FILES -d "$OUT/classes" 2>"$OUT/err.txt"
  UNRES=$(grep -E "error: unresolved reference" "$OUT/err.txt" | sed "s/.*unresolved reference '\([A-Za-z0-9_]*\)'.*/\1/" | sort -u)
  [ -z "$UNRES" ] && break
  added=0
  for sym in $UNRES; do
    hit=$(grep -rlE "(class|object|interface|enum class)[ ]+$sym\b" $MAIN --include='*.kt' | head -3)
    if [ -z "$hit" ]; then hit=$(grep -rlE "fun +[A-Za-z0-9_.<>]*\.$sym\b|^fun $sym\b|^fun <[^>]*> $sym\b" $MAIN --include='*.kt' | head -2); fi
    [ -z "$hit" ] && continue
    for f in $hit; do
      [ -n "${SRCS[$f]:-}" ] && continue
      if grep -qE "^import (android|androidx)" "$f"; then BLOCKERS="$BLOCKERS android-dep:$(basename $f)"; continue; fi
      SRCS[$f]=1; added=1
    done
  done
  [ $added -eq 0 ] && break
done
echo "--- $(basename $TESTFILE): ${#SRCS[@]} source files; blockers: $(echo $BLOCKERS | tr -s ' ' | cut -c1-200)"
if grep -q "error:" "$OUT/err.txt"; then
  echo "CANNOT COMPILE:"; grep "error:" "$OUT/err.txt" | head -6; exit 3
fi
CLS=$(cd "$OUT/classes" && find . -name "$(basename ${TESTFILE%.kt}).class" ! -name '*$*' | sed -e 's#^\./##; s#/#.#g; s#\.class$##')
java -cp "$OUT/classes:$KT_HOME/lib/kotlin-stdlib.jar" runner.MainKt "$CLS"
