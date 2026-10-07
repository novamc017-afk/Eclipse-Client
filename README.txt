BUILD LOCALLY (Java 21 + Gradle 9.x installed):
  gradle build
  -> jar appears in build/libs/eclipse-client-1.0.0.jar (not the -sources one)

BUILD WITHOUT INSTALLING ANYTHING:
  1. Create a GitHub repo and upload everything in this folder (keep the .github folder).
  2. Open the Actions tab, wait for "Build jar" to finish.
  3. Download the "eclipse-client-jar" artifact.

INSTALL: put the jar in .minecraft/mods with Fabric Loader 0.18+ and Fabric API for 1.21.11.
