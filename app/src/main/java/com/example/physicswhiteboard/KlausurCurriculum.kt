package com.example.physicswhiteboard

data class Chapter(
    val number: Int,
    val title: String,
    val subtitle: String,
    val summary: List<String>,
    val formulas: List<Pair<String, String>>, // Formula to Explanation
    val derivations: List<String>,
    val example: String,
    val traps: List<String>,
    val tips15Points: String,
    val source: String
)

data class SelfTestItem(
    val id: String,
    val points: Int,
    val afb: String,
    val topic: String,
    val question: String,
    val solution: String
)

object KlausurCurriculum {

    val CHAPTERS: List<Chapter> = listOf(
        Chapter(
            number = 1,
            title = "Die gleichförmige Kreisbewegung",
            subtitle = "Größen, Geschwindigkeiten, Zentripetalkraft",
            summary = listOf(
                "Eine gleichförmige Kreisbewegung benötigt immer eine zum Drehzentrum gerichtete Kraft — die Zentripetalkraft FZP.",
                "Bahngeschwindigkeit: v = 2πr/T · Winkelgeschwindigkeit: ω = 2π/T · Zusammenhang: v = ω·r",
                "Zentripetalbeschleunigung: aZP = v²/r = ω²·r — zum Zentrum gerichtet, Betrag konstant.",
                "Zentripetalkraft: FZP = m·v²/r = m·ω²·r",
                "Die Zentripetalkraft ist keine eigene Kraftart — eine reale Kraft wirkt als Zentripetalkraft."
            ),
            formulas = listOf(
                "v = 2π·r / T = ω·r" to "Bahngeschwindigkeit (m/s); v ⊥ FZP",
                "ω = 2π / T = 2π·f" to "Winkelgeschwindigkeit (1/s, nicht Hz!)",
                "aZP = v² / r = ω²·r" to "Zentripetalbeschleunigung zum Drehzentrum (m/s²)",
                "FZP = m·v² / r = m·ω²·r" to "Zentripetalkraft (N)"
            ),
            derivations = listOf(
                "1. v ⊥ FZP zu jedem Zeitpunkt: Die Kraft ändert nur die Richtung von v, nie den Betrag.",
                "2. FZP zu klein → Radius r vergrößert sich (Bahn weitet sich).",
                "3. FZP zu groß → Radius r verkleinert sich.",
                "4. Kraft fällt weg → Körper fliegt tangential geradlinig weiter (1. Newtonsches Axiom)."
            ),
            example = "Ein Kind (m = 40 kg, r = 4,0 m) vollführt in t10 = 30 s 10 Umläufe. T = 3,0 s, v = 8,4 m/s, ω = 2,1 s⁻¹, aZP = 17,6 m/s², FZP = 700 N.",
            traps = listOf(
                "Zentripetalkraft als eigene Kraft einzeichnen (wird nie als zusätzlicher Pfeil gezeichnet!).",
                "ω in Hz angeben (nur Frequenz f hat Hz, ω hat 1/s).",
                "Behaupten, ohne Kraft fliege der Körper radial nach außen (fliegt tangential weiter!)."
            ),
            tips15Points = "Bei jeder Kreisbewegungsaufgabe zuerst T oder ω bestimmen ('n Umläufe in t Sekunden' → T = t/n). Ergebnisse immer mit Einheit und Deutungssatz versehen.",
            source = "LEIFIphysik: Größen zur Beschreibung einer Kreisbewegung, Zentripetalkraft"
        ),
        Chapter(
            number = 2,
            title = "Kraftansätze",
            subtitle = "Welche Kraft wirkt als Zentripetalkraft?",
            summary = listOf(
                "FZP ist keine spezielle Kraftart wie Gravitation oder Lorentzkraft — reale Kräfte übernehmen die Zentripetalrolle.",
                "Der Kraftansatz ist der zentrale Lösungsschritt: Situation entscheidet, welche Kraft FZP liefert.",
                "Standardansätze: FZP = FZug (Seil), FZP = FG (Satellit), FZP = FL (Teilchen im B-Feld), FZP = Fhaft (Kurve)."
            ),
            formulas = listOf(
                "FZP = FZug" to "Schnur, Karussell, Rotor",
                "FZP = FG" to "Satellitenbahn, Planeten (FG = G·M·m/r²)",
                "FZP = FL" to "Geladenes Teilchen im Magnetfeld (FL = q·v·B)",
                "FZP = Fhaft" to "Fahrzeug in Kurve (Fhaft = μ·m·g)",
                "FZP = Fres" to "Vektorielle Summe mehrerer Kräfte (z.B. Kegelpendel)"
            ),
            derivations = listOf(
                "Lösungsstrategie Kreisdynamik:",
                "① Reale Kräfte im Inertialsystem identifizieren.",
                "② Vektorielle Resultierende zum Drehzentrum bilden.",
                "③ Gleich m·v²/r setzen und auflösen."
            ),
            example = "Kegelpendel (m = 0,5 kg, l = 1,2 m, v = 3,8 m/s): FZug + FG = Fres = FZP ≈ 6,0 N.",
            traps = listOf(
                "Eigenen Pfeil FZP zusätzlich zu realen Kräften zeichnen (Punktabzug!).",
                "Fliehkraft im Inertialsystem ansetzen (nur im rotierenden Bezugssystem vorhanden).",
                "Kräftebeträge skalar addieren statt vektoriell."
            ),
            tips15Points = "Antwort immer mit Standardsatz beginnen: 'Für eine gleichförmige Kreisbewegung muss eine zum Zentrum gerichtete Kraft vom Betrag FZP = mv²/r wirken; hier wird sie von ... aufgebracht.'",
            source = "LEIFIphysik: Zentripetalkraft als resultierende Kraft"
        ),
        Chapter(
            number = 3,
            title = "Magnetische Felder",
            subtitle = "Feldlinien, Zylinderspule, homogen vs. radial",
            summary = listOf(
                "Magnetfelder werden durch Feldlinien veranschaulicht: außerhalb von N nach S; Tangente = Feldrichtung; Dichte = Flussdichte B.",
                "Im Inneren langer Zylinderspulen herrscht ein homogenes Feld: B = μ₀·n·I.",
                "Mit ferromagnetischem Kern: B = μ₀·μr·n·I (μr bis 140.000).",
                "Feldrichtung: 2. Rechte-Faust-Regel (Finger in Stromrichtung → Daumen zeigt B im Inneren)."
            ),
            formulas = listOf(
                "B = μ₀ · (N/l) · I = μ₀ · n · I" to "Lange Zylinderspule (T = Vs/m²)",
                "B = μ₀ · μr · n · I" to "Spule mit Eisenkern (μr = Verstärkungsfaktor)",
                "BHelmholtz = 8·μ₀·N·IS / (√5³·R)" to "Helmholtzspulenpaar Mittelebene (besonders homogen)"
            ),
            derivations = listOf(
                "1. Spulenende: Feld öffnet sich, Flussdichte sinkt auf ca. B/2 ab.",
                "2. Homogenes Feld: B überall nach Betrag und Richtung konstant — Voraussetzung für echte Kreisbahnen."
            ),
            example = "Spule mit N = 1200, l = 30 cm, B = 10 mT: n = 4000 m⁻¹ → I = B/(μ₀·n) ≈ 2,0 A.",
            traps = listOf(
                "Windungszahl N statt Windungsdichte n = N/l in B = μ₀·n·I einsetzen.",
                "Feldlinien außerhalb von S nach N zeichnen (außerhalb immer N → S).",
                "Einheit von B verwechseln (1 T = 1 Vs/m² = 1 N/(A·m))."
            ),
            tips15Points = "In Klausur stets erwähnen: 'Im homogenen Feld ist B ortsunabhängig konstant; deshalb bleibt auch der Betrag der Lorentzkraft konstant.'",
            source = "LEIFIphysik: Magnetfeld von langen Zylinderspulen"
        ),
        Chapter(
            number = 4,
            title = "Die LORENTZ-Kraft",
            subtitle = "Herleitung, Richtung, Eigenschaften",
            summary = listOf(
                "Auf bewegte Ladungsträger im Magnetfeld wirkt die Lorentzkraft: FL = q·v·B (für v ⊥ B); allgemein FL = q·v·B·sin α.",
                "Drei-Finger-Regel: Rechte Hand für positive Ladungsträger (Daumen = v, Zeigefinger = B, Mittelfinger = FL); linke Hand für Elektronen.",
                "Entscheidend: FL steht immer senkrecht zu v → leistet keine Arbeit (W = 0) → kinetische Energie bleibt konstant."
            ),
            formulas = listOf(
                "FL = q · v · B" to "Lorentzkraft bei senkrechtem Eintritt (v ⊥ B)",
                "FL = q · v · B · sin α" to "Allgemeiner Eintrittswinkel α (α = 0° → FL = 0)",
                "Fmag = B · IL · Δl" to "Kraft auf stromdurchflossenen Leiter"
            ),
            derivations = listOf(
                "Mikroskopische Herleitung:",
                "Leiterstrom IL = ΔQ/Δt = N·q/Δt. Leiterkraft Fmag = B·IL·Δl = B·N·q·(Δl/Δt) = N·(q·v·B).",
                "Kraft pro Teilchen: FL = Fmag / N = q·v·B."
            ),
            example = "Elektron mit v = 3·10⁷ m/s in B = 1,0 mT: FL = 1,602·10⁻¹⁹ C · 3·10⁷ m/s · 0,001 T ≈ 4,8·10⁻¹⁵ N.",
            traps = listOf(
                "Rechte Hand für Elektronen benutzen ohne das Ergebnis um 180° umzukehren.",
                "Behaupten, die Lorentzkraft mache das Teilchen schneller (sie ändert nur die Richtung!)."
            ),
            tips15Points = "Der 15-Punkte-Dreiklang: 'Da FL stets senkrecht auf v steht und im homogenen Feld betraglich konstant ist, ergibt sich eine Kreisbahn; FL wirkt als FZP.'",
            source = "LEIFIphysik: LORENTZ-Kraft"
        ),
        Chapter(
            number = 5,
            title = "Geladene Teilchen im elektrischen Feld",
            subtitle = "Längsfeld & Querfeld",
            summary = listOf(
                "Längsfeld (v ∥ E): Geradlinig gleichmäßig beschleunigt. Elektrische Energie wird zu kinetischer Energie: q·U = ½·m·v² → v = √(2qU/m).",
                "Querfeld (v ⊥ E): Parabelbahn im Kondensator (Superposition aus x = v₀·t und y = ½·a·t²).",
                "Nach Austritt aus dem Kondensator geradlinig entlang der Tangente am Austrittspunkt weiter."
            ),
            formulas = listOf(
                "Fel = q · E = q · (U / d)" to "Elektrische Kraft im Plattenkondensator",
                "v = √(2 · q · U / m)" to "Geschwindigkeit aus Beschleunigungsspannung U",
                "y(x) = (q · E) / (2 · m · v₀²) · x²" to "Parabelbahn im Querfeld (Wurfparabel-Analog)",
                "tan α = vy / v₀ = (q·E·L) / (m·v₀²)" to "Austrittswinkel aus Kondensator der Länge L"
            ),
            derivations = listOf(
                "Querfeld-Herleitung:",
                "x-Richtung: kräftefrei → x = v₀·t → t = x / v₀",
                "y-Richtung: konstante Kraft Fel = q·E → a = q·E/m → y = ½·a·t²",
                "Einsetzen von t: y(x) = [q·E / (2·m·v₀²)] · x² (Parabel)."
            ),
            example = "Elektronen (v₀ = 3·10⁷ m/s, L = 5 cm, d = 1 cm, UK = 100 V): E = 10⁴ V/m, a = 1,76·10¹⁵ m/s², t = 1,67 ns, y = 2,4 mm.",
            traps = listOf(
                "Ablenkrichtung: Elektronen fliegen zur positiven Platte (entgegen E-Feldlinien).",
                "Nach dem Kondensator die Parabel weiterzeichnen statt geradlinig weiter.",
                "v ∝ √U übersehen (doppelte Spannung ergibt nicht doppelte Geschwindigkeit!)."
            ),
            tips15Points = "Warum Parabel? Dreisatz: 1. x gleichförmig x = v₀·t. 2. y gleichmäßig beschleunigt y = ½·at². 3. t eliminieren liefert y ∝ x².",
            source = "LEIFIphysik: Geladene Teilchen im elektrischen Längs-/Querfeld"
        ),
        Chapter(
            number = 6,
            title = "Geladene Teilchen im magnetischen Feld",
            subtitle = "Kreisbahn & Schraubenlinie",
            summary = listOf(
                "Senkrechter Eintritt: Lorentzkraft als Zentripetalkraft FL = FZP → r = m·v / (q·B).",
                "Umlaufdauer T = 2π·m / (q·B) ist unabhängig von Geschwindigkeit v und Radius r!",
                "Schräger Eintritt (Winkel α): Schraubenlinie mit r = (m·v·sin α)/(q·B) und Ganghöhe h = (2π·m·v·cos α)/(q·B)."
            ),
            formulas = listOf(
                "r = (m · v) / (q · B)" to "Bahnradius im homogenen Querfeld",
                "T = (2π · m) / (q · B)" to "Umlaufdauer (v-unabhängig!)",
                "r = (m·v·sin α) / (q·B)" to "Schraubenlinie: Radius der Zylinderbahn",
                "h = v∥ · T = (2π·m·v·cos α) / (q·B)" to "Schraubenlinie: Ganghöhe pro Umlauf"
            ),
            derivations = listOf(
                "Kreisbahn-Herleitung:",
                "q·v·B = m·v² / r  | :v",
                "q·B = m·v / r  <=>  r = (m·v) / (q·B)",
                "Umlaufdauer: T = 2π·r / v = 2π·(m·v/(q·B)) / v = 2π·m / (q·B) (v kürzt sich heraus!)."
            ),
            example = "Elektron (v = 3·10⁷ m/s, B = 1,0 mT): r = 0,17 m, T = 3,6·10⁻⁸ s.",
            traps = listOf(
                "Behaupten, schnellere Teilchen hätten kürzere Umlaufzeit (T ist v-unabhängig!).",
                "Bei der Schraubenlinie sin α (Querkomponente) und cos α (Längskomponente) vertauschen."
            ),
            tips15Points = "Warum ist T unabhängig von v? Weil schnellere Teilchen einen proportional größeren Kreis durchlaufen: Wegstrecke 2πr und v wachsen im selben Verhältnis.",
            source = "LEIFIphysik: Geladene Teilchen im magnetischen Querfeld"
        ),
        Chapter(
            number = 7,
            title = "Der WIENsche Geschwindigkeitsfilter",
            subtitle = "Prinzip & e/m-Bestimmung",
            summary = listOf(
                "Gekreuzte Felder: homogenes E-Feld ⊥ homogenes B-Feld.",
                "Geradliniger Durchgang nur bei Kräftegleichgewicht Fel = FL <=> q·E = q·v·B <=> v = E / B.",
                "Filter selektiert ausschließlich nach Geschwindigkeit — unabhängig von Masse m und Ladung q.",
                "Teilchen mit v > E/B oder v < E/B werden abgelenkt und an den Platten abgefangen."
            ),
            formulas = listOf(
                "vDurchlass = E / B" to "Durchlassbedingung (unabhängig von m und q)",
                "Fel = q · E" to "Elektrische Kraft auf Teilchen",
                "FL = q · v · B" to "Lorentzkraft auf Teilchen",
                "e / me = (125·R²) / (128·μ₀²·d²·N²) · UK² / (IS²·UB)" to "e/m-Bestimmung mit Wien-Filter & Helmholtzspulen"
            ),
            derivations = listOf(
                "Kräftebilanz bei Geschwindigkeitsabweichung:",
                "Für v > E/B: FL > Fel → Ablenkung zur magnetischen Seite.",
                "Für v < E/B: Fel > FL → Ablenkung zur elektrischen Seite."
            ),
            example = "E = 8·10⁴ V/m, B = 0,25 T: vDurchlass = 3,2·10⁵ m/s.",
            traps = listOf(
                "Behaupten, der Wien-Filter trenne nach Masse (er trennt nur nach Geschwindigkeit!).",
                "Richtung der Ablenkung ohne Kräftevergleich Fel vs FL raten."
            ),
            tips15Points = "Frage: Warum wandert der Strahl bei größerem UK? Antwort: Fel = q·E wächst, FL bleibt gleich → Resultierende zeigt zur Platte, Strahl wird abgelenkt.",
            source = "LEIFIphysik: WIENscher Geschwindigkeitsfilter"
        ),
        Chapter(
            number = 8,
            title = "Das Fadenstrahlrohr",
            subtitle = "e/m-Bestimmung mit der Kreisbahn",
            summary = listOf(
                "Elektronen werden mit UB beschleunigt (v₀ = √(2eUB/me)) und treten senkrecht in Helmholtz-B-Feld ein.",
                "Sichtbare Kreisbahn im verdünnten Edelgas durch Stoßanregung (Leuchtfaden): r = me·v₀ / (e·B).",
                "Aus Messung von UB, Spulenstrom IS und Radius r wird die spezifische Ladung e/me bestimmt (Literaturwert 1,76·10¹¹ C/kg)."
            ),
            formulas = listOf(
                "r = (me · v₀) / (e · B)" to "Bahnradius im Fadenstrahlrohr",
                "v₀ = √(2 · e · UB / me)" to "Eintrittsgeschwindigkeit aus UB",
                "e / me = (125 · R²) / (32 · μ₀² · N²) · UB / (r² · IS²)" to "e/me-Messformel des Fadenstrahlrohrs"
            ),
            derivations = listOf(
                "e/m-Herleitung:",
                "Aus r = me·v₀/(e·B) folgt r² = me²·v₀² / (e²·B²).",
                "Einsetzen von v₀² = 2·e·UB/me: r² = 2·UB·me / (e·B²).",
                "Auflösen: e/me = 2·UB / (r²·B²). Mit B ∝ IS folgt e/me ∝ UB / (r²·IS²)."
            ),
            example = "UB = 300 V, IS = 0,60 A, R = 6,8 cm, N = 320: r ≈ 2,3 cm → e/me ≈ 1,76·10¹¹ C/kg.",
            traps = listOf(
                "r mit dem Spulendurchmesser verwechseln.",
                "IS geht quadratisch ein (IS²), weil B ∝ IS und B quadratisch im Nenner steht.",
                "Glauben, Elektronen würden im B-Feld schneller (FL leistet keine Arbeit!)."
            ),
            tips15Points = "Fehlerfortpflanzung: Radius r steht quadratisch im Nenner (e/m ∝ 1/r²). Ein 5% Ablesefehler bei r führt zu ca. 10% Fehler bei e/m.",
            source = "LEIFIphysik: Fadenstrahlrohr"
        ),
        Chapter(
            number = 9,
            title = "Massenspektrometer",
            subtitle = "BAINBRIDGE & ASTON, Fokussierung, Flugzeit TOF",
            summary = listOf(
                "Trennt geladene Teilchen nach ihrer spezifischen Ladung q/m (bzw. bei gleicher Ladung nach Masse → Isotopentrennung).",
                "BAINBRIDGE: Wien-Filter (v = EF/BF) + Analysatorfeld BA → Halbkreisbahnen r = m·EF / (q·BA·BF).",
                "Auftreffabstand auf Detektorplatte beträgt 2·r (Durchmesser des Halbkreises!).",
                "ASTON-Fokussierung: Kompensation kleiner Geschwindigkeits- und Richtungsstreuungen.",
                "Flugzeit-Spektrometer (TOF): t = L·√(m / (2qU)) ∝ √(m/q) → leichteste Ionen kommen zuerst an."
            ),
            formulas = listOf(
                "r = (m · EF) / (q · BA · BF)" to "Bahnradius im Bainbridge-Analysator",
                "m = (r · q · BA · BF) / EF" to "Massenbestimmung aus Radius r",
                "m = (r · q · B²) / EF" to "Spezialfall: identisches B im Filter und Analysator",
                "t = L · √(m / (2 · q · U))" to "Flugzeit-Massenspektrometer (TOF)"
            ),
            derivations = listOf(
                "Bainbridge-Herleitung:",
                "1. Filter: v = EF / BF.",
                "2. Analysator: q·v·BA = m·v² / r => r = m·v / (q·BA).",
                "3. v einsetzen: r = m·EF / (q·BA·BF).",
                "4. Auftreffabstand von Eintrittsblende: x = 2·r."
            ),
            example = "Neon-Isotope ²⁰Ne (m₁ = 3,32·10⁻²⁶ kg) und ²²Ne (m₂ = 3,65·10⁻²⁶ kg) in B = 0,5 T, EF = 5·10⁴ V/m: v = 10⁵ m/s, r₁ = 4,1 cm, r₂ = 4,6 cm, Linienabstand Δ(2r) = 1,0 cm.",
            traps = listOf(
                "Auftreffabstand als r statt 2·r angeben (Halbkreis!).",
                "Wien-Filter (wählt v) mit Analysator (trennt m/q) verwechseln.",
                "Vergessen, warum Blenden allein nicht reichen (ohne Fokussierung sinkt die Strahlintensität drastisch)."
            ),
            tips15Points = "Warum vor dem Analysator ein Wien-Filter? Weil r = mv/(qB) von m UND v abhängt! Ohne Filter würden schnelle leichte und langsame schwere Ionen am selben Ort landen (Verschmierung).",
            source = "LEIFIphysik: BAINBRIDGE- und ASTON-Massenspektrometer"
        ),
        Chapter(
            number = 10,
            title = "Der HALL-Effekt",
            subtitle = "Hallspannung, Herleitung, Anwendungen",
            summary = listOf(
                "Stromdurchflossener Leiter im Magnetfeld B erfährt Ladungsträgertrennung durch Lorentzkraft.",
                "Stationärer Zustand: Lorentzkraft FL kompensiert Quer-E-Feld-Kraft Fel → UH = b·v·B.",
                "Hallspannung UH = RH · (I·B) / d, unabhängig von Plattenbreite b; d = Dicke parallel zu B.",
                "Hall-Konstante RH = 1/(n·q): Vorzeichen zeigt Trägerart (Elektronen: RH < 0); Betrag liefert Trägerdichte n."
            ),
            formulas = listOf(
                "UH = RH · (I · B) / d" to "Hallspannung (V)",
                "RH = 1 / (n · q)" to "Hall-Konstante (m³/C)",
                "n = 1 / (|RH| · e)" to "Ladungsträgerdichte (1/m³)",
                "UH = b · v · B" to "Gleichgewichtszustand (b = Leiterbreite quer zu I)"
            ),
            derivations = listOf(
                "Vollständige Herleitung:",
                "1. Kräftegleichgewicht: q·v·B = q·EH = q·(UH / b) => UH = b·v·B.",
                "2. Stromstärke: I = n·q·A·v mit Querschnitt A = b·d => v = I / (n·q·b·d).",
                "3. Einsetzen: UH = b · [I / (n·q·b·d)] · B = [1/(n·q)] · (I·B / d) = RH · (I·B / d)."
            ),
            example = "Silberplättchen (RH = -8,9·10⁻¹¹ m³/C, d = 0,1 mm, I = 15 A, B = 0,46 T): UH ≈ 6,1 μV, n ≈ 7,0·10²⁸ m⁻³.",
            traps = listOf(
                "b (Breite) und d (Dicke parallel zu B) verwechseln (nur d steht im Nenner von UH).",
                "Glauben, UH hänge von der Breite b ab (b kürzt sich heraus!).",
                "Einheit von RH: 1 m³/C."
            ),
            tips15Points = "Anwendung immer nennen: Hall-Sonde als Magnetfeld-Sensor (da UH ∝ B bei konstantem I) oder kontaktloser Stromsensor (UH ∝ I bei konstantem B).",
            source = "LEIFIphysik: HALL-Effekt"
        )
    )

    val SELF_TEST: List<SelfTestItem> = listOf(
        SelfTestItem(
            id = "A1",
            points = 6,
            afb = "I-II",
            topic = "Kreisbewegung",
            question = "Ein Kind (m = 45 kg) sitzt auf einem Kettenkarussell im Abstand r = 5,0 m von der Drehachse und vollführt 12 Umläufe in 90 s. Berechne Umlaufdauer T, Bahngeschwindigkeit v und die zum Drehzentrum gerichtete Kraft FZP.",
            solution = "T = 90 s / 12 = 7,5 s.\nv = 2π·r / T = 2π·5,0 m / 7,5 s ≈ 4,2 m/s.\nFZP = m·v² / r = 45 kg · (4,2 m/s)² / 5,0 m ≈ 1,6·10² N.\nDie Haltekraft der Ketten wirkt waagerecht zum Drehzentrum als Zentripetalkraft."
        ),
        SelfTestItem(
            id = "A2",
            points = 4,
            afb = "II",
            topic = "Kraftansatz",
            question = "Ein Satellit umkreist die Erde auf einer Kreisbahn. Erkläre mit Kraftansatz, warum keine weitere Kraft neben der Gravitationskraft nötig ist, und warum ein 'Fliehkraftpfeil' in der Inertialskizze falsch wäre.",
            solution = "Für eine gleichförmige Kreisbahn ist eine zum Zentrum gerichtete Kraft vom Betrag FZP = mv²/r nötig. Die Gravitationskraft FG = G·M·m/r² ist stets zum Erdzentrum gerichtet und übernimmt diese Rolle vollständig: FG = FZP. Ein Fliehkraftpfeil ist falsch, da im Inertialsystem nur reale Wechselwirkungskräfte existieren; die Zentrifugalkraft ist eine Scheinkraft im rotierenden Bezugssystem."
        ),
        SelfTestItem(
            id = "A3",
            points = 5,
            afb = "I-II",
            topic = "E-Längsfeld",
            question = "Ein Proton (m = 1,673·10⁻²⁷ kg, q = e) ruht an der Anode und durchläuft die Spannung U = 2,0·10⁵ V. Begründe die Energieerhaltung und berechne die Endgeschwindigkeit v.",
            solution = "Im homogenen E-Feld leistet Fel = q·E Arbeit: q·U = ½·m·v².\nv = √(2·q·U / m) = √(2 · 1,602·10⁻¹⁹ C · 2·10⁵ V / 1,673·10⁻²⁷ kg) ≈ 6,2·10⁶ m/s (≈ 2 % c)."
        ),
        SelfTestItem(
            id = "A4",
            points = 6,
            afb = "II",
            topic = "E-Querfeld",
            question = "Elektronen mit v₀ = 3,0·10⁷ m/s treten mittig in einen Plattenkondensator (L = 6,0 cm, d = 2,0 cm, UK = 200 V) ein. Berechne Ablenkung y beim Austritt und den Austrittswinkel α.",
            solution = "E = UK/d = 200 V / 0,02 m = 10⁴ V/m.\na = e·E / me = 1,602·10⁻¹⁹ · 10⁴ / 9,11·10⁻³¹ ≈ 1,76·10¹⁵ m/s².\nFlugzeit t = L / v₀ = 0,06 m / 3·10⁷ m/s = 2,0·10⁻⁹ s.\ny = ½·a·t² = 0,5 · 1,76·10¹⁵ · (2·10⁻⁹)² ≈ 3,5 mm (zur positiven Platte hin).\nvy = a·t = 3,5·10⁶ m/s → tan α = vy / v₀ = 3,5·10⁶ / 3·10⁷ ≈ 0,117 → α ≈ 6,7°."
        ),
        SelfTestItem(
            id = "A5",
            points = 6,
            afb = "I-II",
            topic = "B-Querfeld",
            question = "Ein α-Teilchen (q = 2e, m = 6,64·10⁻²⁷ kg) fliegt mit v = 1,5·10⁷ m/s senkrecht in B = 0,80 T. Begründe die Kreisbahn und berechne r und T. Was passiert bei doppeltem B?",
            solution = "FL = q·v·B steht stets senkrecht auf v und ist betraglich konstant → Kreisbahn (FL = FZP).\nr = (m·v) / (q·B) = 6,64·10⁻²⁷ · 1,5·10⁷ / (3,204·10⁻¹⁹ · 0,80) ≈ 0,39 m.\nT = 2π·m / (q·B) = 2π · 6,64·10⁻²⁷ / 2,563·10⁻¹⁹ ≈ 1,6·10⁻⁷ s.\nBei doppeltem B halbiert sich r (r ∝ 1/B) und auch T halbiert sich (T ∝ 1/B)."
        ),
        SelfTestItem(
            id = "A6",
            points = 5,
            afb = "II",
            topic = "Schraubenlinie",
            question = "Ein Elektron tritt mit v = 2,0·10⁶ m/s unter dem Winkel α = 30° zu den Feldlinien in B = 1,0 mT ein. Berechne Radius r und Ganghöhe h.",
            solution = "Senkrechte Komponente: v⊥ = v·sin(30°) = 1,0·10⁶ m/s.\nParallele Komponente: v∥ = v·cos(30°) ≈ 1,73·10⁶ m/s.\nr = (me·v⊥) / (e·B) = 9,11·10⁻³¹ · 10⁶ / (1,602·10⁻¹⁹ · 10⁻³) ≈ 5,7 mm.\nT = 2π·me / (e·B) ≈ 3,57·10⁻⁸ s → h = v∥ · T ≈ 1,73·10⁶ · 3,57·10⁻⁸ ≈ 6,2 cm."
        ),
        SelfTestItem(
            id = "A7",
            points = 5,
            afb = "II",
            topic = "Wien-Filter",
            question = "Im Wien-Filter gilt E = 8,0·10⁴ V/m und B = 0,25 T. a) Berechne vDurchlass. b) Was erfährt ein langsameres Ion (v = 1,0·10⁵ m/s)? c) Warum trennt der Filter nicht nach Masse?",
            solution = "a) vDurchlass = E / B = 8·10⁴ / 0,25 = 3,2·10⁵ m/s.\nb) Für v < vDurchlass ist FL = q·v·B kleiner als Fel = q·E. Die elektrische Kraft überwiegt → Teilchen wird zur elektrischen Platte hin abgelenkt und abgefangen.\nc) In q·E = q·v·B kürzt sich q heraus und m kommt nicht vor; Durchlass hängt nur von v ab."
        ),
        SelfTestItem(
            id = "A8",
            points = 7,
            afb = "II",
            topic = "Massenspektrometer",
            question = "Bainbridge-Spektrometer mit B = 0,40 T und EF = 2,0·10⁵ V/m. Für ein Ion (q = e) wird r₁ = 15,0 cm gemessen. Berechne vDurchlass und m₁. Ein zweites Ion trifft bei r₂ = 17,5 cm auf; berechne m₂.",
            solution = "vDurchlass = EF / B = 2·10⁵ / 0,40 = 5,0·10⁵ m/s.\nm₁ = (r₁ · q · B²) / EF = 0,15 · 1,602·10⁻¹⁹ · 0,16 / 2·10⁵ ≈ 1,92·10⁻²⁶ kg (≈ 11,6 u ≈ ¹²C).\nm₂ / m₁ = r₂ / r₁ = 17,5 / 15,0 = 1,167 → m₂ ≈ 2,24·10⁻²⁶ kg (≈ 13,5 u ≈ ¹⁴C).\nAuftreffabstand vom Eintrittsspalt beträgt 2·r (Halbkreis!)."
        ),
        SelfTestItem(
            id = "A9",
            points = 5,
            afb = "II-III",
            topic = "Flugzeit-Spektrometer",
            question = "Einfach geladene ¹²C⁺ (m₁₂ = 1,99·10⁻²⁶ kg) und ¹⁴C⁺ (m₁₄ = 2,33·10⁻²⁶ kg) werden mit U = 500 V beschleunigt und fliegen L = 1,5 m. Berechne Flugzeiten t₁₂, t₁₄ und die Differenz Δt. Warum ist kein Wien-Filter nötig?",
            solution = "t = L · √(m / (2·q·U)).\nt₁₂ = 1,5 · √(1,99·10⁻²⁶ / (2 · 1,602·10⁻¹⁹ · 500)) ≈ 16,7 μs.\nt₁₄ = 1,5 · √(2,33·10⁻²⁶ / 1,602·10⁻¹⁶) ≈ 18,1 μs.\nΔt ≈ 1,4 μs.\nKein Filter nötig, weil beide Ionen dieselbe kinetische Energie q·U erhalten; wegen v = √(2qU/m) ist die Geschwindigkeitsdifferenz (v ∝ 1/√m) gerade der Träger der Masseninformation!"
        ),
        SelfTestItem(
            id = "A10",
            points = 6,
            afb = "I-II",
            topic = "Hall-Effekt",
            question = "Kupferplättchen (RH = -5,3·10⁻¹¹ m³/C, Dicke d = 0,20 mm parallel zu B) führt I = 10 A im Feld B = 0,80 T. Berechne UH und n. Was besagt das Vorzeichen?",
            solution = "|UH| = |RH| · (I · B) / d = 5,3·10⁻¹¹ · 10 · 0,80 / (2·10⁻⁴ m) ≈ 2,1·10⁻⁶ V = 2,1 μV.\nn = 1 / (|RH| · e) = 1 / (5,3·10⁻¹¹ · 1,602·10⁻¹⁹) ≈ 1,2·10²⁹ m⁻³.\nRH < 0 beweist, dass im Kupfer negative Ladungsträger (Elektronen) den Strom tragen."
        ),
        SelfTestItem(
            id = "A11",
            points = 6,
            afb = "II",
            topic = "Fadenstrahlrohr",
            question = "UB = 300 V, Helmholtzspulen N = 320, R = 6,80 cm, IS = 0,60 A. Berechne Eintrittsgeschwindigkeit v₀, B-Feld und Radius r.",
            solution = "v₀ = √(2 · (e/me) · UB) = √(2 · 1,7588·10¹¹ · 300) ≈ 1,03·10⁷ m/s.\nB = 8·μ₀·N·IS / (√5³·R) = 8 · 1,2566·10⁻⁶ · 320 · 0,60 / (11,18 · 0,068) ≈ 2,5·10⁻³ T = 2,5 mT.\nr = (me · v₀) / (e · B) = 9,11·10⁻³¹ · 1,03·10⁷ / (1,602·10⁻¹⁹ · 2,5·10⁻³) ≈ 2,3 cm."
        ),
        SelfTestItem(
            id = "A12",
            points = 4,
            afb = "III",
            topic = "Transfer Umlaufzeit",
            question = "Begründe quantitativ, warum T = 2πm/(qB) nicht von v abhängt, und nenne eine messtechnische Konsequenz.",
            solution = "T = 2πr/v. Mit r = mv/(qB) folgt T = 2π(mv/(qB))/v = 2πm/(qB) — v kürzt sich heraus. Größere Geschwindigkeit verlangt zwar quadratisch größere Zentripetalkraft (v²), der Bahnradius wächst aber proportional zu v: Wegstrecke und Tempo steigen im selben Verhältnis. Konsequenz: Ionen gleicher q/m landen stets zeitgleich (Fokussierung im Massenspektrometer; Zyklotron-Beschleunigung)."
        ),
        SelfTestItem(
            id = "A13",
            points = 4,
            afb = "III",
            topic = "Transfer Arbeit in Feldern",
            question = "'Die Lorentzkraft kann die kinetische Energie eines Teilchens nie verändern, das E-Feld sehr wohl.' Nimm Stellung.",
            solution = "Aussage ist physikalisch exakt. FL steht zu jedem Zeitpunkt senkrecht auf v und somit senkrecht auf dem Wegstück ds: W = ∫ FL · ds = 0 (keine Arbeit, |v| konstant). Das E-Feld dagegen wirkt parallel/antiparallel (Längsfeld) bzw. mit Wegkomponente in Kraftrichtung: W = q·U = ΔEkin (Teilchen wird beschleunigt oder abgebremst)."
        ),
        SelfTestItem(
            id = "A14",
            points = 5,
            afb = "III",
            topic = "Transfer Filter-Notwendigkeit",
            question = "Erkläre, warum im Bainbridge-Spektrometer ein Wien-Filter vor dem Analysatorfeld steht, obwohl r ohnehin von m/q abhängt.",
            solution = "Im Analysator gilt r = m·v / (q·BA). Der Radius hängt von m UND v ab. Ohne Filter kämen Ionen aus dem Ofen mit breiter Geschwindigkeitsverteilung an: Ein schnelles leichtes Ion könnte denselben Radius beschreiben wie ein langsames schweres Ion. Das Massenspektrum würde verschmieren. Der Wien-Filter stellt exakt v = EF/BF sicher; damit wird der Radius eine eindeutige Funktion von m/q."
        )
    )

    val STRATEGY_15_POINTS: List<String> = listOf(
        "1. AFB-Verteilung beachten: AFB I (Wiedergabe) ca. 40%, AFB II (Anwendung) ca. 40%, AFB III (Transfer) ca. 20%.",
        "2. Bei Herleitungen immer mit physikalischem Ansatz beginnen (z.B. 'Kräftegleichgewicht Fel = FL' oder 'FL wirkt als FZP').",
        "3. Einheitenprobe ist Pflicht: 1 T = 1 Vs/m² = 1 N/(A·m); ω in 1/s (nie in Hz!); Radius r immer in Meter umrechnen.",
        "4. Immer einen Deutungssatz nach einer Rechnung formulieren ('Dies entspricht ca. 2% der Lichtgeschwindigkeit').",
        "5. Skizzen vollständig beschriften: Feldrichtung (Symbole ⊙ für heraus, ⊗ für hinein), Geschwindigkeitsvektoren, Radien, Abstände.",
        "6. Bei Massenspektrometern immer beachten: Der Detektorabstand nach dem Halbkreis beträgt 2·r!",
        "7. Rechte-Hand vs. Linke-Hand-Regel: Für Elektronen (negative Ladung) gilt die linke Hand!"
    )
}
