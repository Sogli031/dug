# Project Map
_Generated: 2026-05-12 22:44 | Staleness: timestamps_

## Directory Structure
app/ — Android Compose aplikacija za praćenje dugovanja, uplata, valuta, analiza i cloud backup-a.
app/src/main/java/com/example/debttracker/data/ — modeli, `DebtManager` i Google Drive backup sloj.
app/src/main/java/com/example/debttracker/ui/components/ — zajedničke Compose kartice, dugmad, indikatori i picker-i.
app/src/main/java/com/example/debttracker/ui/dialogs/ — dijalozi za dužnike, uplate, potvrde, info i backup.
app/src/main/java/com/example/debttracker/ui/screens/ — glavni ekran i crash log ekran.
app/src/main/java/com/example/debttracker/ui/sheets/ — bottom sheet prikazi za mesečnu analizu i istoriju uplata.

## Key Files
settings.gradle.kts — definiše Gradle root `DebtTracker` i modul `:app`.
build.gradle.kts — koristi AGP 8.9.0 i Kotlin 2.2.20, drugačije od većine ostalih projekata.
app/build.gradle.kts — Compose Android konfiguracija, namespace `com.example.debttracker`.
app/src/main/AndroidManifest.xml — aplikacija, launcher aktivnost i storage/internet permisije.
app/src/main/java/com/example/debttracker/MainActivity.kt — ulaz u Compose UI.
app/src/main/java/com/example/debttracker/DebtTrackerApp.kt — Application klasa.
app/src/main/java/com/example/debttracker/data/DebtManager.kt — centralna logika za dužnike, uplate, backup i promene stanja.
app/src/main/java/com/example/debttracker/data/Models.kt — modeli uplata, dužnika i valuta.
app/src/main/java/com/example/debttracker/ui/screens/DebtTrackerScreen.kt — glavni ekran i UI state.
app/src/main/java/com/example/debttracker/data/backup/GoogleDriveBackup.kt — Google Drive sign-in i sync tok.

## Critical Constraints
- Poštovati parent `AGENTS.md`: raditi samo lokalno i ne pokretati Git komande.
- Projekat trenutno odstupa od workspace standarda verzija Gradle/Kotlin; ne usklađivati usput bez eksplicitnog zahteva.
- `DebtManager` je centralni persistence i poslovni sloj; UI izmene ne treba da dupliraju pravila duga ili uplata.
- Backup/import treba proveravati kroz JSON round-trip i postojeći Google Drive tok.

## Hot Files
app/src/main/java/com/example/debttracker/data/DebtManager.kt, app/src/main/java/com/example/debttracker/data/Models.kt, app/src/main/java/com/example/debttracker/ui/screens/DebtTrackerScreen.kt, app/src/main/java/com/example/debttracker/data/backup/GoogleDriveBackup.kt
