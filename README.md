# DoubleJump – podwójny skok dla Minecraft Java 1.21.11

Projekt ma **dwie niezależne części** (osobne budowania Gradle):

| Katalog | Co to jest | Gdzie działa |
|---|---|---|
| `client-fabric/` | Mod Fabric (tylko klient): podwójny skok, menu ustawień, cząsteczki, dźwięk | komputer gracza |
| `server-paper/` | Plugin Paper 1.21.11: ogłasza obsługę skoku, waliduje zgłoszenia, uprawnienia | serwer multiplayer |

## Jak to działa

**Singleplayer / świat lokalny** – mod działa sam, bez niczego dodatkowego.

**Multiplayer** – mod domyślnie **nie wykonuje żadnego dodatkowego ruchu**. Włącza się dopiero, gdy serwer
(plugin `DoubleJump`) wyśle pakiet `doublejump:hello` z informacją „obsługuję podwójny skok”. Na serwerze
vanilla lub bez pluginu mod pozostaje bezczynny (bez błędów i bez crashy). Klient respektuje limity serwera
(maksymalna siła, minimalny czas w powietrzu). Każdy skok jest zgłaszany serwerowi
(`doublejump:jump`), a plugin sprawdza m.in.: uprawnienie `doublejump.use`, świat, stan gracza,
czy gracz jest w powietrzu, czy skok nie został już użyty, cooldown i minimalny czas w powietrzu.
Plugin **niczego nie omija** – nie rusza antycheata ani innych pluginów. Jeśli używasz antycheata,
skonfiguruj go wg jego dokumentacji tak, aby tolerował jeden skok w powietrzu.

### Zasady skoku (klient)
1. Normalny skok, potem **drugi skok po ponownym naciśnięciu** klawisza (samo przytrzymanie nie liczy się).
2. Po użyciu drugiego skoku trzeba dotknąć ziemi, aby go odnowić (maks. 1 skok w powietrzu).
3. Drugi skok ustawia prędkość pionową na wartość „siły” (poziomy pęd zostaje).
4. Cząsteczki (chmurki) i dźwięk przy aktywacji – obie rzeczy można wyłączyć.
5. Nie działa w wodzie i lawie, na drabinie/pnączach/rusztowaniu, w trybie widza, podczas latania w trybie
   kreatywnym, na elytrach, w pojeździe i podczas snu. Woda/drabina odnawiają skok.
6. **Ochrona przed krawędzią:** drugi skok jest możliwy dopiero po ≥ 4 tickach (0,2 s) w powietrzu,
   a naciśnięcie wcześniej jest ignorowane (nie jest buforowane). Dzięki temu spóźnione naciśnięcie
   po zejściu z krawędzi nie zużywa skoku.
7. Obrażenia od upadku działają normalnie (serwer liczy je po swojej stronie). Dlatego siła jest ograniczona
   do 0,20–0,80 (domyślnie 0,42 – tyle co zwykły skok).

### Klawisze i język
* **Klawisz aktywacji**: domyślnie nieprzypisany = używany jest *klawisz skoku z ustawień gry* (domyślnie SPACJA).
  Własny klawisz możesz przypisać w *Opcje → Sterowanie → Klawisze → DoubleJump*.
  (Nie wiążemy własnego klawisza ze SPACJĄ, bo Minecraft pozwala jednemu klawiszowi uruchamiać tylko
  jedną akcję i mod mógłby „ukraść” SPACJĘ zwykłemu skokowi.)
* **Ustawienia moda**: klawisz *Otwórz ustawienia DoubleJump* (domyślnie **J**) w trakcie gry.
  Menu: włącz/wyłącz mod, suwak siły, cząsteczki, dźwięk. Zapis do `config/doublejump.json` (przetrwa restart).
* **Język**: domyślnie polski (`pl_pl.json`; `en_us.json` – język zapasowy Minecrafta – też zawiera polskie
  teksty, więc polski jest wersją domyślną dla każdego języka gry).

## Wymagania
* JDK **21** (do budowania) i Java 21 (do gry)
* Gradle **9.2+** (patrz niżej)
* Minecraft Java **1.21.11**, Fabric Loader **≥ 0.18.1**, Fabric API **0.141.x+1.21.11**
* Serwer: Paper **1.21.11**

## Budowanie JAR-ów

Paczka nie zawiera `gradle-wrapper.jar` (plik binarny), więc użyj zainstalowanego Gradle 9.2+
albo wygeneruj wrapper raz:

```bash
cd client-fabric
gradle wrapper --gradle-version 9.2.1     # jednorazowo (wymaga zainstalowanego Gradle)
./gradlew build                            # Windows: gradlew.bat build
```

(Albo bez wrappera: `gradle build`. Albo otwórz katalog w IntelliJ IDEA – zaimportuje projekt sam.)

**Gotowy plik moda:** `client-fabric/build/libs/doublejump-fabric-1.0.0.jar`
(pomiń `…-sources.jar`).

Plugin serwerowy:

```bash
cd server-paper
gradle build           # lub ./gradlew build po wygenerowaniu wrappera
```

**Gotowy plik pluginu:** `server-paper/build/libs/DoubleJump-Paper-1.0.0.jar`

Uruchomienie klienta w środowisku deweloperskim: `./gradlew runClient` (w `client-fabric`).

## Instalacja – krok po kroku

### Gracz (klient)
1. Zainstaluj Javę 21.
2. Pobierz **Fabric Installer** (fabricmc.net/use/installer), wybierz Minecraft **1.21.11** i zainstaluj loader (≥ 0.18.1).
3. Pobierz **Fabric API** `0.141.x+1.21.11` i wrzuć do folderu `mods`.
4. Skopiuj `doublejump-fabric-1.0.0.jar` do folderu `mods`
   (Windows: `%appdata%\.minecraft\mods`, Linux: `~/.minecraft/mods`, macOS: `~/Library/Application Support/minecraft/mods`).
5. Uruchom profil „fabric-loader-1.21.11”. Wejdź do świata i naciśnij SPACJĘ dwa razy w powietrzu.
6. Ustawienia: klawisz **J**; klawisze: *Opcje → Sterowanie → Klawisze*.

### Serwer (opcjonalnie, dla multiplayera)
1. Użyj Paper 1.21.11.
2. Wrzuć `DoubleJump-Paper-1.0.0.jar` do `plugins/` i zrestartuj serwer.
3. Edytuj `plugins/DoubleJump/config.yml` (siła maks., minimalny czas w powietrzu, cooldown, wyłączone światy,
   `resync-on-reject`), potem `/doublejump reload`.
4. Uprawnienia: `doublejump.use` (domyślnie dla wszystkich – odbierz je wybranym grupom w swoim pluginie
   uprawnień), `doublejump.admin` (domyślnie OP – komendy `/doublejump reload|status`).
5. Gracze muszą mieć mod kliencki – bez niego serwer po prostu nic nie robi.

## Co zostało sprawdzone, a co nie

**Sprawdzone (uruchomione):** logika podwójnego skoku klienta (18 sprawdzeń) i walidator serwera (17 sprawdzeń):

```bash
# klient
java -m jdk.compiler/com.sun.tools.javac.Main -d /tmp/t1 \
  client-fabric/src/main/java/pl/doublejump/client/JumpStateMachine.java \
  client-fabric/src/test/java/pl/doublejump/client/JumpStateMachineSelfTest.java
java -cp /tmp/t1 pl.doublejump.client.JumpStateMachineSelfTest

# serwer
java -m jdk.compiler/com.sun.tools.javac.Main -d /tmp/t2 \
  server-paper/src/main/java/pl/doublejump/paper/JumpValidator.java \
  server-paper/src/test/java/pl/doublejump/paper/JumpValidatorSelfTest.java
java -cp /tmp/t2 pl.doublejump.paper.JumpValidatorSelfTest
```

**Niesprawdzone:** pełna kompilacja Gradle i uruchomienie w grze. Środowisko, w którym powstał projekt,
nie miało dostępu do internetu, więc nie dało się pobrać Minecrafta, Fabric Loom ani Paper API.
Wersje zależności i nazwy klas/metod (mapowania Mojang, Fabric Networking/KeyBinding) sprawdziłem
w dokumentacji Fabric dla 1.21.11, ale **pierwszy `gradle build` u Ciebie jest właściwym testem**.
Jeśli kompilator zgłosi błąd:
* *nie znaleziono wersji* → sprawdź `gradle.properties` (loader/Fabric API) na fabricmc.net/develop;
* *nie znaleziono metody/klasy* → najbardziej prawdopodobne miejsca: `DoubleJumpHandler.spawnParticles`
  (`particleEngine.createParticle`), `DoubleJumpConfigScreen.render` (`drawCenteredString`)
  i `DoubleJumpClient` (`InputConstants.UNKNOWN`); wklej mi komunikat błędu, a poprawię.
