pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "ZIG"

include(":app")
include(":core:model")
include(":core:engine")
include(":core:data")
include(":core:credits")
include(":feature:museum")
include(":feature:viewer")
include(":tools:assetkit")
include(":tools:blackhole-lut")
include(":tools:ci")
